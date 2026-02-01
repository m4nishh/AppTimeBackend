package com.apptime.code.referral

import kotlinx.serialization.Serializable

/**
 * Referral status enum
 */
@Serializable
enum class ReferralStatus {
    PENDING,    // Referred user has signed up but hasn't completed required action
    COMPLETED,  // Referred user completed required action (e.g., onboarding)
    REWARDED    // Both users have been rewarded
}

/**
 * User's referral code info
 */
@Serializable
data class UserReferralCode(
    val userId: String,
    val referralCode: String,
    val totalReferrals: Int = 0,
    val totalCoinsEarned: Long = 0L,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Referral record
 */
@Serializable
data class Referral(
    val id: Long,
    val referrerId: String,
    val referredUserId: String,
    val referralCode: String,
    val status: String, // ReferralStatus as string
    val referrerReward: Long = 0L,
    val referredReward: Long = 0L,
    val completedAt: String? = null,
    val rewardedAt: String? = null,
    val createdAt: String
)

/**
 * Request to apply a referral code during signup
 */
@Serializable
data class ApplyReferralCodeRequest(
    val referralCode: String
)

/**
 * Response after applying referral code
 */
@Serializable
data class ApplyReferralCodeResponse(
    val success: Boolean,
    val message: String,
    val referrerId: String? = null,
    val bonusCoins: Long = 0L // Bonus coins the new user will receive
)

/**
 * Response for getting user's referral info
 */
@Serializable
data class MyReferralInfoResponse(
    val userId: String,
    val referralCode: String,
    val totalReferrals: Int = 0,
    val totalCoinsEarned: Long = 0L,
    val pendingReferrals: Int = 0,
    val completedReferrals: Int = 0,
    val referrals: List<ReferralDetails> = emptyList()
)

/**
 * Details of a single referral
 */
@Serializable
data class ReferralDetails(
    val referredUserId: String,
    val referredUsername: String? = null, // Optional username for display
    val status: String,
    val coinsEarned: Long = 0L,
    val createdAt: String,
    val completedAt: String? = null,
    val rewardedAt: String? = null
)

/**
 * Request to complete a referral (mark as completed)
 */
@Serializable
data class CompleteReferralRequest(
    val referredUserId: String
)

/**
 * Response after completing a referral
 */
@Serializable
data class CompleteReferralResponse(
    val success: Boolean,
    val message: String,
    val referrerReward: Long = 0L,
    val referredReward: Long = 0L
)

/**
 * Referral statistics
 */
@Serializable
data class ReferralStats(
    val totalReferrals: Int = 0,
    val pendingReferrals: Int = 0,
    val completedReferrals: Int = 0,
    val totalCoinsEarned: Long = 0L,
    val referralCode: String
)

/**
 * Admin: All referrals response
 */
@Serializable
data class AllReferralsResponse(
    val referrals: List<ReferralAdminDetails>,
    val total: Int,
    val pending: Int,
    val completed: Int,
    val rewarded: Int
)

/**
 * Admin: Detailed referral info
 */
@Serializable
data class ReferralAdminDetails(
    val id: Long,
    val referrerId: String,
    val referrerUsername: String? = null,
    val referredUserId: String,
    val referredUsername: String? = null,
    val referralCode: String,
    val status: String,
    val referrerReward: Long = 0L,
    val referredReward: Long = 0L,
    val completedAt: String? = null,
    val rewardedAt: String? = null,
    val createdAt: String
)

/**
 * Leaderboard entry for top referrers
 */
@Serializable
data class ReferralLeaderboardEntry(
    val userId: String,
    val username: String? = null,
    val totalReferrals: Int = 0,
    val totalCoinsEarned: Long = 0L,
    val rank: Int
)

/**
 * Referral leaderboard response
 */
@Serializable
data class ReferralLeaderboardResponse(
    val leaderboard: List<ReferralLeaderboardEntry>,
    val myRank: Int? = null,
    val myStats: ReferralStats? = null
)

