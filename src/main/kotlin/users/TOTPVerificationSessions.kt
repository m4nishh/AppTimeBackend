package users

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * TOTP Verification Sessions table
 * Tracks which user (requestingUserId) has verified access to another user's (targetUserId) data
 * Only one active session per requestingUserId -> targetUserId pair at a time
 */
object TOTPVerificationSessions : Table("totp_verification_sessions") {
    val id = long("id").autoIncrement()
    val requestingUserId = varchar("requesting_user_id", 255) // User A (who wants to see data)
    val targetUserId = varchar("target_user_id", 255) // User B (whose data is being accessed)
    val targetUsername = varchar("target_username", 255).nullable() // Username of target user
    val verifiedAt = timestamp("verified_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    val expiresAt = timestamp("expires_at") // Session expiration time (e.g., 1 hour from verification)
    val createdAt = timestamp("created_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        // Index for checking active sessions
        index(isUnique = false, requestingUserId, targetUserId)
        index(isUnique = false, expiresAt)
        // Unique constraint: only one active session per requestingUserId -> targetUserId pair
        // This is enforced at application level by invalidating old sessions
    }
}

