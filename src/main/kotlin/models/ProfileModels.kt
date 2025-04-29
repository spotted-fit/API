package models

import kotlinx.serialization.Serializable

@Serializable
data class ProfileResponse(
    val id: Int,
    val avatar: String?,
    val friendsCount: Int,
    val posts: List<ProfilePost>
)

@Serializable
data class ProfilePost(
    val id: Int,
    val photo1: String,
    val photo2: String,
    val emoji: String?,
    val text: String?,
    val duration: Int,
    val createdAt: Long
)
