package utils

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

object AuthExtensions {
    fun ApplicationCall.getUserId(): Int? {
        val principal = this.principal<JWTPrincipal>() ?: return null
        return principal.payload.getClaim("userId").asInt()
    }
}

suspend fun ApplicationCall.userIdOrThrow(): Int {
    val principal = this.principal<JWTPrincipal>()
        ?: throw IllegalArgumentException("Missing JWT Principal")

    return principal.payload.getClaim("userId").asInt()
        ?: throw IllegalArgumentException("Missing userId claim")
}
