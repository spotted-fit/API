package models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val username: String
)

@Serializable
data class LoginResponse(
    val token: String
)
