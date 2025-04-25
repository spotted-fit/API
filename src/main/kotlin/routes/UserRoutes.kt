package routes

import db.dao.UserDao
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.UserPreview

fun Route.userRoutes() {

    get("/search") {
        val q = call.request.queryParameters["q"]

        if (q.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Query parameter 'q' is missing")
            return@get
        }

        val results = UserDao.searchByUsername(q)

        val response = results.map {
            UserPreview(
                id = it.id,
                username = it.username,
                avatar = it.avatarPath
            )
        }
        call.respond(response)
    }
}
