package models

import kotlinx.serialization.Serializable

@Serializable
data class GetPostResponse (
    val id: Int,
    val userId: Int,
    val photo1: String,
    val photo2: String?,
    val text: String?,
    val emoji: String?,
    val createdAt: Long,
    val likes: Int,
    val isLikedByMe: Boolean,
    val comments: List<Comment>
)