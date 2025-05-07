package db.dao

import db.DatabaseFactory.dbQuery
import db.tables.FriendRequestTable
import db.tables.UserTable
import models.FriendRequestPreview
import models.FriendshipRequestAnswer
import models.UserShortDto
import org.jetbrains.exposed.sql.*
import java.lang.IllegalStateException
import java.time.Instant

object FriendRequestDao {

    suspend fun sendFriendRequest(senderId: Int, receiverId: Int) = dbQuery {
        val existingRequest = FriendRequestTable
            .select {
                (FriendRequestTable.fromUser eq senderId) and
                        (FriendRequestTable.toUser eq receiverId) and
                        (FriendRequestTable.status eq "pending")
            }
            .singleOrNull()

        if (existingRequest != null) {
            throw IllegalStateException("Friend request already sent")
        }
        if (senderId == receiverId) {
            error("Can't send friend request to yourself")
        }

        FriendRequestTable.insert {
            it[fromUser] = senderId
            it[toUser] = receiverId
            it[status] = "pending"
            it[createdAt] = Instant.now().toEpochMilli()
        }
    }

    suspend fun respondToFriendRequest(userId: Int, answer: FriendshipRequestAnswer) = dbQuery {
        val request = FriendRequestTable
            .select { FriendRequestTable.id eq answer.requestId }
            .singleOrNull() ?: error("Request not found")

        val toUserId = request[FriendRequestTable.toUser].value

        if (toUserId != userId) {
            throw IllegalAccessException("You are not allowed to respond to this request")
        }

        FriendRequestTable.update({ FriendRequestTable.id eq answer.requestId }) {
            it[FriendRequestTable.status] = if (answer.accepted) "accepted" else "declined"
        }
    }



    suspend fun getIncomingRequests(userId: Int): List<FriendRequestPreview> = dbQuery {
        (FriendRequestTable
            .innerJoin(UserTable, { FriendRequestTable.fromUser }, { UserTable.id }))
            .select {
                (FriendRequestTable.toUser eq userId) and
                        (FriendRequestTable.status eq "pending")
            }
            .map {
                FriendRequestPreview(
                    requestId = it[FriendRequestTable.id].value,
                    fromId = it[FriendRequestTable.fromUser].value,
                    username = it[UserTable.username]
                )
            }
    }

    suspend fun getFriends(userId: Int): List<UserShortDto> = dbQuery {
        (FriendRequestTable
            .innerJoin(UserTable, { FriendRequestTable.toUser }, { UserTable.id }))
            .select {
                (FriendRequestTable.status eq "accepted") and
                        (FriendRequestTable.fromUser eq userId)
            }
            .map {
                UserShortDto(
                    id = it[FriendRequestTable.toUser].value,
                    username = it[UserTable.username]
                )
            } +
                (FriendRequestTable
                    .innerJoin(UserTable, { FriendRequestTable.fromUser }, { UserTable.id }))
                    .select {
                        (FriendRequestTable.status eq "accepted") and
                                (FriendRequestTable.toUser eq userId)
                    }
                    .map {
                        UserShortDto(
                            id = it[FriendRequestTable.fromUser].value,
                            username = it[UserTable.username]
                        )
                    }
    }

}
