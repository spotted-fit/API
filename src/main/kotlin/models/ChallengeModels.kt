package fit.spotted.api.models

import kotlinx.serialization.Serializable

/**
 * Represents a group challenge where users can work together to reach a target workout duration.
 * Workout minutes are calculated from user posts, where the post's timer field represents
 * the workout duration in minutes.
 */
@Serializable
data class Challenge(
    val id: Int,
    val name: String,
    val description: String,
    val startDate: Long,  // Unix timestamp in ms
    val endDate: Long,    // Unix timestamp in ms
    val targetDuration: Int,  // Target workout time in minutes
    val currentProgress: Int, // Current total workout time in minutes
    val participants: List<ChallengeParticipant>,
    val createdBy: UserPreview,
    val isCompleted: Boolean
)

@Serializable
data class ChallengeParticipant(
    val user: UserPreview,
    val contributedMinutes: Int,
    val joinedAt: Long  // Unix timestamp in ms
)

@Serializable
data class ChallengeInvite(
    val id: Int,
    val challenge: Challenge,
    val invitedBy: UserPreview,
    val invitedAt: Long  // Unix timestamp in ms
)

@Serializable
data class Achievement(
    val id: Int,
    val userId: Int,
    val name: String,
    val description: String,
    val iconUrl: String,
    val earnedAt: Long,  // Unix timestamp in ms
    val challengeId: Int?  // Null if not challenge-related
)

// Request models
@Serializable
data class CreateChallengeRequest(
    val name: String,
    val description: String,
    val startDate: Long,  // Unix timestamp in ms
    val endDate: Long,    // Unix timestamp in ms
    val targetDuration: Int,  // Minutes
    val invitedUsernames: List<String>
)

@Serializable
data class RespondToChallengeInviteRequest(
    val challengeId: Int,
    val accepted: Boolean
)

// Response models
@Serializable
data class ChallengesResponse(
    val challenges: List<Challenge>
)

@Serializable
data class ChallengeResponse(
    val challenge: Challenge
)

@Serializable
data class ChallengeInvitesResponse(
    val invites: List<ChallengeInvite>
)

@Serializable
data class AchievementsResponse(
    val achievements: List<Achievement>
)

@Serializable
data class AchievementResponse(
    val achievement: Achievement
) 