package fit.spotted.api.db.dao

import fit.spotted.api.db.DatabaseFactory.dbQuery
import fit.spotted.api.db.tables.*
import fit.spotted.api.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object AchievementDao {
    suspend fun create(
        userId: Int,
        name: String,
        description: String,
        iconUrl: String,
        challengeId: Int? = null
    ): Achievement? = dbQuery {
        val id = AchievementTable.insert {
            it[AchievementTable.userId] = userId
            it[AchievementTable.name] = name
            it[AchievementTable.description] = description
            it[AchievementTable.iconUrl] = iconUrl
            it[AchievementTable.earnedAt] = Instant.now().toEpochMilli()
            if (challengeId != null) {
                it[AchievementTable.challengeId] = challengeId
            }
        } get AchievementTable.id

        // Need to use a direct query to avoid suspend function call
        AchievementTable.select { AchievementTable.id eq id }
            .mapNotNull { 
                Achievement(
                    id = it[AchievementTable.id],
                    userId = it[AchievementTable.userId],
                    name = it[AchievementTable.name],
                    description = it[AchievementTable.description],
                    iconUrl = it[AchievementTable.iconUrl],
                    earnedAt = it[AchievementTable.earnedAt],
                    challengeId = it[AchievementTable.challengeId]
                )
            }
            .singleOrNull()
    }

    suspend fun findById(id: Int): Achievement? = dbQuery {
        AchievementTable.select { AchievementTable.id eq id }
            .mapNotNull { rowToAchievement(it) }
            .singleOrNull()
    }

    suspend fun findAllByUser(userId: Int): List<Achievement> = dbQuery {
        AchievementTable.select { AchievementTable.userId eq userId }
            .mapNotNull { rowToAchievement(it) }
    }

    suspend fun findAllByChallenge(challengeId: Int): List<Achievement> = dbQuery {
        AchievementTable.select { AchievementTable.challengeId eq challengeId }
            .mapNotNull { rowToAchievement(it) }
    }

    private fun rowToAchievement(row: ResultRow): Achievement {
        return Achievement(
            id = row[AchievementTable.id],
            userId = row[AchievementTable.userId],
            name = row[AchievementTable.name],
            description = row[AchievementTable.description],
            iconUrl = row[AchievementTable.iconUrl],
            earnedAt = row[AchievementTable.earnedAt],
            challengeId = row[AchievementTable.challengeId]
        )
    }

    // Create achievements for challenge completion
    suspend fun createChallengeCompletionAchievements(challengeId: Int): Boolean = coroutineScope {
        val challengeDeferred = async { ChallengeDao.findById(challengeId) }
        val challenge = challengeDeferred.await() ?: return@coroutineScope false
        
        // Award achievements to all participants based on their contribution
        val participants = challenge.participants
        
        // Sort participants by contribution
        val sortedParticipants = participants.sortedByDescending { it.contributedMinutes }
        
        // Create different achievement levels
        when {
            sortedParticipants.isNotEmpty() -> {
                // Top contributor achievement
                val topContributor = sortedParticipants.first()
                
                val topAchievementDeferred = async {
                    create(
                        userId = topContributor.user.id,
                        name = "Top Contributor",
                        description = "Contributed the most minutes to challenge: ${challenge.name}",
                        iconUrl = "/achievements/top_contributor.png",
                        challengeId = challengeId
                    )
                }
                
                // Wait for top achievement to be created
                topAchievementDeferred.await()
                
                // All participants get a completion achievement
                sortedParticipants.forEach { participant ->
                    val contributionPercent = (participant.contributedMinutes.toFloat() / challenge.targetDuration.toFloat()) * 100
                    
                    val achievementName = when {
                        contributionPercent >= 50 -> "Major Contributor"
                        contributionPercent >= 25 -> "Valuable Contributor"
                        else -> "Challenge Participant"
                    }
                    
                    async {
                        create(
                            userId = participant.user.id,
                            name = achievementName,
                            description = "Completed challenge: ${challenge.name}",
                            iconUrl = "/achievements/${achievementName.lowercase().replace(" ", "_")}.png",
                            challengeId = challengeId
                        )
                    }
                }
                
                true
            }
            else -> false
        }
    }
} 