package fit.spotted.api.db.dao

import fit.spotted.api.db.DatabaseFactory.dbQuery
import fit.spotted.api.db.tables.CommentTable
import fit.spotted.api.db.tables.UserTable
import fit.spotted.api.models.Comment
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

object CommentDao {
    suspend fun addComment(postId: Int, userId: Int, text: String): Unit = dbQuery {
        CommentTable.insert {
            it[CommentTable.post] = postId
            it[CommentTable.user] = userId
            it[CommentTable.text] = text
            it[CommentTable.createdAt] = System.currentTimeMillis()
        }
    }

    suspend fun getComments(postId: Int): List<Comment> = dbQuery {
        (CommentTable innerJoin UserTable)
            .select { CommentTable.post eq postId }
            .orderBy(CommentTable.createdAt to SortOrder.ASC)
            .map {
                Comment(
                    id = it[CommentTable.id].value,
                    userId = it[CommentTable.user].value,
                    username = it[UserTable.username],
                    text = it[CommentTable.text],
                    createdAt = it[CommentTable.createdAt]
                )
            }
    }
}