import db.DatabaseFactory
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import models.ErrorResponse
import org.jetbrains.exposed.exceptions.ExposedSQLException
import routes.*
import security.JwtService

fun main() {
    DatabaseFactory.init()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json()
        }
        install(Authentication) {
            jwt {
                verifier(JwtService.verifier)
                validate { credential ->
                    val userId = credential.payload.getClaim("userId").asInt()
                    if (userId != null) JWTPrincipal(credential.payload) else null
                }
            }
        }
        install(StatusPages) {
            exception<IllegalArgumentException> { call, cause ->
                call.respond(ErrorResponse(message = "Bad request. " + cause.message))
            }
            exception<IllegalAccessException> { call, cause ->
                call.respond(ErrorResponse(message = "Forbidden. " + cause.message))
            }
            exception<IllegalStateException> { call, cause ->
                call.respond(ErrorResponse(message = "Illegal state. " + cause.message))
            }
            exception<NoSuchElementException> { call, cause ->
                call.respond(ErrorResponse(message = "Not found. " + cause.message))
            }
            exception<BadRequestException> { call, cause ->
                call.respond(ErrorResponse(message = "Invalid JSON format. " + cause.message))
            }
            exception<ExposedSQLException> { call, cause ->
                call.respond(ErrorResponse(message = "Invalid value. " + cause.message))
            }
            exception<Throwable> { call, cause ->
                call.respond(ErrorResponse(message = "Internal server error. " + cause.message))
            }
        }
        routing {
            authRoutes()
            profileRoutes()
            postRoutes()
            userRoutes()
            friendshipRoutes()
            feedRoutes()
//            debugRoutes()
        }
    }.start(wait = true)
}
