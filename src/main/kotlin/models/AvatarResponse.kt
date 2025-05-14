package models

import kotlinx.serialization.Serializable

@Serializable
data class AvatarResponse(
    val avatarUrl: String
) 