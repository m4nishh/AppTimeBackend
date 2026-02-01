package com.apptime.code.referral

import com.apptime.code.rewards.AddCoinsRequest
import com.apptime.code.rewards.CoinSource
import com.apptime.code.rewards.RewardRepository
import com.apptime.code.notifications.NotificationQueueService
import users.UserRepository

/**
 * Referral service - handles business logic for referrals
 */
class ReferralService(
    private val referralRepository: ReferralRepository,
    private val rewardRepository: RewardRepository,
    private val userRepository: UserRepository
) {
    
    companion object {
        // Reward configuration
        const val REFERRER_REWARD_COINS = 500L // Coins given to the person who referred
        const val REFERRED_REWARD_COINS = 200L // Coins given to the new user who was referred
    }
    
    /**
     * Get or create a referral code for a user
     */
    suspend fun getOrCreateReferralCode(userId: String): UserReferralCode {
        val existingCode = referralRepository.getUserReferralCode(userId)
        if (existingCode != null) {
            return existingCode
        }
        
        // Create new referral code
        val code = referralRepository.ensureUserHasReferralCode(userId)
        return referralRepository.getUserReferralCode(userId)!!
    }
    
    /**
     * Apply a referral code during signup
     */
    suspend fun applyReferralCode(
        newUserId: String,
        referralCode: String
    ): ApplyReferralCodeResponse {
        // Normalize code (uppercase, trim)
        val normalizedCode = referralCode.trim().uppercase()
        
        // Check if user was already referred
        if (referralRepository.isUserAlreadyReferred(newUserId)) {
            throw IllegalArgumentException("You have already used a referral code. Each user can only be referred once.")
        }
        
        // Get referrer user ID from code
        val referrerId = referralRepository.getUserIdByReferralCode(normalizedCode)
            ?: throw IllegalArgumentException("Invalid referral code. Please check the code and try again.")
        
        // Check if user is trying to use their own code
        if (referrerId == newUserId) {
            throw IllegalArgumentException("You cannot use your own referral code.")
        }
        
        // Create referral record
        val referralId = referralRepository.createReferral(
            referrerId = referrerId,
            referredUserId = newUserId,
            referralCode = normalizedCode
        )
        
        // Auto-complete the referral immediately (or you can do this later based on some action)
        // For now, we'll mark it as pending and the app can call completeReferral later
        
        return ApplyReferralCodeResponse(
            success = true,
            message = "Referral code applied successfully! You'll receive ${REFERRED_REWARD_COINS} coins.",
            referrerId = referrerId,
            bonusCoins = REFERRED_REWARD_COINS
        )
    }
    
    /**
     * Complete a referral and award coins to both users
     * This should be called when the referred user completes a required action (e.g., onboarding, first challenge)
     */
    suspend fun completeReferral(referredUserId: String): CompleteReferralResponse {
        // Get referral record
        val referral = referralRepository.getReferralByReferredUserId(referredUserId)
            ?: throw IllegalArgumentException("No referral found for this user.")
        
        if (referral.status != ReferralStatus.PENDING.name) {
            throw IllegalArgumentException("Referral has already been completed or rewarded.")
        }
        
        // Award coins to referrer
        val referrerCoins = rewardRepository.addCoins(
            userId = referral.referrerId,
            amount = REFERRER_REWARD_COINS,
            source = CoinSource.REFERRAL,
            description = "Referral reward: Successfully referred a new user",
            metadata = "{\"referredUserId\": \"$referredUserId\", \"referralId\": ${referral.id}}"
        )
        
        // Award coins to referred user
        val referredCoins = rewardRepository.addCoins(
            userId = referredUserId,
            amount = REFERRED_REWARD_COINS,
            source = CoinSource.REFERRAL,
            description = "Welcome bonus: Joined using a referral code",
            metadata = "{\"referrerId\": \"${referral.referrerId}\", \"referralId\": ${referral.id}}"
        )
        
        // Mark referral as completed
        referralRepository.completeReferral(
            referralId = referral.id,
            referrerReward = REFERRER_REWARD_COINS,
            referredReward = REFERRED_REWARD_COINS
        )
        
        // Mark as rewarded
        referralRepository.markReferralAsRewarded(referral.id)
        
        // Update referrer's stats
        referralRepository.updateReferralCodeStats(referral.referrerId, REFERRER_REWARD_COINS)
        
        // Send notifications
        try {
            // Notify referrer
            val referredUsername = userRepository.getUserById(referredUserId)?.username ?: "Someone"
            NotificationQueueService.enqueueReferralSuccessNotification(
                userId = referral.referrerId,
                referredUsername = referredUsername,
                coinsEarned = REFERRER_REWARD_COINS
            )
            
            // Notify referred user
            NotificationQueueService.enqueueWelcomeBonusNotification(
                userId = referredUserId,
                coinsEarned = REFERRED_REWARD_COINS
            )
        } catch (e: Exception) {
            println("❌ Failed to send referral notifications: ${e.message}")
            // Don't throw - coins were already awarded
        }
        
        return CompleteReferralResponse(
            success = true,
            message = "Referral completed successfully!",
            referrerReward = REFERRER_REWARD_COINS,
            referredReward = REFERRED_REWARD_COINS
        )
    }
    
    /**
     * Get user's referral info and statistics
     */
    suspend fun getMyReferralInfo(userId: String): MyReferralInfoResponse {
        // Ensure user has a referral code
        val codeInfo = getOrCreateReferralCode(userId)
        
        // Get all referrals
        val allReferrals = referralRepository.getUserReferrals(userId)
        
        // Count by status
        val pendingCount = referralRepository.getReferralCountByStatus(userId, ReferralStatus.PENDING)
        val completedCount = referralRepository.getReferralCountByStatus(userId, ReferralStatus.COMPLETED) +
                            referralRepository.getReferralCountByStatus(userId, ReferralStatus.REWARDED)
        
        // Get details for each referral with usernames
        val referralDetails = allReferrals.map { ref ->
            val username = try {
                userRepository.getUserById(ref.referredUserId)?.username
            } catch (e: Exception) {
                null
            }
            
            ReferralDetails(
                referredUserId = ref.referredUserId,
                referredUsername = username,
                status = ref.status,
                coinsEarned = ref.referrerReward,
                createdAt = ref.createdAt,
                completedAt = ref.completedAt,
                rewardedAt = ref.rewardedAt
            )
        }
        
        return MyReferralInfoResponse(
            userId = userId,
            referralCode = codeInfo.referralCode,
            totalReferrals = codeInfo.totalReferrals,
            totalCoinsEarned = codeInfo.totalCoinsEarned,
            pendingReferrals = pendingCount,
            completedReferrals = completedCount,
            referrals = referralDetails
        )
    }
    
    /**
     * Get referral leaderboard
     */
    suspend fun getReferralLeaderboard(
        userId: String?,
        limit: Int = 20
    ): ReferralLeaderboardResponse {
        val topReferrers = referralRepository.getTopReferrers(limit)
        
        val leaderboard = topReferrers.mapIndexed { index, codeInfo ->
            val username = try {
                userRepository.getUserById(codeInfo.userId)?.username
            } catch (e: Exception) {
                null
            }
            
            ReferralLeaderboardEntry(
                userId = codeInfo.userId,
                username = username,
                totalReferrals = codeInfo.totalReferrals,
                totalCoinsEarned = codeInfo.totalCoinsEarned,
                rank = index + 1
            )
        }
        
        // Get user's rank and stats if userId provided
        var myRank: Int? = null
        var myStats: ReferralStats? = null
        
        if (userId != null) {
            myRank = referralRepository.getUserReferralRank(userId)
            val codeInfo = referralRepository.getUserReferralCode(userId)
            if (codeInfo != null) {
                val pendingCount = referralRepository.getReferralCountByStatus(userId, ReferralStatus.PENDING)
                val completedCount = referralRepository.getReferralCountByStatus(userId, ReferralStatus.COMPLETED) +
                                    referralRepository.getReferralCountByStatus(userId, ReferralStatus.REWARDED)
                
                myStats = ReferralStats(
                    totalReferrals = codeInfo.totalReferrals,
                    pendingReferrals = pendingCount,
                    completedReferrals = completedCount,
                    totalCoinsEarned = codeInfo.totalCoinsEarned,
                    referralCode = codeInfo.referralCode
                )
            }
        }
        
        return ReferralLeaderboardResponse(
            leaderboard = leaderboard,
            myRank = myRank,
            myStats = myStats
        )
    }
    
    /**
     * Get all referrals (admin)
     */
    suspend fun getAllReferrals(
        status: ReferralStatus? = null,
        limit: Int? = null,
        offset: Int = 0
    ): AllReferralsResponse {
        val referrals = referralRepository.getAllReferrals(status, limit, offset)
        val stats = referralRepository.getReferralStatsByStatus()
        
        val referralDetails = referrals.map { ref ->
            val referrerUsername = try {
                userRepository.getUserById(ref.referrerId)?.username
            } catch (e: Exception) {
                null
            }
            
            val referredUsername = try {
                userRepository.getUserById(ref.referredUserId)?.username
            } catch (e: Exception) {
                null
            }
            
            ReferralAdminDetails(
                id = ref.id,
                referrerId = ref.referrerId,
                referrerUsername = referrerUsername,
                referredUserId = ref.referredUserId,
                referredUsername = referredUsername,
                referralCode = ref.referralCode,
                status = ref.status,
                referrerReward = ref.referrerReward,
                referredReward = ref.referredReward,
                completedAt = ref.completedAt,
                rewardedAt = ref.rewardedAt,
                createdAt = ref.createdAt
            )
        }
        
        return AllReferralsResponse(
            referrals = referralDetails,
            total = stats.values.sum(),
            pending = stats[ReferralStatus.PENDING.name] ?: 0,
            completed = stats[ReferralStatus.COMPLETED.name] ?: 0,
            rewarded = stats[ReferralStatus.REWARDED.name] ?: 0
        )
    }
    
    /**
     * Manually complete a referral (admin)
     */
    suspend fun adminCompleteReferral(referralId: Long): CompleteReferralResponse {
        val referral = referralRepository.getReferralById(referralId)
            ?: throw IllegalArgumentException("Referral not found")
        
        return completeReferral(referral.referredUserId)
    }
}

