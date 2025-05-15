package fit.spotted.api.routes

import fit.spotted.api.db.dao.UserDao
import fit.spotted.api.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.mindrot.jbcrypt.BCrypt
import fit.spotted.api.security.JwtService

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
        val user = UserDao.create(body.email, hashed, body.username, body.firebaseToken)
        val token = JwtService.generateToken(user.id)
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
            call.respond(ErrorResponse(message = "Wrong username or password"))
            return@post
        }
        if (!email.isNullOrEmpty() && !username.isNullOrEmpty()) {
            if (user.username != username || user.email != email) {
                call.respond(ErrorResponse(message = "Email and username of the account do not match"))
                return@post
            }
        }

        if (body.firebaseToken != null) {
            UserDao.updateFirebaseToken(user.id, body.firebaseToken)
        }

        val token = JwtService.generateToken(user.id)

        call.respond(OkResponse(response = buildJsonObject { put("token", token) }))
    }
}
