package fit.spotted.api.db

import fit.spotted.api.db.tables.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val url = System.getenv("DB_URL") ?: error("DB_URL is required!")
        val driver = "org.postgresql.Driver"
        val user = System.getenv("DB_USER") ?: error("DB_USER is required!")
        val password = System.getenv("DB_PASSWORD") ?: error("DB_PASSWORD is required!")

        Database.connect(url, driver, user, password)

        transaction {
            SchemaUtils.create(
                UserTable,
                FriendRequestTable,
                PhotoTable,
                PostTable,
                LikeTable,
                CommentTable,
                ChallengeTable,
                ChallengeParticipantTable,
                ChallengeInviteTable,
                AchievementTable
            )
        }
    }

    inline fun <T> dbQuery(crossinline block: () -> T): T =
        transaction { block() }
}
