package routes

import db.dao.UserDao
import db.dao.PostDao
import db.tables.FriendRequestTable
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import models.ProfilePost
import models.ProfileResponse
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.profileRoutes() {
    get("/profile/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "Invalid id")
            return@get
        }

        val user = UserDao.findById(id)
        if (user == null) {
            call.respond(HttpStatusCode.NotFound, "User not found")
            return@get
        }

        val posts = PostDao.findByUserId(user.id).map {
            ProfilePost(
                id = it.id,
                photo1 = it.photo1,
                photo2 = it.photo2,
                emoji = it.emoji,
                text = it.text,
                createdAt = it.createdAt
            )
        }

        val friendsCount = transaction {
            FriendRequestTable
                .select {
                    (FriendRequestTable.fromUser eq id and (FriendRequestTable.status eq "accepted")) or
                            (FriendRequestTable.toUser eq id and (FriendRequestTable.status eq "accepted"))
                }
                .count()
        }

        call.respond(
            ProfileResponse(
                id = user.id,
                avatar = user.avatarPath,
                friendsCount = friendsCount.toInt(),
                posts = posts
            )
        )
    }
}
