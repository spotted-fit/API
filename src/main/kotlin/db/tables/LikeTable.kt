package fit.spotted.api.db.tables

import org.jetbrains.exposed.sql.Table

object LikeTable : Table("likes") {
    val user = reference("user_id", UserTable)
    val post = reference("post_id", PostTable)
    override val primaryKey = PrimaryKey(user, post)
}

