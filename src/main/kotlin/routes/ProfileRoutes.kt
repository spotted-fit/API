package routes

import db.dao.UserDao
import db.dao.PostDao
import db.tables.FriendRequestTable
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.*
import models.ErrorResponse
import models.OkResponse
import models.ProfilePost
import models.ProfileResponse
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import utils.buildFullPhotoUrl

fun Route.profileRoutes() {
    get("/profile/{username}") {
        val username = call.parameters["username"]
        if (username == null) {
            call.respond(ErrorResponse(message = "Invalid username"))
            return@get
        }

        val user = UserDao.findByUsername(username)
        if (user == null) {
            call.respond(ErrorResponse(message = "User not found"))
            return@get
        }

        val posts = PostDao.findByUserId(user.id).map {
            ProfilePost(
                id = it.id,
                photo1 = buildFullPhotoUrl(it.photo1),
                photo2 = buildFullPhotoUrl(it.photo2),
                emoji = it.emoji,
                text = it.text,
                createdAt = it.createdAt
            )
        }

        val friendsCount = transaction {
            FriendRequestTable
                .select {
                    (FriendRequestTable.fromUser eq user.id and (FriendRequestTable.status eq "accepted")) or
                            (FriendRequestTable.toUser eq user.id and (FriendRequestTable.status eq "accepted"))
                }
                .count()
        }

        call.respond(
            OkResponse(
                response = Json.encodeToJsonElement(ProfileResponse(
                    id = user.id,
                    username = user.username,
                    avatar = user.avatarPath,
                    friendsCount = friendsCount.toInt(),
                    posts = posts
                ))
            )
        )
    }

    get("/profile/{username}/posts") {
        val username = call.parameters["username"]
        if (username == null) {
            call.respond(ErrorResponse(message = "Invalid username"))
            return@get
        }

        val user = UserDao.findByUsername(username)
        if (user == null) {
            call.respond(ErrorResponse(message = "User not found"))
            return@get
        }

        val posts = PostDao.findByUserId(user.id).reversed()

        call.respond(OkResponse(response = buildJsonArray {
            posts.forEach { post ->
                add(buildJsonObject {
                    put("postId", post.id)
                    put("photo1", buildFullPhotoUrl(post.photo1))
                    put("photo2", buildFullPhotoUrl(post.photo2))
                    put("text", post.text ?: "")
                    put("emoji", post.emoji ?: "")
                    put("createdAt", post.createdAt.toString())
                })
            }
        }))
    }

}
