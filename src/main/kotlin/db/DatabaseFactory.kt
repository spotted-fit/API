package db

import db.tables.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val url = "jdbc:postgresql://localhost:5432/postgres"
        val driver = "org.postgresql.Driver"
        val user = "postgres"
        val password = System.getenv("DB_PASSWORD") ?: error("DB_PASSWORD is required!")

        Database.connect(url, driver, user, password)

        transaction {
            SchemaUtils.create(
                UserTable,
                FriendRequestTable,
                PhotoTable,
                PostTable,
                LikeTable,
                CommentTable
            )
        }
    }

    inline fun <T> dbQuery(crossinline block: () -> T): T =
        transaction { block() }
}
