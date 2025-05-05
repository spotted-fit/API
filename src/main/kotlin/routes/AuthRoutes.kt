package routes

import db.dao.UserDao
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import models.*
import org.mindrot.jbcrypt.BCrypt
import security.JwtService
import io.ktor.http.CookieEncoding
import io.ktor.http.Cookie.*

fun Route.authRoutes() {
    post("/register") {
        val body = call.receive<RegisterRequest>()

        val existing = UserDao.findByEmail(body.email)
        if (existing != null) {
            call.respond(ErrorResponse(message = "User with this email already exists"))
            return@post
        }

        val usernameRegex = Regex("^[A-Za-z0-9_]{3,20}$")

        if (!usernameRegex.matches(body.username)) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(message = "Invalid username"))
            return@post
        }
        val usernameTaken = UserDao.findByUsername(body.username)
        if (usernameTaken != null) {
            call.respond(ErrorResponse(message = "Username taken"))
            return@post
        }
        val hashed = BCrypt.hashpw(body.password, BCrypt.gensalt())
        val user = UserDao.create(body.email, hashed, body.username)
        val token = JwtService.generateToken(user.id)
        call.response.cookies.append(
            name     = "auth_token",
            value    = token,
            maxAge   = 365L * 24 * 60 * 60,              // 1 year
            path     = "/",
            secure   = false,
            httpOnly = true,
            extensions = mapOf("SameSite" to "Lax")
        )

        call.respond(OkResponse(response = buildJsonObject { put("token", token) }))
    }

    post("/login") {
        val body = call.receive<LoginRequest>()

        val email = body.email?.trim()
        val username = body.username?.trim()
        val password = body.password.trim()

        if ((email.isNullOrEmpty() && username.isNullOrEmpty()) || password.isEmpty()) {
            call.respond(ErrorResponse(message = "Provide either email or username, and password"))
            return@post
        }
        val user = if (!email.isNullOrEmpty()) {
            UserDao.findByEmail(email)
        } else {
            UserDao.findByUsername(username!!)
        }
        if (user == null || !BCrypt.checkpw(body.password, user.passwordHash)) {
            call.respond(ErrorResponse(message = "Wrong email, username or password"))
            return@post
        }
        if (!email.isNullOrEmpty() && !username.isNullOrEmpty()) {
            if (user.username != username || user.email != email) {
                call.respond(ErrorResponse(message = "Email and username of the account do not match"))
                return@post
            }
        }

        val token = JwtService.generateToken(user.id)
        call.response.cookies.append(
            name     = "auth_token",
            value    = token,
            maxAge   = 365L * 24 * 60 * 60,              // 1 year
            path     = "/",
            secure   = false,
            httpOnly = true,
            extensions = mapOf("SameSite" to "Lax")
        )

        call.respond(OkResponse(response = buildJsonObject { put("token", token) }))
    }
}
