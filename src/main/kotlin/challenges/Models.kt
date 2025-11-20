package com.apptime.code.challenges

import kotlinx.serialization.Serializable

/**
 * Challenge type enum
 */
enum class ChallengeType {
    LESS_SCREENTIME,  // Lower screen time is better (ranked ascending)
    MORE_SCREENTIME   // Higher screen time is better (ranked descending)
}

/**
 * Challenge data model
 */
@Serializable
data class Challenge(
    val id: Long,
    val title: String,
    val description: String? = null,
    val reward: String? = null,
    val startTime: String, // ISO 8601 format
    val endTime: String,   // ISO 8601 format
    val thumbnail: String? = null,
    val challengeType: String, // "LESS_SCREENTIME" or "MORE_SCREENTIME"
    val isActive: Boolean = true,
    val createdAt: String? = null
)

/**
 * Active challenge response (simplified for listing)
 */
@Serializable
data class ActiveChallenge(
    val id: Long,
    val title: String,
    val description: String? = null,
    val reward: String? = null,
    val startTime: String,
    val endTime: String,
    val thumbnail: String? = null,
    val hasJoined: Boolean = false
)

/**
 * Response for getting all active challenges
 */
@Serializable
data class ActiveChallengesResponse(
    val challenges: List<ActiveChallenge>
)

/**
 * Request to join/register for a challenge
 */
@Serializable
data class JoinChallengeRequest(
    val challengeId: Long
)

/**
 * Response for joining a challenge
 */
@Serializable
data class JoinChallengeResponse(
    val challengeId: Long,
    val userId: String,
    val joinedAt: String,
    val message: String
)

/**
 * User challenge (includes past challenges)
 */
@Serializable
data class UserChallenge(
    val id: Long,
    val title: String,
    val description: String? = null,
    val reward: String? = null,
    val startTime: String,
    val endTime: String,
    val thumbnail: String? = null,
    val challengeType: String,
    val isActive: Boolean,
    val joinedAt: String,
    val isPast: Boolean // true if challenge has ended
)

/**
 * Response for getting all user challenges
 */
@Serializable
data class UserChallengesResponse(
    val challenges: List<UserChallenge>
)

/**
 * Challenge detail with participant count
 */
@Serializable
data class ChallengeDetail(
    val id: Long,
    val title: String,
    val description: String? = null,
    val reward: String? = null,
    val startTime: String,
    val endTime: String,
    val thumbnail: String? = null,
    val challengeType: String,
    val isActive: Boolean,
    val participantCount: Int,
    val createdAt: String? = null
)

/**
 * Request to submit challenge participant stats
 */
@Serializable
data class ChallengeStatsSubmissionRequest(
    val challengeId: Long,
    val appName: String,
    val packageName: String,
    val startSyncTime: String, // ISO 8601 format
    val endSyncTime: String,   // ISO 8601 format
    val duration: Long // milliseconds
)

/**
 * Batch request to submit multiple challenge stats
 */
@Serializable
data class BatchChallengeStatsSubmissionRequest(
    val challengeId: Long,
    val stats: List<ChallengeStatsSubmissionRequest>
)

/**
 * Response for submitting challenge stats
 */
@Serializable
data class ChallengeStatsSubmissionResponse(
    val submitted: Int,
    val totalDuration: Long
)

/**
 * Challenge participant rank entry
 */
@Serializable
data class ChallengeRankEntry(
    val rank: Int,
    val userId: String,
    val totalDuration: Long, // milliseconds
    val appCount: Int // number of different apps tracked
)

/**
 * Response for challenge ranking
 */
@Serializable
data class ChallengeRankResponse(
    val challengeId: Long,
    val challengeTitle: String,
    val challengeType: String,
    val rankings: List<ChallengeRankEntry>,
    val userRank: ChallengeRankEntry? = null, // Current user's rank if they're participating
    val totalParticipants: Int
)

