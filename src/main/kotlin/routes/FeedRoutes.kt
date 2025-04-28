package routes

import db.dao.FriendRequestDao
import db.dao.PostDao
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import models.OkResponse
import utils.buildFullPhotoUrl
import utils.userIdOrThrow

fun Route.feedRoutes() {
    authenticate {
        get("/feed") {
            val userId = call.userIdOrThrow()

            val friendIds = FriendRequestDao.getFriends(userId).map { it.id }

            if (friendIds.isEmpty()) {
                call.respond(OkResponse(response = buildJsonArray { }))
                return@get
            }

            val posts = PostDao.getRecentPosts(friendIds)

            call.respond(OkResponse(response = buildJsonArray {
                posts.forEach { post ->
                    add(buildJsonObject {
                        put("postId", post.id)
                        put("photo1Url", buildFullPhotoUrl(post.photo1))
                        put("photo2Url", buildFullPhotoUrl(post.photo2))
                        put("text", post.text ?: "")
                        put("emoji", post.emoji ?: "")
                        put("createdAt", post.createdAt.toString())
                    })
                }
            }))
        }

    }
}