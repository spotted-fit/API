import db.DatabaseFactory
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.ErrorResponse
import org.jetbrains.exposed.exceptions.ExposedSQLException
import routes.*
import security.JwtService
import utils.initializeFirebaseApp

fun main() {
    DatabaseFactory.init()
    initializeFirebaseApp()

    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
        host = System.getenv("HOST") ?: "0.0.0.0"
    ) {
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
        install(CORS) {
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Patch)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.AccessControlAllowOrigin)
            allowCredentials = true
            anyHost() // Allow requests from any host
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
