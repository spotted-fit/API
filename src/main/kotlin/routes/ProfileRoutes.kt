package routes

import fit.spotted.api.db.dao.UserDao
import fit.spotted.api.db.dao.PostDao
import fit.spotted.api.db.tables.FriendRequestTable
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.*
import fit.spotted.api.models.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import fit.spotted.api.storage.S3Config
import fit.spotted.api.storage.S3Uploader
import fit.spotted.api.utils.AuthExtensions.getUserId
import fit.spotted.api.utils.ImageProcessor
import fit.spotted.api.utils.buildFullPhotoUrl
import java.util.*

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
                    avatar = buildFullPhotoUrl(user.avatarPath),
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

    authenticate {
        post("/profile/avatar") {
            // Get user ID from JWT token
            val userId = call.getUserId()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(message = "Authentication required"))
                return@post
            }

            // Ensure user exists
            val user = UserDao.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(message = "User not found"))
                return@post
            }

            // Process multipart data
            var avatarBytes: ByteArray? = null
            var fileExtension = "jpg"

            // Use suspendable function to process the multipart data
            try {
                call.receiveMultipart().forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            if (part.name == "avatar") {
                                avatarBytes = part.streamProvider().readBytes()
                                
                                // Extract the file extension from the original filename
                                part.originalFileName?.let { originalFilename ->
                                    val extension = originalFilename.substringAfterLast('.', "")
                                    if (extension.isNotEmpty()) {
                                        fileExtension = extension.lowercase()
                                    }
                                }
                            }
                        }
                        else -> {} // Ignore other parts
                    }
                    part.dispose()
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(message = "Failed to process upload: ${e.message}"))
                return@post
            }

            // Validate image data
            if (avatarBytes == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(message = "No avatar image provided"))
                return@post
            }

            // Validate image format
            if (!ImageProcessor.isValidImage(avatarBytes!!)) {
                call.respond(
                    HttpStatusCode.BadRequest, 
                    ErrorResponse(message = "Invalid image format. Please upload a JPEG or PNG file.")
                )
                return@post
            }

            // Process image (resize, compress)
            try {
                // Determine format from actual image data if possible
                val detectedFormat = ImageProcessor.getImageFormat(avatarBytes!!) ?: when (fileExtension) {
                    "png" -> "png"
                    else -> "jpg"
                }
                
                // Process the image
                val processedImage = ImageProcessor.process(
                    imageBytes = avatarBytes!!, 
                    targetWidth = 400, 
                    targetHeight = 400,
                    format = detectedFormat
                )
                
                // Generate a unique filename
                val filename = "avatar_${userId}_${UUID.randomUUID()}.${detectedFormat}"
                
                // Upload to S3
                val s3Uploader = S3Uploader(S3Config.s3Client, S3Config.bucketName)
                val avatarPath = s3Uploader.uploadImage(processedImage, filename)
                
                // Update the user's avatar path in the database
                val updated = UserDao.updateAvatar(userId, avatarPath)
                if (!updated) {
                    call.respond(
                        HttpStatusCode.InternalServerError, 
                        ErrorResponse(message = "Failed to update avatar in database")
                    )
                    return@post
                }
                
                // Return success response with the avatar URL
                call.respond(
                    OkResponse(
                        response = Json.encodeToJsonElement(
                            AvatarResponse(avatarUrl = buildFullPhotoUrl(avatarPath))
                        )
                    )
                )
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError, 
                    ErrorResponse(message = "Failed to process the image: ${e.message}")
                )
            }
        }
    }
}
