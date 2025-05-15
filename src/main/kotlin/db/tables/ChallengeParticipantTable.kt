package fit.spotted.api.db.tables

import org.jetbrains.exposed.sql.Table

object ChallengeParticipantTable : Table("challenge_participants") {
    val id = integer("id").autoIncrement()
    val challengeId = integer("challenge_id").references(ChallengeTable.id)
    val userId = integer("user_id").references(UserTable.id)
    val contributedMinutes = integer("contributed_minutes").default(0)
    val joinedAt = long("joined_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        uniqueIndex(challengeId, userId)
    }
} 