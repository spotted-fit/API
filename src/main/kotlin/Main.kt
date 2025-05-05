import db.DatabaseFactory
import io.ktor.http.*
import io.ktor.http.auth.*
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

fun main() {
    DatabaseFactory.init()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json()
        }
        install(Authentication) {
            jwt("jwt-auth") {
                realm = "spotted"
                verifier(JwtService.verifier)
                validate { creds ->
                    creds.payload.getClaim("userId")?.asInt()
                        ?.let { JWTPrincipal(creds.payload) }
                }
                authHeader { call ->
                    call.request.cookies["auth_token"]
                        ?.let { HttpAuthHeader.Single("Bearer", it) }
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
            allowHeader(HttpHeaders.Cookie)
            allowCredentials = true
            anyHost() // Allow requests from any host - consider restricting to specific origins for better security
        }
        routing {
            authRoutes()
            authenticate("jwt-auth") {
                profileRoutes()
                postRoutes()
                userRoutes()
                friendshipRoutes()
                feedRoutes()
            }
//            debugRoutes()
        }
    }.start(wait = true)
}
