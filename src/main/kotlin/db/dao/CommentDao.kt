package db.dao

import db.DatabaseFactory.dbQuery
import db.tables.CommentTable
import models.Comment
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
        CommentTable.select { CommentTable.post eq postId }
            .orderBy(CommentTable.createdAt to SortOrder.ASC)
            .map {
                Comment(
                    id = it[CommentTable.id].value,
                    userId = it[CommentTable.user].value,
                    text = it[CommentTable.text],
                    createdAt = it[CommentTable.createdAt]
                )
            }
    }
}