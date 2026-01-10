package users

import kotlinx.serialization.Serializable

/**
 * Response model for listing who has access to user's profile
 */
@Serializable
data class TOTPAccessorInfo(
    val requestingUsername: String?,
    val verifiedAt: String, // ISO 8601 timestamp
    val expiresAt: String?, // ISO 8601 timestamp (null for whitelist - permanent access)
    val remainingSeconds: Int? = null, // null for whitelist (infinite access)
    val accessType: String // "TOTP" or "WHITELIST"
)

/**
 * Response model for control panel overview
 */
@Serializable
data class TOTPControlPanelResponse(
    val activeSessions: List<TOTPAccessorInfo>,
)

/**
 * Response model for whitelisted user info
 */
@Serializable
data class WhitelistedUserInfo(
    val userId: String,
    val username: String?,
    val whitelistedAt: String, // ISO 8601 timestamp
    val createdAt: String // ISO 8601 timestamp
)

/**
 * Request model for revoking access
 */
@Serializable
data class RevokeAccessRequest(
    val username: String // User whose access should be revoked
)

/**
 * Request model for adding user to whitelist
 */
@Serializable
data class AddWhitelistRequest(
    val username: String // Username of user to whitelist
)

/**
 * Request model for removing user from whitelist
 */
@Serializable
data class RemoveWhitelistRequest(
    val username: String // Username of user to remove from whitelist
)

/**
 * Request model for extending access time
 */
@Serializable
data class ExtendAccessRequest(
    val username: String, // User whose access should be extended
    val additionalSeconds: Int // Additional seconds to add to current expiration
)

/**
 * Request model for granting access without TOTP
 */
@Serializable
data class GrantAccessRequest(
    val username: String, // Username of user to grant access to
    val durationSeconds: Int? = null // Optional: Duration in seconds (default: 31536000 = 1 year, max: 31536000)
)

/**
 * Response model for extend access operation
 */
@Serializable
data class ExtendAccessResponse(
    val success: Boolean,
    val message: String,
    val expiresAt: String?, // ISO 8601 timestamp
    val remainingSeconds: Int?
)

