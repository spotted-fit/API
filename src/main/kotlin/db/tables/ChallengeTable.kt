package fit.spotted.api.db.tables

import org.jetbrains.exposed.sql.Table

object ChallengeTable : Table("challenges") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val description = text("description")
    val startDate = long("start_date")
    val endDate = long("end_date")
    val targetDuration = integer("target_duration")
    val currentProgress = integer("current_progress").default(0)
    val createdById = integer("created_by_id").references(UserTable.id)
    val isCompleted = bool("is_completed").default(false)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
} 