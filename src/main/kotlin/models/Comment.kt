package fit.spotted.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: Int,
    val userId: Int,
    val username: String,
    val text: String,
    val createdAt: Long
)
