package models

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: Int,
    val userId: Int,
    val photo1: String,
    val photo2: String,
    val text: String?,
    val emoji: String?,
    val createdAt: Long,
    val duration: Int
)