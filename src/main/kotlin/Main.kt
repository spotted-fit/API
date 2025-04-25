import db.DatabaseFactory
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
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

        routing {
            authRoutes()
            profileRoutes()
            postRoutes()
            userRoutes()
//            debugRoutes()
        }
    }.start(wait = true)
}
