package db.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object CommentTable : IntIdTable("comments") {
    val user = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val post = reference("post_id", PostTable, onDelete = ReferenceOption.CASCADE)
    val text = text("text")
    val createdAt = long("created_at")
}
