package models

import kotlinx.serialization.Serializable

@Serializable
data class CreatePostRequest(
    val emoji: String? = null,
    val text: String? = null
)