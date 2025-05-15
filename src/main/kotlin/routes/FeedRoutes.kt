package routes

import fit.spotted.api.db.dao.CommentDao
import fit.spotted.api.db.dao.LikeDao
import fit.spotted.api.db.dao.PostDao
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import fit.spotted.api.models.GetPostResponse
import fit.spotted.api.models.OkResponse
import fit.spotted.api.utils.buildFullPhotoUrl
import fit.spotted.api.utils.userIdOrThrow

fun Route.feedRoutes() {
    authenticate {
        get("/feed") {
            val userId = call.userIdOrThrow()

            val posts = PostDao.getAllPosts()
            val postResponses = posts.map { post ->
                val comments = CommentDao.getComments(post.id)
                val likesCount = LikeDao.countForPost(post.id)
                val isLiked = LikeDao.isLikedByUser(userId, post.id)

                GetPostResponse(
                    id = post.id,
                    userId = post.userId,
                    username = post.username,
                    photo1 = buildFullPhotoUrl(post.photo1),
                    photo2 = buildFullPhotoUrl(post.photo2),
                    text = post.text,
                    emoji = post.emoji,
                    createdAt = post.createdAt,
                    likes = likesCount,
                    timer = post.timer,
                    isLikedByMe = isLiked,
                    comments = comments
                )
            }

            call.respond(OkResponse(response = Json.encodeToJsonElement(postResponses)))
        }
    }
}
