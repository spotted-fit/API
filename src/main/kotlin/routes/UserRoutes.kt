package routes

import db.dao.UserDao
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import models.ErrorResponse
import models.OkResponse
import models.UserPreview

fun Route.userRoutes() {

    get("/search") {
        val q = call.request.queryParameters["q"]

        if (q.isNullOrBlank()) {
            call.respond(ErrorResponse(message = "Query parameter 'q' is missing"))
            return@get
        }

        val results = UserDao.searchByUsername(q)

        call.respond(
            OkResponse(
                response = Json.encodeToJsonElement(
                    ListSerializer(UserPreview.serializer()),
                    results.map {
                        UserPreview(
                            id = it.id,
                            username = it.username,
                            avatar = it.avatarPath
                        )
                    }
                )
            )
        )
    }
}
