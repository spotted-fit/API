package fit.spotted.api.db.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object PhotoTable : IntIdTable("photos") {
    val user = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val path = varchar("path", 512)
    val uploadedAt = long("uploaded_at")
}
