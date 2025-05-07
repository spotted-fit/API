package db.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object PostTable : IntIdTable("posts") {
    val user = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val photo1 = reference("photo1_id", PhotoTable, onDelete = ReferenceOption.CASCADE)
    val photo2 = reference("photo2_id", PhotoTable, onDelete = ReferenceOption.CASCADE)
    val text = text("text").nullable()
    val timer = integer("timer")
    val emoji = varchar("emoji", 10).nullable()
    val createdAt = long("created_at")
}
