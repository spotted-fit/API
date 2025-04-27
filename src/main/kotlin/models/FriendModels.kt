package models

import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data class FriendRequestPreview(
    val requestId: Int,
    val fromId: Int,
    val username: String
)

@Serializable
data class FriendshipRequestCreate(
    val toId: Int
)

@Serializable
data class FriendshipRequestAnswer(
    val requestId: Int,
    val accepted: Boolean
)

@Serializable
data class UserShortDto(
    val id: Int,
    val username: String
)