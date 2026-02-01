package com.apptime.code.referral

import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.SecureRandom

/**
 * Repository for referral operations
 */
class ReferralRepository {
    
    private val random = SecureRandom()
    
    /**
     * Generate a unique referral code
     */
    private fun generateReferralCode(userId: String): String {
        // Create a code based on userId hash + random characters
        val prefix = userId.take(3).uppercase()
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Exclude confusing characters (I, O, 0, 1)
        val randomPart = (1..6).map { chars[random.nextInt(chars.length)] }.joinToString("")
        return "$prefix$randomPart"
    }
    
    /**
     * Ensure user has a referral code, create one if not exists
     */
    suspend fun ensureUserHasReferralCode(userId: String): String = transaction {
        // Check if user already has a referral code
        val existingCode = UserReferralCodes.select { UserReferralCodes.userId eq userId }
            .map { it[UserReferralCodes.referralCode] }
            .singleOrNull()
        
        if (existingCode != null) {
            return@transaction existingCode
        }
        
        // Generate unique code
        var code = generateReferralCode(userId)
        var attempts = 0
        while (isReferralCodeTaken(code) && attempts < 10) {
            code = generateReferralCode(userId)
            attempts++
        }
        
        if (attempts >= 10) {
            throw IllegalStateException("Failed to generate unique referral code")
        }
        
        // Insert new referral code
        UserReferralCodes.insert {
            it[UserReferralCodes.userId] = userId
            it[referralCode] = code
            it[totalReferrals] = 0
            it[totalCoinsEarned] = 0L
        }
        
        code
    }
    
    /**
     * Check if referral code is already taken
     */
    private fun isReferralCodeTaken(code: String): Boolean = transaction {
        UserReferralCodes.select { UserReferralCodes.referralCode eq code }
            .count() > 0
    }
    
    /**
     * Get user's referral code info
     */
    suspend fun getUserReferralCode(userId: String): UserReferralCode? = transaction {
        UserReferralCodes.select { UserReferralCodes.userId eq userId }
            .map { resultRow ->
                UserReferralCode(
                    userId = resultRow[UserReferralCodes.userId],
                    referralCode = resultRow[UserReferralCodes.referralCode],
                    totalReferrals = resultRow[UserReferralCodes.totalReferrals],
                    totalCoinsEarned = resultRow[UserReferralCodes.totalCoinsEarned],
                    createdAt = resultRow[UserReferralCodes.createdAt].toString(),
                    updatedAt = resultRow[UserReferralCodes.updatedAt].toString()
                )
            }
            .singleOrNull()
    }
    
    /**
     * Get user ID by referral code
     */
    suspend fun getUserIdByReferralCode(code: String): String? = transaction {
        UserReferralCodes.select { UserReferralCodes.referralCode eq code }
            .map { it[UserReferralCodes.userId] }
            .singleOrNull()
    }
    
    /**
     * Check if a user was already referred
     */
    suspend fun isUserAlreadyReferred(userId: String): Boolean = transaction {
        Referrals.select { Referrals.referredUserId eq userId }
            .count() > 0
    }
    
    /**
     * Create a referral record
     */
    suspend fun createReferral(
        referrerId: String,
        referredUserId: String,
        referralCode: String
    ): Long = transaction {
        Referrals.insert {
            it[Referrals.referrerId] = referrerId
            it[Referrals.referredUserId] = referredUserId
            it[Referrals.referralCode] = referralCode
            it[status] = ReferralStatus.PENDING.name
            it[referrerReward] = 0L
            it[referredReward] = 0L
        }[Referrals.id]
    }
    
    /**
     * Get referral by ID
     */
    suspend fun getReferralById(referralId: Long): Referral? = transaction {
        Referrals.select { Referrals.id eq referralId }
            .map { mapToReferral(it) }
            .singleOrNull()
    }
    
    /**
     * Get referral by referred user ID
     */
    suspend fun getReferralByReferredUserId(referredUserId: String): Referral? = transaction {
        Referrals.select { Referrals.referredUserId eq referredUserId }
            .map { mapToReferral(it) }
            .singleOrNull()
    }
    
    /**
     * Get all referrals made by a user
     */
    suspend fun getUserReferrals(
        userId: String,
        status: ReferralStatus? = null,
        limit: Int? = null,
        offset: Int = 0
    ): List<Referral> = transaction {
        var query = Referrals.select { Referrals.referrerId eq userId }
        
        if (status != null) {
            query = query.andWhere { Referrals.status eq status.name }
        }
        
        query = query.orderBy(Referrals.createdAt, SortOrder.DESC)
        
        if (limit != null) {
            query = query.limit(limit, offset.toLong())
        }
        
        query.map { mapToReferral(it) }
    }
    
    /**
     * Get referral count by status for a user
     */
    suspend fun getReferralCountByStatus(userId: String, status: ReferralStatus): Int = transaction {
        Referrals.select { 
            (Referrals.referrerId eq userId) and (Referrals.status eq status.name)
        }.count().toInt()
    }
    
    /**
     * Complete a referral (mark as completed)
     */
    suspend fun completeReferral(
        referralId: Long,
        referrerReward: Long,
        referredReward: Long
    ): Boolean = transaction {
        val updated = Referrals.update({ Referrals.id eq referralId }) {
            it[status] = ReferralStatus.COMPLETED.name
            it[Referrals.referrerReward] = referrerReward
            it[Referrals.referredReward] = referredReward
            it[completedAt] = kotlinx.datetime.Clock.System.now()
        }
        updated > 0
    }
    
    /**
     * Mark referral as rewarded
     */
    suspend fun markReferralAsRewarded(referralId: Long): Boolean = transaction {
        val updated = Referrals.update({ Referrals.id eq referralId }) {
            it[status] = ReferralStatus.REWARDED.name
            it[rewardedAt] = kotlinx.datetime.Clock.System.now()
        }
        updated > 0
    }
    
    /**
     * Update user's referral code stats (total referrals and coins earned)
     */
    suspend fun updateReferralCodeStats(userId: String, additionalCoins: Long): Boolean = transaction {
        val updated = UserReferralCodes.update({ UserReferralCodes.userId eq userId }) {
            it[totalReferrals] = totalReferrals + 1
            it[totalCoinsEarned] = totalCoinsEarned + additionalCoins
            it[updatedAt] = kotlinx.datetime.Clock.System.now()
        }
        updated > 0
    }
    
    /**
     * Get all referrals (admin)
     */
    suspend fun getAllReferrals(
        status: ReferralStatus? = null,
        limit: Int? = null,
        offset: Int = 0
    ): List<Referral> = transaction {
        val query = if (status != null) {
            Referrals.select { Referrals.status eq status.name }
        } else {
            Referrals.selectAll()
        }
        
        val orderedQuery = query.orderBy(Referrals.createdAt, SortOrder.DESC)
        
        val finalQuery = if (limit != null) {
            orderedQuery.limit(limit, offset.toLong())
        } else {
            orderedQuery
        }
        
        finalQuery.map { mapToReferral(it) }
    }
    
    /**
     * Get referral statistics by status
     */
    suspend fun getReferralStatsByStatus(): Map<String, Int> = transaction {
        val stats = mutableMapOf<String, Int>()
        
        ReferralStatus.values().forEach { status ->
            val count = Referrals.select { Referrals.status eq status.name }
                .count()
                .toInt()
            stats[status.name] = count
        }
        
        stats
    }
    
    /**
     * Get top referrers leaderboard
     */
    suspend fun getTopReferrers(limit: Int = 10): List<UserReferralCode> = transaction {
        UserReferralCodes.selectAll()
            .orderBy(UserReferralCodes.totalReferrals, SortOrder.DESC)
            .limit(limit)
            .map { resultRow ->
                UserReferralCode(
                    userId = resultRow[UserReferralCodes.userId],
                    referralCode = resultRow[UserReferralCodes.referralCode],
                    totalReferrals = resultRow[UserReferralCodes.totalReferrals],
                    totalCoinsEarned = resultRow[UserReferralCodes.totalCoinsEarned],
                    createdAt = resultRow[UserReferralCodes.createdAt].toString(),
                    updatedAt = resultRow[UserReferralCodes.updatedAt].toString()
                )
            }
    }
    
    /**
     * Get user's rank in referral leaderboard
     */
    suspend fun getUserReferralRank(userId: String): Int? = transaction {
        // Get all users ordered by total referrals
        val allUsers = UserReferralCodes.selectAll()
            .orderBy(UserReferralCodes.totalReferrals, SortOrder.DESC)
            .map { it[UserReferralCodes.userId] }
        
        val rank = allUsers.indexOf(userId)
        if (rank == -1) null else rank + 1
    }
    
    /**
     * Map ResultRow to Referral
     */
    private fun mapToReferral(row: ResultRow): Referral {
        return Referral(
            id = row[Referrals.id],
            referrerId = row[Referrals.referrerId],
            referredUserId = row[Referrals.referredUserId],
            referralCode = row[Referrals.referralCode],
            status = row[Referrals.status],
            referrerReward = row[Referrals.referrerReward],
            referredReward = row[Referrals.referredReward],
            completedAt = row[Referrals.completedAt]?.toString(),
            rewardedAt = row[Referrals.rewardedAt]?.toString(),
            createdAt = row[Referrals.createdAt].toString()
        )
    }
}

