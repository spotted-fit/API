package fit.spotted.api.db.dao

import fit.spotted.api.db.DatabaseFactory.dbQuery
import fit.spotted.api.db.tables.UserTable
import org.jetbrains.exposed.sql.*

data class User(
    val id: Int,
    val email: String,
    val passwordHash: String,
    val username: String,
    val avatarPath: String?,
    val firebaseToken: String? = null
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

    suspend fun create(email: String, passwordHash: String, username: String, firebaseToken: String? = null): User = dbQuery {
        val insertedId = UserTable.insertAndGetId {
            it[UserTable.email] = email
            it[UserTable.passwordHash] = passwordHash
            it[UserTable.username] = username
            it[UserTable.firebaseToken] = firebaseToken
        }.value

        User(insertedId, email, passwordHash, username, avatarPath = null, firebaseToken = firebaseToken)
    }

    suspend fun updateAvatar(userId: Int, avatarPath: String): Boolean = dbQuery {
        val updatedRows = UserTable.update({ UserTable.id eq userId }) {
            it[UserTable.avatarPath] = avatarPath
        }
        updatedRows > 0
    }

    suspend fun searchByUsername(query: String): List<User> = dbQuery {
        UserTable
            .select { UserTable.username.lowerCase() like "%${query.lowercase()}%" }
            .map { toUser(it) }
    }

    suspend fun updateFirebaseToken(userId: Int, token: String?): Boolean = dbQuery {
        val updated = UserTable.update({ UserTable.id eq userId }) {
            it[firebaseToken] = token
        }
        updated > 0
    }

    private fun toUser(row: ResultRow): User = User(
        id = row[UserTable.id].value,
        email = row[UserTable.email],
        passwordHash = row[UserTable.passwordHash],
        username = row[UserTable.username],
        avatarPath = row[UserTable.avatarPath],
        firebaseToken = row[UserTable.firebaseToken]
    )
}
