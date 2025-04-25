package models

import kotlinx.serialization.Serializable

@Serializable
data class UserPreview(
    val id: Int,
    val username: String,
    val avatar: String?
)
