package com.apptime.code.clans

import kotlinx.serialization.Serializable

/**
 * App category enum
 */
@Serializable
enum class AppCategory {
    PRODUCTIVE,   // EdTech/Learning apps (Coursera, Udemy, Khan Academy)
    DISTRACTIVE   // Social Media/Entertainment (Instagram, TikTok, etc.)
}

/**
 * User app category model
 */
@Serializable
data class UserAppCategory(
    val id: Long,
    val userId: String,
    val packageName: String,
    val appName: String? = null,
    val category: String, // "PRODUCTIVE" or "DISTRACTIVE"
    val createdAt: String, // ISO 8601
    val updatedAt: String // ISO 8601
)

/**
 * Request to set app category
 */
@Serializable
data class SetAppCategoryRequest(
    val packageName: String,
    val appName: String? = null,
    val category: String // "PRODUCTIVE" or "DISTRACTIVE"
)

/**
 * App category usage sync request (from Android UsageStatsManager)
 */
@Serializable
data class AppCategoryUsageSyncRequest(
    val date: String, // YYYY-MM-DD format
    val productiveTime: Long, // Total productive time in milliseconds
    val distractiveTime: Long, // Total distractive time in milliseconds
    val packageNames: String? = null // Optional: comma-separated package names (for privacy, can be null)
)

/**
 * App category usage response
 */
@Serializable
data class AppCategoryUsageResponse(
    val userId: String,
    val date: String,
    val productiveTime: Long,
    val distractiveTime: Long,
    val lastSyncedAt: String? // ISO 8601
)

/**
 * Clan vault model
 */
@Serializable
data class ClanVaultInfo(
    val clanId: Long,
    val totalCoins: Long,
    val lockedCoins: Long,
    val availableCoins: Long,
    val lastUpdatedAt: String // ISO 8601
)

/**
 * Clan challenge type enum
 */
@Serializable
enum class ClanChallengeType {
    NEGATIVE_STAKE,  // "Scroll-Less Sprint" - penalty for exceeding limit
    POSITIVE_STAKE   // "Knowledge Surge" - bonus for meeting goal
}

/**
 * Clan challenge model
 */
@Serializable
data class ClanChallenge(
    val id: Long,
    val clanId: Long,
    val title: String,
    val description: String? = null,
    val challengeType: String, // "NEGATIVE_STAKE" or "POSITIVE_STAKE"
    val category: String, // "DISTRACTIVE" or "PRODUCTIVE"
    val packageNames: String? = null,
    val timeLimit: Long? = null, // For negative: max time in ms
    val timeGoal: Long? = null, // For positive: goal time in ms
    val coinMultiplier: Double = 2.0,
    val buyInAmount: Long,
    val jackpotPool: Long,
    val startTime: String, // ISO 8601
    val endTime: String, // ISO 8601
    val status: String, // ACTIVE, COMPLETED, CANCELLED
    val participantCount: Int = 0,
    val hasJoined: Boolean = false,
    val createdAt: String, // ISO 8601
    val updatedAt: String // ISO 8601
)

/**
 * Request to create a clan challenge
 */
@Serializable
data class CreateClanChallengeRequest(
    val clanId: Long,
    val title: String,
    val description: String? = null,
    val challengeType: String, // "NEGATIVE_STAKE" or "POSITIVE_STAKE"
    val category: String, // "DISTRACTIVE" or "PRODUCTIVE"
    val packageNames: String? = null,
    val timeLimit: Long? = null, // For negative: max time in ms (e.g., 30 min = 1800000)
    val timeGoal: Long? = null, // For positive: goal time in ms
    val coinMultiplier: Double = 2.0,
    val buyInAmount: Long,
    val startTime: String, // ISO 8601
    val endTime: String // ISO 8601
)

/**
 * Request to join a clan challenge
 */
@Serializable
data class JoinClanChallengeRequest(
    val challengeId: Long
)

/**
 * Clan challenge participant model
 */
@Serializable
data class ClanChallengeParticipant(
    val challengeId: Long,
    val userId: String,
    val username: String? = null,
    val buyInPaid: Long,
    val totalTime: Long, // Total time in milliseconds
    val isSuccessful: Boolean,
    val coinsEarned: Long,
    val coinsLeaked: Long,
    val joinedAt: String, // ISO 8601
    val lastSyncedAt: String? = null // ISO 8601
)

/**
 * Clan challenge leaderboard entry
 */
@Serializable
data class ClanChallengeLeaderboardEntry(
    val rank: Int,
    val userId: String,
    val username: String? = null,
    val totalTime: Long,
    val isSuccessful: Boolean,
    val coinsEarned: Long,
    val coinsLeaked: Long
)

/**
 * Clan challenge leaderboard response
 */
@Serializable
data class ClanChallengeLeaderboardResponse(
    val challengeId: Long,
    val challengeTitle: String,
    val challengeType: String,
    val jackpotPool: Long,
    val entries: List<ClanChallengeLeaderboardEntry>,
    val userEntry: ClanChallengeLeaderboardEntry? = null,
    val totalParticipants: Int
)

/**
 * Clan war model
 */
@Serializable
data class ClanWar(
    val id: Long,
    val clan1Id: Long,
    val clan1Name: String? = null,
    val clan2Id: Long,
    val clan2Name: String? = null,
    val season: String,
    val startTime: String, // ISO 8601
    val endTime: String, // ISO 8601
    val clan1ProductiveTime: Long,
    val clan1DistractiveTime: Long,
    val clan2ProductiveTime: Long,
    val clan2DistractiveTime: Long,
    val clan1Ratio: Double, // Learning Time / Distraction Time
    val clan2Ratio: Double,
    val winnerClanId: Long? = null,
    val status: String, // ACTIVE, COMPLETED, CANCELLED
    val createdAt: String, // ISO 8601
    val updatedAt: String // ISO 8601
)

/**
 * Request to create a clan war
 */
@Serializable
data class CreateClanWarRequest(
    val clan1Id: Long,
    val clan2Id: Long,
    val season: String,
    val startTime: String, // ISO 8601
    val endTime: String // ISO 8601
)

/**
 * Clan education leaderboard entry
 */
@Serializable
data class ClanEducationLeaderboardEntry(
    val rank: Int,
    val userId: String,
    val username: String? = null,
    val productiveTime: Long, // Total productive time in milliseconds
    val dailyAverage: Long? = null // Average per day in milliseconds
)

/**
 * Clan education leaderboard response
 */
@Serializable
data class ClanEducationLeaderboardResponse(
    val clanId: Long,
    val period: String, // "daily", "weekly", "monthly"
    val periodDate: String,
    val entries: List<ClanEducationLeaderboardEntry>,
    val userEntry: ClanEducationLeaderboardEntry? = null,
    val totalParticipants: Int
)

