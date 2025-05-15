package fit.spotted.api.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String,
    val firebaseToken: String? = null
)

@Serializable
data class LoginRequest(
    val email: String? = null,
    val password: String,
    val username: String? = null,
    val firebaseToken: String? = null
)
