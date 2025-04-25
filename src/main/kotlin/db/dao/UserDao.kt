package db.dao

import db.DatabaseFactory.dbQuery
import db.tables.UserTable
import org.jetbrains.exposed.sql.*

data class User(
    val id: Int,
    val email: String,
    val passwordHash: String,
    val username: String,
    val avatarPath: String?
)
object UserDao {

    suspend fun findByEmail(email: String): User? = dbQuery {
        UserTable
            .select { UserTable.email eq email }
            .mapNotNull { toUser(it) }
            .singleOrNull()
    }

    suspend fun findByUsername(username: String): User? = dbQuery {
        UserTable
            .select { UserTable.username eq username }
            .mapNotNull { toUser(it) }
            .singleOrNull()
    }

    suspend fun findById(id: Int): User? = dbQuery {
        UserTable
            .select { UserTable.id eq id }
            .mapNotNull { toUser(it) }
            .singleOrNull()
    }

    suspend fun create(email: String, passwordHash: String, username: String): User = dbQuery {
        val insertedId = UserTable.insertAndGetId {
            it[UserTable.email] = email
            it[UserTable.passwordHash] = passwordHash
            it[UserTable.username] = username
        }.value

        User(insertedId, email, passwordHash, username, avatarPath = null)
    }


    suspend fun searchByUsername(query: String): List<User> = dbQuery {
        UserTable
            .select { UserTable.username.lowerCase() like "%${query.lowercase()}%" }
            .map { toUser(it) }
    }

    private fun toUser(row: ResultRow): User = User(
        id = row[UserTable.id].value,
        email = row[UserTable.email],
        passwordHash = row[UserTable.passwordHash],
        username = row[UserTable.username],
        avatarPath = row[UserTable.avatarPath]
    )
}
