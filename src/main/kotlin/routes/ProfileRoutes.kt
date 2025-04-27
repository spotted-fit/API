package routes

import db.dao.UserDao
import db.dao.PostDao
import db.tables.FriendRequestTable
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import models.ErrorResponse
import models.OkResponse
import models.ProfilePost
import models.ProfileResponse
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.profileRoutes() {
    get("/profile/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(ErrorResponse(message = "Invalid Id"))
            return@get
        }

        val user = UserDao.findById(id)
        if (user == null) {
            call.respond(ErrorResponse(message = "User not found"))
            return@get
        }

        val posts = PostDao.findByUserId(user.id).map {
            ProfilePost(
                id = it.id,
                photo1 = it.photo1,
                photo2 = it.photo2,
                emoji = it.emoji,
                text = it.text,
                createdAt = it.createdAt
            )
        }

        val friendsCount = transaction {
            FriendRequestTable
                .select {
                    (FriendRequestTable.fromUser eq id and (FriendRequestTable.status eq "accepted")) or
                            (FriendRequestTable.toUser eq id and (FriendRequestTable.status eq "accepted"))
                }
                .count()
        }

        call.respond(
            OkResponse(
                response = Json.encodeToJsonElement(ProfileResponse(
                    id = user.id,
                    avatar = user.avatarPath,
                    friendsCount = friendsCount.toInt(),
                    posts = posts
                ))
            )
        )
    }
}
