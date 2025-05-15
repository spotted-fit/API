package routes

import fit.spotted.api.db.dao.*
import fit.spotted.api.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import fit.spotted.api.utils.userIdOrThrow

@Serializable
data class RecordWorkoutRequest(
    val durationMinutes: Int,
    val description: String? = null
)

@Serializable
data class WorkoutRecordedResponse(
    val updatedChallenges: Int,
    val newAchievements: List<Achievement> = emptyList()
)

fun Route.challengeRoutes() {
    authenticate {
        // 1. GET /challenges - get all challenges for authenticated user
        get("/challenges") {
            val userId = call.userIdOrThrow()
            val challenges = ChallengeDao.findAll(userId)
            
            call.respond(ChallengesResponse(challenges = challenges))
        }
        
        // 2. GET /challenges/:id - get specific challenge details
        get("/challenges/{id}") {
            val userId = call.userIdOrThrow()
            val challengeId = call.parameters["id"]?.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid challenge ID")

            val challenge = ChallengeDao.findById(challengeId)
                ?: throw NoSuchElementException("Challenge not found")
                
            // Verify user is a participant or has been invited
            val isParticipant = ChallengeParticipantDao.isParticipant(challengeId, userId)
            val invite = ChallengeInviteDao.findByChallengeAndUser(challengeId, userId)
            
            if (!isParticipant && invite == null) {
                throw IllegalAccessException("You don't have access to this challenge")
            }
            
            call.respond(ChallengeResponse(challenge = challenge))
        }
        
        // 3. POST /challenges - create new challenge
        post("/challenges") {
            val userId = call.userIdOrThrow()
            val request = call.receive<CreateChallengeRequest>()
            
            // Validate request
            if (request.name.isBlank()) {
                throw IllegalArgumentException("Challenge name cannot be empty")
            }
            
            if (request.startDate >= request.endDate) {
                throw IllegalArgumentException("End date must be after start date")
            }
            
            if (request.targetDuration <= 0) {
                throw IllegalArgumentException("Target duration must be positive")
            }
            
            // Create the challenge
            val challenge = ChallengeDao.create(
                name = request.name,
                description = request.description,
                startDate = request.startDate,
                endDate = request.endDate,
                targetDuration = request.targetDuration,
                createdById = userId
            ) ?: throw IllegalStateException("Failed to create challenge")
            
            // Process invites
            for (username in request.invitedUsernames) {
                val invitedUser = UserDao.findByUsername(username)
                if (invitedUser != null) {
                    ChallengeInviteDao.create(
                        challengeId = challenge.id,
                        invitedUserId = invitedUser.id,
                        invitedById = userId
                    )
                }
            }
            
            call.respond(ChallengeResponse(challenge = challenge))
        }
        
        // 4. GET /challenges/invites - get pending challenge invites
        get("/challenges/invites") {
            val userId = call.userIdOrThrow()
            val invites = ChallengeInviteDao.findAllByUser(userId)
            
            call.respond(ChallengeInvitesResponse(invites = invites))
        }
        
        // 5. POST /challenges/invites/respond - respond to a challenge invite
        post("/challenges/invites/respond") {
            val userId = call.userIdOrThrow()
            val request = call.receive<RespondToChallengeInviteRequest>()
            
            val invite = ChallengeInviteDao.findByChallengeAndUser(request.challengeId, userId)
                ?: throw NoSuchElementException("Invite not found")
            
            // Delete the invitation regardless of response
            ChallengeInviteDao.delete(invite.id)
            
            // If accepted, add user as participant
            if (request.accepted) {
                ChallengeParticipantDao.add(request.challengeId, userId)
            }
            
            call.respond(HttpStatusCode.OK)
        }
        
        // 6. DELETE /challenges/:id/leave - leave a challenge
        delete("/challenges/{id}/leave") {
            val userId = call.userIdOrThrow()
            val challengeId = call.parameters["id"]?.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid challenge ID")

            val challenge = ChallengeDao.findById(challengeId)
                ?: throw NoSuchElementException("Challenge not found")
                
            // Check if user is a participant
            if (!ChallengeParticipantDao.isParticipant(challengeId, userId)) {
                throw IllegalStateException("You are not a participant in this challenge")
            }
            
            // Remove the user but keep their contribution in the challenge total
            ChallengeParticipantDao.remove(challengeId, userId)
            
            call.respond(HttpStatusCode.OK)
        }
        
        // 7. GET /achievements - get all achievements for the authenticated user
        get("/achievements") {
            val userId = call.userIdOrThrow()
            val achievements = AchievementDao.findAllByUser(userId)
            
            call.respond(AchievementsResponse(achievements = achievements))
        }
        
        // 8. GET /achievements/:id - get details of a specific achievement
        get("/achievements/{id}") {
            val userId = call.userIdOrThrow()
            val achievementId = call.parameters["id"]?.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid achievement ID")
                
            val achievement = AchievementDao.findById(achievementId)
                ?: throw NoSuchElementException("Achievement not found")
                
            // Check if achievement belongs to the user
            if (achievement.userId != userId) {
                throw IllegalAccessException("You don't have access to this achievement")
            }
            
            call.respond(AchievementResponse(achievement = achievement))
        }
    }
} 