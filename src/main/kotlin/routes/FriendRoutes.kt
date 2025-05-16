package routes

import com.google.firebase.messaging.FirebaseMessaging
import fit.spotted.api.db.dao.FriendRequestDao
import fit.spotted.api.db.dao.UserDao
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import fit.spotted.api.models.*
import fit.spotted.api.utils.AuthExtensions.getUserId
import fit.spotted.api.utils.createFirebaseNotification
import fit.spotted.api.utils.sendFriendActivityNotification
import fit.spotted.api.utils.userIdOrThrow

fun Route.friendshipRoutes() {
    authenticate {
        route("/friends") {

            post("/poke") {
                val request = call.receive<PokeRequest>()
                val senderId = call.getUserId() ?: throw IllegalArgumentException("User not logged in")
                val sender = UserDao.findById(senderId) ?: throw IllegalArgumentException("User not found")
                val recipient = UserDao.findByUsername(request.toUsername)
                    ?: throw IllegalArgumentException("User with username ${request.toUsername} not found")

                if (recipient.firebaseToken == null) {
                    throw IllegalArgumentException("User ${request.toUsername} does not have a Firebase token")
                }

                val response = sendFriendActivityNotification(
                    sender = sender,
                    recipient = recipient, 
                    action = "spotted you!",
                    message = "Go show them what activity you're up to today!"
                ) ?: throw IllegalArgumentException("Failed to send notification")
                
                call.respond(OkResponse(response = buildJsonObject { put("response", response) }))
            }

            post("/request") {
                val userId = call.userIdOrThrow()
                val request = call.receive<FriendshipRequestCreate>()

                FriendRequestDao.sendFriendRequest(userId, request.toId)
                call.respond(OkResponse(response = buildJsonObject {}))
            }

            post("/respond") {
                val userId = call.userIdOrThrow()
                val answer = call.receive<FriendshipRequestAnswer>()

                FriendRequestDao.respondToFriendRequest(userId, answer)
                val response = if (answer.accepted) "accepted" else "declined"
                call.respond(OkResponse(response = buildJsonObject { put("response", response) }))
            }

            get("/requests") {
                val userId = call.userIdOrThrow()

                val requests = FriendRequestDao.getIncomingRequests(userId)
                call.respond(
                    OkResponse(
                        response = buildJsonObject {
                            put(
                                "requests",
                                Json.encodeToJsonElement(
                                    ListSerializer(FriendRequestPreview.serializer()),
                                    requests
                                )
                            )
                        }
                    )
                )
            }

            get("") {
                val userId = call.userIdOrThrow()

                val friends = FriendRequestDao.getFriends(userId)

                call.respond(
                    OkResponse(
                        response = buildJsonObject {
                            put(
                                "friends",
                                Json.encodeToJsonElement(
                                    ListSerializer(UserShortDto.serializer()),
                                    friends.toSet().distinct()
                                )
                            )
                        }
                    )
                )
            }
        }
    }
}
