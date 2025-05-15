package routes

import fit.spotted.api.db.dao.CommentDao
import fit.spotted.api.db.dao.LikeDao
import fit.spotted.api.db.dao.PostDao
import fit.spotted.api.db.dao.ChallengeParticipantDao
import fit.spotted.api.db.tables.PhotoTable
import fit.spotted.api.db.tables.UserTable
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import fit.spotted.api.models.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import fit.spotted.api.storage.S3Config
import fit.spotted.api.storage.S3Uploader
import fit.spotted.api.utils.buildFullPhotoUrl
import fit.spotted.api.utils.generateRandom32
import fit.spotted.api.utils.userIdOrThrow
import java.awt.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.*
import com.madgag.gif.fmsware.AnimatedGifEncoder
import java.awt.geom.RoundRectangle2D
import java.awt.font.TextAttribute
import java.text.AttributedString

fun Route.postRoutes() {

    authenticate {
        post("/posts") {
            val userId = call.userIdOrThrow()
            val multipart = call.receiveMultipart()

            var photo1Bytes: ByteArray? = null
            var photo2Bytes: ByteArray? = null
            var timer = 0
            var text: String? = null
            var emoji: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "text" -> text = part.value
                            "emoji" -> emoji = part.value
                            "timer" -> timer = part.value.toInt()
                        }
                    }
                    is PartData.FileItem -> {
                        val bytes = part.streamProvider().readBytes()

                        if (photo1Bytes == null) {
                            photo1Bytes = bytes
                        } else if (photo2Bytes == null) {
                            photo2Bytes = bytes
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

            val uploader = S3Uploader(S3Config.s3Client, S3Config.bucketName)

            val randomDir1 = generateRandom32() + ".png"
            val randomDir2 = generateRandom32() + ".png"

            val path1 = uploader.uploadImage(photo1Bytes!!, randomDir1)
            val path2 = uploader.uploadImage(photo2Bytes!!, randomDir2)

            val photo1Id = savePhotoToDb(userId, path1)
            val photo2Id = savePhotoToDb(userId, path2)

            val postId = PostDao.create(
                userId = userId,
                photo1Id = photo1Id,
                photo2Id = photo2Id,
                timer = timer,
                text = text,
                emoji = emoji
            )
            
            // Update challenges progress - each post is considered a workout
            // Use the timer as workout duration in minutes
            if (timer > 0) {
                ChallengeParticipantDao.updateActiveChallengesProgress(
                    userId = userId,
                    workoutDurationMinutes = timer
                )
            }

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
                            username = post.username,
                            photo1 = buildFullPhotoUrl(post.photo1),
                            photo2 = buildFullPhotoUrl(post.photo2),
                            text = post.text,
                            emoji = post.emoji,
                            createdAt = post.createdAt,
                            likes = likesCount,
                            isLikedByMe = isLiked,
                            timer = post.timer,
                            comments = comments
                        )
                    )
                )
            )
        }

        authenticate {
            delete("/posts/{id}") {
                val userId = call.userIdOrThrow()
                val postId = call.parameters["id"]?.toIntOrNull()

                if (postId == null) {
                    call.respond(ErrorResponse(message = "Invalid post id"))
                    return@delete
                }

                val post = PostDao.findById(postId)
                if (post == null) {
                    call.respond(ErrorResponse(message = "Post not found"))
                    return@delete
                }

                if (post.userId != userId) {
                    call.respond(ErrorResponse(message = "You are not allowed to delete this post"))
                    return@delete
                }

                val uploader = S3Uploader(S3Config.s3Client, S3Config.bucketName)

                uploader.deleteImage(post.photo1)
                uploader.deleteImage(post.photo2)

                PostDao.delete(postId)

                call.respond(OkResponse(response = buildJsonObject {}))
            }
        }


        post("/posts/{id}/like") {
            val userId = call.userIdOrThrow()
            val postId = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(ErrorResponse(message = "Post not found"))

            LikeDao.likePost(userId, postId)
            call.respond(OkResponse(response = buildJsonObject {}))
        }

        delete("/posts/{id}/like") {
            val userId = call.userIdOrThrow()
            val postId = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(ErrorResponse(message = "Post not found"))

            LikeDao.unlikePost(userId, postId)
            call.respond(OkResponse(response = buildJsonObject {}))
        }

        post("/posts/{id}/comment") {
            val userId = call.userIdOrThrow()
            val postId = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(ErrorResponse(message = "Post not found"))
            val body = call.receive<CommentRequest>()

            CommentDao.addComment(postId, userId, body.text)
            call.respond(OkResponse(response = buildJsonObject {}))
        }

        get("/posts/{id}/comments") {
            val postId = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(ErrorResponse(message = "Post not found"))

            val comments = CommentDao.getComments(postId)
            call.respond(OkResponse(response = Json.encodeToJsonElement(ListSerializer(Comment.serializer()), comments)))
        }

        get("/posts/{id}/share") {
            val userId = call.userIdOrThrow()
            val postId = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(ErrorResponse(message = "Invalid post ID"))
            val post = PostDao.findById(postId)
                ?: return@get call.respond(ErrorResponse(message = "Post not found"))

            val s3     = S3Config.s3Client
            val bucket = S3Config.bucketName
            fun load(key: String) = s3.getObject(GetObjectRequest.builder()
                .bucket(bucket).key(key).build()).use { ImageIO.read(it)!! }

            var img1 = load(post.photo1)
            var img2 = load(post.photo2)

            fun resize(src: BufferedImage, w: Int, h: Int): BufferedImage {
                val tmp = src.getScaledInstance(w, h, Image.SCALE_SMOOTH)
                val dst = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
                val g = dst.createGraphics()
                g.drawImage(tmp, 0, 0, null); g.dispose()
                return dst
            }
            val targetW = img1.width / 2
            val targetH = img1.height / 2
            img1 = resize(img1, targetW, targetH)
            img2 = resize(img2, targetW, targetH)

            val fps       = 10
            val delayMs   = 100
            val fadeSteps = fps
            val frames    = mutableListOf<BufferedImage>()

            repeat(fps) { frames += drawOverlay(img1, post.text, post.emoji, post.timer.toString()) }

            (1..fadeSteps).forEach { i ->
                val α2 = i.toFloat() / fadeSteps; val α1 = 1 - α2
                val buf = BufferedImage(img1.width, img1.height, BufferedImage.TYPE_INT_RGB)
                val g2 = buf.createGraphics().apply {
                    setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON)
                    composite = AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, α1)
                    drawImage(img1, 0, 0, null)
                    composite = AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, α2)
                    drawImage(img2, 0, 0, null)
                    dispose()
                }
                frames += drawOverlay(buf, post.text, post.emoji, post.timer.toString())
            }

            repeat(fps) { frames += drawOverlay(img2, post.text, post.emoji, post.timer.toString()) }

            (1..fadeSteps).forEach { i ->
                val α1 = i.toFloat() / fadeSteps; val α2 = 1 - α1
                val buf = BufferedImage(img1.width, img1.height, BufferedImage.TYPE_INT_RGB)
                val g2 = buf.createGraphics().apply {
                    setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON)
                    composite = AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, α2)
                    drawImage(img2, 0, 0, null)
                    composite = AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, α1)
                    drawImage(img1, 0, 0, null)
                    dispose()
                }
                frames += drawOverlay(buf, post.text, post.emoji, post.timer.toString())
            }

            val baos = ByteArrayOutputStream()
            val encoder = AnimatedGifEncoder().apply {
                start(baos)
                setRepeat(0)
                setDelay(delayMs)
                setQuality(20)
            }
            frames.forEach { frame ->
                encoder.addFrame(frame)
            }
            encoder.finish()

            call.response.header(HttpHeaders.CacheControl, "no-store, no-cache")
            call.respondBytes(baos.toByteArray(), ContentType.Image.GIF)
        }
    }
}

fun drawOverlay(
    base: BufferedImage,
    text: String?,
    emoji: String?,
    duration: String
): BufferedImage {
    val w = base.width
    val h = base.height
    val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)

    val g = out.createGraphics().apply {
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
        setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        drawImage(base, 0, 0, null)
    }


    val gradientHeight = (h * 0.15f).toInt()
    g.paint = GradientPaint(0f, h.toFloat(), Color(0, 0, 0, 210), 0f, (h - gradientHeight).toFloat(), Color(0, 0, 0, 0))
    g.fillRect(0, h - gradientHeight, w, gradientHeight)
    g.paint = GradientPaint(0f, 0f, Color(0, 0, 0, 120), 0f, 70f, Color(0, 0, 0, 0))
    g.fillRect(0, 0, w, 70)

    val plainFont = Font("Sans Serif", Font.PLAIN,  18)
    val italicBoldFont = Font("Sans Serif", Font.BOLD or Font.ITALIC,  30)
    val boldFont = Font("Sans Serif", Font.BOLD,  30)
    val panelHeight = 60
    val panelY = h - panelHeight
    g.color = Color(0, 0, 0, 150)
    g.fillRect(0, panelY, w, panelHeight)

    if (!text.isNullOrEmpty() || !emoji.isNullOrEmpty()) {
        val displayText = buildString {
            if (!text.isNullOrEmpty()) append(text)
            if (!emoji.isNullOrEmpty()) {
                if (isNotEmpty()) append("  ")
                append(emoji)
            }
        }
        g.font = plainFont
        g.color = Color.WHITE
        val fm = g.fontMetrics
        val textY = panelY + (panelHeight + fm.ascent - fm.descent) / 2
        g.drawString(displayText, 24, textY)
    }

    if (duration.isNotEmpty()) {
        val durText = "$duration min"
        g.font = plainFont
        g.color = Color.WHITE
        val fmD = g.fontMetrics
        val durWidth = fmD.stringWidth(durText)
        val x = w - durWidth - 24
        val y = panelY + (panelHeight + fmD.ascent - fmD.descent) / 2
        g.drawString(durText, x, y)
    }

    val full = "Join me on spotted.fit!"
    val link = "spotted.fit"
    val asText = AttributedString(full).apply {
        addAttribute(TextAttribute.FONT, boldFont, 0, full.length)
        val start = full.indexOf(link)
        val end   = start + link.length
        addAttribute(TextAttribute.FONT, italicBoldFont, start, end)
        addAttribute(TextAttribute.FOREGROUND, Color.WHITE, 0, full.length)
    }

    g.drawString(asText.iterator, w / 2 - g.fontMetrics.stringWidth(asText.toString()) / 2 - 15, 40)

    g.dispose()
    return out
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
