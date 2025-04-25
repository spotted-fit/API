package models

import kotlinx.serialization.Serializable

@Serializable
data class CommentRequest(
    val text: String
)
