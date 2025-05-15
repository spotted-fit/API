package fit.spotted.api.models

import kotlinx.serialization.Serializable

@Serializable
data class CommentRequest(
    val text: String
)
