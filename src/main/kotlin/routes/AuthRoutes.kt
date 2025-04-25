package routes

import db.dao.UserDao
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import models.LoginRequest
import models.LoginResponse
import org.mindrot.jbcrypt.BCrypt
import models.RegisterRequest
import security.JwtService

fun Route.authRoutes() {
    post("/register") {
        val body = call.receive<RegisterRequest>()
        val existing = UserDao.findByEmail(body.email)
        if (existing != null) {
            call.respond(HttpStatusCode.Conflict, "User already exists")
            return@post
        }
        val usernameTaken = UserDao.findByUsername(body.username)
        if (usernameTaken != null) {
            call.respond(HttpStatusCode.Conflict, "Username taken")
            return@post
        }
        val hashed = BCrypt.hashpw(body.password, BCrypt.gensalt())
        val user = UserDao.create(body.email, hashed, body.username)
        val token = JwtService.generateToken(user.id)
        call.respond(mapOf("token" to token))
    }

    post("/login") {
        val body = call.receive<LoginRequest>()
        val user = UserDao.findByEmail(body.email)
        if (user == null || !BCrypt.checkpw(body.password, user.passwordHash)) {
            call.respond(HttpStatusCode.Unauthorized, "Wrong email or password")
            return@post
        }

        val token = JwtService.generateToken(user.id)
        call.respond(mapOf("token" to token))

        call.respond(LoginResponse(token = token))
    }
}
