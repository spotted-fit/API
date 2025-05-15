package fit.spotted.api.db.dao

import fit.spotted.api.db.DatabaseFactory.dbQuery
import fit.spotted.api.db.tables.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object ChallengeParticipantDao {
    suspend fun add(challengeId: Int, userId: Int): Boolean = dbQuery {
        // Check if already a participant
        val existingParticipant = ChallengeParticipantTable.select {
            (ChallengeParticipantTable.challengeId eq challengeId) and
                    (ChallengeParticipantTable.userId eq userId)
        }.singleOrNull()

        if (existingParticipant != null) {
            return@dbQuery false
        }

        ChallengeParticipantTable.insert {
            it[ChallengeParticipantTable.challengeId] = challengeId
            it[ChallengeParticipantTable.userId] = userId
            it[ChallengeParticipantTable.joinedAt] = Instant.now().toEpochMilli()
        }
        
        true
    }

    suspend fun remove(challengeId: Int, userId: Int): Boolean = dbQuery {
        // Don't remove contribution data, just remove the user from the challenge
        ChallengeParticipantTable.deleteWhere {
            (ChallengeParticipantTable.challengeId eq challengeId) and
                    (ChallengeParticipantTable.userId eq userId)
        } > 0
    }

    suspend fun isParticipant(challengeId: Int, userId: Int): Boolean = dbQuery {
        ChallengeParticipantTable.select {
            (ChallengeParticipantTable.challengeId eq challengeId) and
                    (ChallengeParticipantTable.userId eq userId)
        }.singleOrNull() != null
    }

    suspend fun getContributedMinutes(challengeId: Int, userId: Int): Int = dbQuery {
        val participant = ChallengeParticipantTable.select {
            (ChallengeParticipantTable.challengeId eq challengeId) and
                    (ChallengeParticipantTable.userId eq userId)
        }.singleOrNull()

        participant?.get(ChallengeParticipantTable.contributedMinutes) ?: 0
    }

    /**
     * Updates progress for all active challenges a user is participating in.
     * This method is called when a user creates a post, as each post represents a workout.
     * The post's timer value is used as the workout duration in minutes.
     *
     * @param userId The ID of the user who completed the workout
     * @param workoutDurationMinutes Duration of the workout in minutes
     * @return true if any challenges were updated, false otherwise
     */
    suspend fun updateActiveChallengesProgress(userId: Int, workoutDurationMinutes: Int): Boolean = coroutineScope {
        val activeChallengePairs = dbQuery {
            val currentTime = Instant.now().toEpochMilli()
            
            // Find all active challenges where the user is a participant
            (ChallengeParticipantTable innerJoin ChallengeTable)
                .slice(
                    ChallengeParticipantTable.challengeId,
                    ChallengeParticipantTable.contributedMinutes
                )
                .select {
                    (ChallengeParticipantTable.userId eq userId) and
                    (ChallengeTable.startDate lessEq currentTime) and
                    (ChallengeTable.endDate greaterEq currentTime) and
                    (ChallengeTable.isCompleted eq false)
                }.map {
                    Pair(
                        it[ChallengeParticipantTable.challengeId],
                        it[ChallengeParticipantTable.contributedMinutes]
                    )
                }
        }
        
        if (activeChallengePairs.isEmpty()) {
            return@coroutineScope false
        }
        
        // Update each active challenge with progress
        var anyUpdated = false
        
        // Process challenges in parallel
        val updateResults = activeChallengePairs.map { (challengeId, currentContribution) ->
            async {
                // Update participant contribution in database
                val participantUpdated = dbQuery {
                    ChallengeParticipantTable.update({
                        (ChallengeParticipantTable.challengeId eq challengeId) and
                                (ChallengeParticipantTable.userId eq userId)
                    }) {
                        it[contributedMinutes] = currentContribution + workoutDurationMinutes
                    } > 0
                }
                
                if (participantUpdated) {
                    // Update challenge total progress
                    val progressUpdated = ChallengeDao.updateProgress(challengeId, workoutDurationMinutes)
                    
                    // Check if challenge is now completed and generate achievements if needed
                    val challengeDeferred = async { ChallengeDao.findById(challengeId) }
                    val challenge = challengeDeferred.await()
                    
                    if (challenge != null && challenge.isCompleted) {
                        AchievementDao.createChallengeCompletionAchievements(challengeId)
                    }
                    
                    progressUpdated
                } else {
                    false
                }
            }
        }.map { it.await() }
        
        updateResults.any { it }
    }
} 