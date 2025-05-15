package fit.spotted.api.db.tables

import org.jetbrains.exposed.sql.Table

object ChallengeInviteTable : Table("challenge_invites") {
    val id = integer("id").autoIncrement()
    val challengeId = integer("challenge_id").references(ChallengeTable.id)
    val invitedUserId = integer("invited_user_id").references(UserTable.id)
    val invitedById = integer("invited_by_id").references(UserTable.id)
    val invitedAt = long("invited_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        uniqueIndex(challengeId, invitedUserId)
    }
} 