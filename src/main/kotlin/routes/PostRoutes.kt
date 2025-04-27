package routes

import db.dao.CommentDao
import db.dao.PostDao
import db.dao.LikeDao
import db.tables.PhotoTable
import db.tables.UserTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.content.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import models.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import utils.userIdOrThrow
import java.io.File

fun Route.postRoutes() {

    authenticate {
        post("/posts") {
            val userId = call.userIdOrThrow()
            val multipart = call.receiveMultipart()

            var photo1Bytes: ByteArray? = null
            var photo1Name: String? = null
            var photo2Bytes: ByteArray? = null
            var photo2Name: String? = null
            var text: String? = null
            var emoji: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "text" -> text = part.value
                            "emoji" -> emoji = part.value
                        }
                    }
                    is PartData.FileItem -> {
                        val name = part.originalFileName ?: "upload.jpg"
                        val bytes = part.streamProvider().readBytes()

                        if (photo1Bytes == null) {
                            photo1Bytes = bytes
                            photo1Name = name
                        } else if (photo2Bytes == null) {
                            photo2Bytes = bytes
                            photo2Name = name
                        }
                    }
                    else -> {}
                }
                part.dispose()
            }

            if (photo1Bytes == null || photo2Bytes == null) {
                call.respond(ErrorResponse(message = "There should be 2 photos"))
                return@post
            }

            val path1 = savePhoto(photo1Name!!, photo1Bytes!!)
            val path2 = savePhoto(photo2Name!!, photo2Bytes!!)

            val photo1Id = savePhotoToDb(userId, path1)
            val photo2Id = savePhotoToDb(userId, path2)

            val postId = PostDao.create(
                userId = userId,
                photo1Id = photo1Id,
                photo2Id = photo2Id,
                text = text,
                emoji = emoji
            )

            call.respond(OkResponse(response = buildJsonObject { put("postId", postId) }))
        }

        get("/posts/{id}") {
            val userId = call.userIdOrThrow()
            val postId = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(ErrorResponse(message = "Invalid post ID"))

            val post = PostDao.findById(postId)
                ?: return@get call.respond(ErrorResponse(message = "Post not found"))

            val comments = CommentDao.getComments(postId)
            val likesCount = LikeDao.countForPost(postId)
            val isLiked = LikeDao.isLikedByUser(userId, postId)

            call.respond(
                OkResponse(
                    response = Json.encodeToJsonElement(
                        GetPostResponse(
                            id = post.id,
                            userId = post.userId,
                            photo1 = post.photo1,
                            photo2 = post.photo2,
                            text = post.text,
                            emoji = post.emoji,
                            createdAt = post.createdAt,
                            likes = likesCount,
                            isLikedByMe = isLiked,
                            comments = comments
                        )
                    )
                )
            )
        }

        post("/posts/{id}/like") {
            val userId = call.userIdOrThrow()
            val postId = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(ErrorResponse(message = "There should be 2 photos"))

            LikeDao.likePost(userId, postId)
            call.respond(HttpStatusCode.OK)
        }

        delete("/posts/{id}/like") {
            val userId = call.userIdOrThrow()
            val postId = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(ErrorResponse(message = "There should be 2 photos"))

            LikeDao.unlikePost(userId, postId)
            call.respond(HttpStatusCode.OK)
        }

        post("/posts/{id}/comment") {
            val userId = call.userIdOrThrow()
            val postId = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(ErrorResponse(message = "There should be 2 photos"))
            val body = call.receive<CommentRequest>()

            CommentDao.addComment(postId, userId, body.text)
            call.respond(OkResponse(response = buildJsonObject {}))
        }

        get("/posts/{id}/comments") {
            val postId = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(ErrorResponse(message = "Invalid post ID"))

            val comments = CommentDao.getComments(postId)
            call.respond(OkResponse(response = Json.encodeToJsonElement(ListSerializer(Comment.serializer()), comments)))
        }
    }
}

private fun savePhoto(filename: String, bytes: ByteArray): String {
    val uploadDir = File("uploads")
    if (!uploadDir.exists()) uploadDir.mkdirs()

    val file = File(uploadDir, System.currentTimeMillis().toString() + "_" + filename)
    file.writeBytes(bytes)

    return "/uploads/${file.name}"
}

private fun savePhotoToDb(userId: Int, path: String): Int {
    return transaction {
        PhotoTable.insertAndGetId {
            it[PhotoTable.user] = EntityID(userId, UserTable)
            it[PhotoTable.path] = path
            it[PhotoTable.uploadedAt] = System.currentTimeMillis()
        }.value
    }
}
