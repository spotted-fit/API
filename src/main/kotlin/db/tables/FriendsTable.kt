package db.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object FriendRequestTable : IntIdTable("friend_requests") {
    val fromUser = reference("from_user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val toUser = reference("to_user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val status = varchar("status", 20) // "pending", "accepted", "declined"
    val createdAt = long("created_at")
}
