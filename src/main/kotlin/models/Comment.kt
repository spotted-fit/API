package models

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: Int,
    val userId: Int,
    val text: String,
    val createdAt: Long
)
