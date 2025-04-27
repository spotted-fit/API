package routes

import db.dao.FriendRequestDao
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import models.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import utils.userIdOrThrow

fun Route.friendshipRoutes() {
    authenticate {
        route("/friends") {

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

            get("/") {
                val userId = call.userIdOrThrow()

                val friends = FriendRequestDao.getFriends(userId)

                call.respond(
                    OkResponse(
                        response = buildJsonObject {
                            put(
                                "friends",
                                Json.encodeToJsonElement(
                                    ListSerializer(UserShortDto.serializer()),
                                    friends
                                )
                            )
                        }
                    )
                )
            }
        }
    }
}