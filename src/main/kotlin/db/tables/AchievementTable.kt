package fit.spotted.api.db.tables

import org.jetbrains.exposed.sql.Table

object AchievementTable : Table("achievements") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(UserTable.id)
    val name = varchar("name", 255)
    val description = text("description")
    val iconUrl = varchar("icon_url", 255)
    val earnedAt = long("earned_at")
    val challengeId = integer("challenge_id").references(ChallengeTable.id).nullable()
    
    override val primaryKey = PrimaryKey(id)
} 