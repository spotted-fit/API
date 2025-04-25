package utils

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

suspend fun ApplicationCall.userIdOrThrow(): Int {
    val principal = this.principal<JWTPrincipal>()
        ?: throw IllegalArgumentException("Missing JWT Principal")

    return principal.payload.getClaim("userId").asInt()
        ?: throw IllegalArgumentException("Missing userId claim")
}
