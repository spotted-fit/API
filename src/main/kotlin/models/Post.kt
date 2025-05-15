package fit.spotted.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: Int,
    val userId: Int,
    val username: String,
    val photo1: String,
    val photo2: String,
    val timer: Int,
    val text: String?,
    val emoji: String?,
    val createdAt: Long
)
