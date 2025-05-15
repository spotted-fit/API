package fit.spotted.api.db.dao

import fit.spotted.api.db.DatabaseFactory.dbQuery
import fit.spotted.api.db.tables.PhotoTable
import fit.spotted.api.db.tables.PostTable
import fit.spotted.api.db.tables.UserTable
import fit.spotted.api.models.Post
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId


object PostDao {
    suspend fun findById(postId: Int): Post? = dbQuery {
        val p1 = PhotoTable.alias("p1")
        val p2 = PhotoTable.alias("p2")

        PostTable
            .innerJoin(UserTable, { PostTable.user }, { UserTable.id })
            .leftJoin(p1, { PostTable.photo1 }, { p1[PhotoTable.id] })
            .leftJoin(p2, { PostTable.photo2 }, { p2[PhotoTable.id] })
            .select { PostTable.id eq postId }
            .mapNotNull { row ->
                Post(
                    id = row[PostTable.id].value,
                    userId = row[PostTable.user].value,
                    username = row[UserTable.username],
                    photo1 = row[p1[PhotoTable.path]],
                    photo2 = row[p2[PhotoTable.path]],
                    timer = row[PostTable.timer],
                    text = row[PostTable.text],
                    emoji = row[PostTable.emoji],
                    createdAt = row[PostTable.createdAt]
                )
            }
            .singleOrNull()
    }

    suspend fun findByUserId(userId: Int): List<Post> = dbQuery {
        val p1 = PhotoTable.alias("p1")
        val p2 = PhotoTable.alias("p2")

        PostTable
            .innerJoin(UserTable, { PostTable.user }, { UserTable.id })
            .join(p1, JoinType.INNER, PostTable.photo1, p1[PhotoTable.id])
            .join(p2, JoinType.LEFT, PostTable.photo2, p2[PhotoTable.id])
            .select { PostTable.user eq userId }
            .map { row ->
                Post(
                    id = row[PostTable.id].value,
                    userId = row[PostTable.user].value,
                    username = row[UserTable.username],
                    photo1 = row[p1[PhotoTable.path]],
                    photo2 = row[p2[PhotoTable.path]],
                    text = row[PostTable.text],
                    emoji = row[PostTable.emoji],
                    timer = row[PostTable.timer],
                    createdAt = row[PostTable.createdAt]
                )
            }
    }

    suspend fun getRecentPosts(userIds: List<Int>): List<Post> = dbQuery {
        val sevenDays = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L

        val p1 = PhotoTable.alias("p1")
        val p2 = PhotoTable.alias("p2")

        PostTable
            .innerJoin(UserTable, { PostTable.user }, { UserTable.id })
            .join(p1, JoinType.INNER, PostTable.photo1, p1[PhotoTable.id])
            .join(p2, JoinType.LEFT, PostTable.photo2, p2[PhotoTable.id])
            .select {
                (PostTable.user inList userIds) and
                        (PostTable.createdAt greaterEq sevenDays)
            }
            .orderBy(PostTable.createdAt to SortOrder.DESC)
            .limit(30)
            .map { row ->
                Post(
                    id = row[PostTable.id].value,
                    userId = row[PostTable.user].value,
                    username = row[UserTable.username],
                    photo1 = row[p1[PhotoTable.path]],
                    photo2 = row[p2[PhotoTable.path]],
                    text = row[PostTable.text],
                    emoji = row[PostTable.emoji],
                    timer = row[PostTable.timer],
                    createdAt = row[PostTable.createdAt]
                )
            }
    }

    suspend fun getAllPosts(): List<Post> = dbQuery {
        val p1 = PhotoTable.alias("p1")
        val p2 = PhotoTable.alias("p2")

        PostTable
            .innerJoin(UserTable, { PostTable.user }, { UserTable.id })
            .join(p1, JoinType.INNER, PostTable.photo1, p1[PhotoTable.id])
            .join(p2, JoinType.LEFT, PostTable.photo2, p2[PhotoTable.id])
            .selectAll()
            .orderBy(PostTable.createdAt to SortOrder.DESC)
            .map { row ->
                Post(
                    id = row[PostTable.id].value,
                    userId = row[PostTable.user].value,
                    username = row[UserTable.username],
                    photo1 = row[p1[PhotoTable.path]],
                    photo2 = row[p2[PhotoTable.path]],
                    text = row[PostTable.text],
                    emoji = row[PostTable.emoji],
                    timer = row[PostTable.timer],
                    createdAt = row[PostTable.createdAt]
                )
            }
    }

    suspend fun create(
        userId: Int,
        photo1Id: Int,
        photo2Id: Int,
        timer: Int,
        text: String?,
        emoji: String?
    ): Int = dbQuery {
        PostTable.insertAndGetId {
            it[PostTable.user] = EntityID(userId, UserTable)
            it[PostTable.photo1] = EntityID(photo1Id, PhotoTable)
            it[PostTable.photo2] = EntityID(photo2Id, PhotoTable)
            it[PostTable.text] = text
            it[PostTable.timer] = timer
            it[PostTable.emoji] = emoji
            it[PostTable.createdAt] = System.currentTimeMillis()
        }.value
    }

    suspend fun delete(postId: Int) = dbQuery {
        PostTable.deleteWhere { PostTable.id eq postId }
    }
}
