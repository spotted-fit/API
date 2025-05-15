package fit.spotted.api.db.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object UserTable : IntIdTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val username = varchar("username", 30).uniqueIndex()
    val avatarPath = varchar("avatar_path", 512).nullable()
    val firebaseToken = varchar("firebase_token", 512).nullable()
}
