package fit.spotted.api.db.dao

import fit.spotted.api.db.DatabaseFactory.dbQuery
import fit.spotted.api.db.tables.*
import fit.spotted.api.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object ChallengeInviteDao {
    suspend fun create(challengeId: Int, invitedUserId: Int, invitedById: Int): Int? = dbQuery {
        // Check if user is already invited
        val existingInvite = ChallengeInviteTable.select {
            (ChallengeInviteTable.challengeId eq challengeId) and
                    (ChallengeInviteTable.invitedUserId eq invitedUserId)
        }.singleOrNull()

        if (existingInvite != null) {
            return@dbQuery null
        }

        // Check if user is already a participant
        val isAlreadyParticipant = ChallengeParticipantTable.select {
            (ChallengeParticipantTable.challengeId eq challengeId) and
                    (ChallengeParticipantTable.userId eq invitedUserId)
        }.singleOrNull() != null

        if (isAlreadyParticipant) {
            return@dbQuery null
        }

        ChallengeInviteTable.insert {
            it[ChallengeInviteTable.challengeId] = challengeId
            it[ChallengeInviteTable.invitedUserId] = invitedUserId
            it[ChallengeInviteTable.invitedById] = invitedById
            it[ChallengeInviteTable.invitedAt] = Instant.now().toEpochMilli()
        } get ChallengeInviteTable.id
    }

    suspend fun findAllByUser(userId: Int): List<ChallengeInvite> = coroutineScope {
        val inviteRows = dbQuery {
            ChallengeInviteTable.select { ChallengeInviteTable.invitedUserId eq userId }
                .map { row ->
                    Triple(
                        row[ChallengeInviteTable.id],
                        row[ChallengeInviteTable.challengeId],
                        row[ChallengeInviteTable.invitedById]
                    ) to row[ChallengeInviteTable.invitedAt]
                }
        }
        
        // Process invites in parallel
        val invites = inviteRows.mapNotNull { (info, invitedAt) ->
            val (id, challengeId, invitedById) = info
            
            async {
                val challenge = ChallengeDao.findById(challengeId) ?: return@async null
                val inviter = UserDao.findById(invitedById) ?: return@async null
                
                ChallengeInvite(
                    id = id,
                    challenge = challenge,
                    invitedBy = UserPreview(
                        id = inviter.id,
                        username = inviter.username,
                        avatar = inviter.avatarPath
                    ),
                    invitedAt = invitedAt
                )
            }
        }.map { it.await() }
        
        // Filter nulls to satisfy type system
        invites.filterNotNull()
    }

    suspend fun findById(id: Int): ChallengeInvite? = coroutineScope {
        val row = dbQuery {
            ChallengeInviteTable.select { ChallengeInviteTable.id eq id }
                .map { row ->
                    InviteInfo(
                        id = row[ChallengeInviteTable.id],
                        challengeId = row[ChallengeInviteTable.challengeId],
                        invitedById = row[ChallengeInviteTable.invitedById],
                        invitedAt = row[ChallengeInviteTable.invitedAt]
                    )
                }
                .singleOrNull()
        } ?: return@coroutineScope null
        
        val challengeDeferred = async { ChallengeDao.findById(row.challengeId) }
        val inviterDeferred = async { UserDao.findById(row.invitedById) }
        
        val challenge = challengeDeferred.await() ?: return@coroutineScope null
        val inviter = inviterDeferred.await() ?: return@coroutineScope null
        
        ChallengeInvite(
            id = row.id,
            challenge = challenge,
            invitedBy = UserPreview(
                id = inviter.id,
                username = inviter.username,
                avatar = inviter.avatarPath
            ),
            invitedAt = row.invitedAt
        )
    }

    suspend fun findByChallengeAndUser(challengeId: Int, userId: Int): ChallengeInvite? = coroutineScope {
        val row = dbQuery {
            ChallengeInviteTable.select {
                (ChallengeInviteTable.challengeId eq challengeId) and
                        (ChallengeInviteTable.invitedUserId eq userId)
            }.map {
                InviteInfo(
                    id = it[ChallengeInviteTable.id],
                    challengeId = it[ChallengeInviteTable.challengeId],
                    invitedById = it[ChallengeInviteTable.invitedById],
                    invitedAt = it[ChallengeInviteTable.invitedAt]
                )
            }.singleOrNull()
        } ?: return@coroutineScope null
        
        val challengeDeferred = async { ChallengeDao.findById(row.challengeId) }
        val inviterDeferred = async { UserDao.findById(row.invitedById) }
        
        val challenge = challengeDeferred.await() ?: return@coroutineScope null  
        val inviter = inviterDeferred.await() ?: return@coroutineScope null
        
        ChallengeInvite(
            id = row.id,
            challenge = challenge,
            invitedBy = UserPreview(
                id = inviter.id,
                username = inviter.username,
                avatar = inviter.avatarPath
            ),
            invitedAt = row.invitedAt
        )
    }

    suspend fun delete(id: Int): Boolean = dbQuery {
        ChallengeInviteTable.deleteWhere { ChallengeInviteTable.id eq id } > 0
    }
    
    private data class InviteInfo(
        val id: Int,
        val challengeId: Int,
        val invitedById: Int,
        val invitedAt: Long
    )
} 