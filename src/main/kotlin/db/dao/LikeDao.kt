package db.dao

import db.DatabaseFactory.dbQuery
import db.tables.LikeTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select

object LikeDao {
    suspend fun likePost(userId: Int, postId: Int) = dbQuery {
        LikeTable.insertIgnore {
            it[LikeTable.user] = userId
            it[LikeTable.post] = postId
        }
    }

    suspend fun unlikePost(userId: Int, postId: Int) = dbQuery {
        LikeTable.deleteWhere {
            (LikeTable.user eq userId) and (LikeTable.post eq postId)
        }
    }

    suspend fun countForPost(postId: Int): Int = dbQuery {
        LikeTable.select { LikeTable.post eq postId }.count().toInt()
    }

    suspend fun isLikedByUser(userId: Int, postId: Int): Boolean = dbQuery {
        LikeTable.select {
            (LikeTable.post eq postId) and (LikeTable.user eq userId)
        }.count() > 0
    }
}
