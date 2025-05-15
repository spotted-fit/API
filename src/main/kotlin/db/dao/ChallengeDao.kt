package fit.spotted.api.db.dao

import fit.spotted.api.db.DatabaseFactory.dbQuery
import fit.spotted.api.db.tables.*
import fit.spotted.api.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object ChallengeDao {
    suspend fun findById(id: Int): Challenge? = coroutineScope {
        val result = dbQuery {
            ChallengeTable.select { ChallengeTable.id eq id }
                .map { row -> 
                    Pair(
                        row,
                        row[ChallengeTable.createdById]
                    )
                }
                .singleOrNull()
        }
        
        if (result == null) return@coroutineScope null
        
        val (row, createdById) = result
        
        // Get creator separately using coroutines
        val creatorDeferred = async { UserDao.findById(createdById) }
        val creator = creatorDeferred.await() ?: throw IllegalStateException("Creator not found")
        
        val creatorPreview = UserPreview(
            id = creator.id,
            username = creator.username,
            avatar = creator.avatarPath
        )
        
        // Get participants separately
        val participantsDeferred = async { getParticipants(id) }
        val participants = participantsDeferred.await()
        
        Challenge(
            id = row[ChallengeTable.id],
            name = row[ChallengeTable.name],
            description = row[ChallengeTable.description],
            startDate = row[ChallengeTable.startDate],
            endDate = row[ChallengeTable.endDate],
            targetDuration = row[ChallengeTable.targetDuration],
            currentProgress = row[ChallengeTable.currentProgress],
            participants = participants,
            createdBy = creatorPreview,
            isCompleted = row[ChallengeTable.isCompleted]
        )
    }

    suspend fun findAll(userId: Int): List<Challenge> = coroutineScope {
        val challengeIds = dbQuery {
            (ChallengeTable innerJoin ChallengeParticipantTable)
                .slice(ChallengeTable.id)
                .select { ChallengeParticipantTable.userId eq userId }
                .map { it[ChallengeTable.id] }
        }
        
        // Fetch each challenge individually
        val challenges = challengeIds.mapNotNull { id ->
            async { findById(id) }
        }.map { it.await() }
        
        // Filter out nulls to satisfy type system
        challenges.filterNotNull()
    }

    suspend fun create(
        name: String, 
        description: String, 
        startDate: Long, 
        endDate: Long, 
        targetDuration: Int, 
        createdById: Int
    ): Challenge = coroutineScope {
        val challengeId = dbQuery {
            val id = ChallengeTable.insert {
                it[ChallengeTable.name] = name
                it[ChallengeTable.description] = description
                it[ChallengeTable.startDate] = startDate
                it[ChallengeTable.endDate] = endDate
                it[ChallengeTable.targetDuration] = targetDuration
                it[ChallengeTable.createdById] = createdById
                it[ChallengeTable.createdAt] = Instant.now().toEpochMilli()
            } get ChallengeTable.id

            // Add creator as a participant
            val now = Instant.now().toEpochMilli()
            ChallengeParticipantTable.insert {
                it[ChallengeParticipantTable.challengeId] = id
                it[ChallengeParticipantTable.userId] = createdById
                it[ChallengeParticipantTable.joinedAt] = now
            }
            
            id
        }

        val challengeDeferred = async { findById(challengeId) }
        val challenge = challengeDeferred.await()
        // Throw error if challenge is null
        challenge ?: throw IllegalStateException("Failed to retrieve newly created challenge")
    }

    suspend fun updateProgress(challengeId: Int, additionalMinutes: Int): Boolean = dbQuery {
        val challenge = ChallengeTable.select { ChallengeTable.id eq challengeId }.singleOrNull() ?: return@dbQuery false
        
        val currentProgress = challenge[ChallengeTable.currentProgress]
        val newProgress = currentProgress + additionalMinutes
        val targetDuration = challenge[ChallengeTable.targetDuration]
        
        ChallengeTable.update({ ChallengeTable.id eq challengeId }) {
            it[ChallengeTable.currentProgress] = newProgress
            if (newProgress >= targetDuration && !challenge[ChallengeTable.isCompleted]) {
                it[ChallengeTable.isCompleted] = true
            }
        } > 0
    }

    suspend fun markAsCompleted(challengeId: Int): Boolean = dbQuery {
        ChallengeTable.update({ ChallengeTable.id eq challengeId }) {
            it[isCompleted] = true
        } > 0
    }

    suspend fun updateParticipantProgress(userId: Int, challengeId: Int, additionalMinutes: Int): Boolean = dbQuery {
        val participant = ChallengeParticipantTable.select { 
            (ChallengeParticipantTable.userId eq userId) and (ChallengeParticipantTable.challengeId eq challengeId) 
        }.singleOrNull() ?: return@dbQuery false
        
        val currentContribution = participant[ChallengeParticipantTable.contributedMinutes]
        
        ChallengeParticipantTable.update({ 
            (ChallengeParticipantTable.userId eq userId) and (ChallengeParticipantTable.challengeId eq challengeId) 
        }) {
            it[contributedMinutes] = currentContribution + additionalMinutes
        } > 0
    }
    
    private suspend fun getParticipants(challengeId: Int): List<ChallengeParticipant> = coroutineScope {
        val participantRows = dbQuery {
            ChallengeParticipantTable.select { ChallengeParticipantTable.challengeId eq challengeId }
                .map { row ->
                    Triple(
                        row[ChallengeParticipantTable.userId],
                        row[ChallengeParticipantTable.contributedMinutes],
                        row[ChallengeParticipantTable.joinedAt]
                    )
                }
        }
        
        // Fetch users in parallel
        participantRows.map { (userId, contributedMinutes, joinedAt) ->
            async {
                val user = UserDao.findById(userId) ?: return@async null
                
                ChallengeParticipant(
                    user = UserPreview(
                        id = user.id,
                        username = user.username,
                        avatar = user.avatarPath
                    ),
                    contributedMinutes = contributedMinutes,
                    joinedAt = joinedAt
                )
            }
        }.mapNotNull { it.await() }
    }
} 