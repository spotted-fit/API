package security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.*

object JwtService {
    private const val issuer = "spotted"
    private const val validityInMs = 7 * 24 * 60 * 60 * 1000L // 7 days

    private val secret = System.getenv("JWT_SECRET") ?: error("JWT_SECRET is required!")
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .build()

    fun generateToken(userId: Int): String {
        return JWT.create()
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
            .sign(algorithm)
    }

    fun getUserIdFrom(jwt: DecodedJWT): Int? {
        return jwt.getClaim("userId").asInt()
    }
}
