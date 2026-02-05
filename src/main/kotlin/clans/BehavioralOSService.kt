package com.apptime.code.clans

import com.apptime.code.rewards.CoinSource
import com.apptime.code.rewards.RewardRepository
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import java.time.format.DateTimeFormatter

class BehavioralOSService {
    private val repository = BehavioralOSRepository()
    private val rewardRepository = RewardRepository()
    private val clanRepository = ClanRepository()
    
    // ========== APP CATEGORY METHODS ==========
    
    /**
     * Set app category for a user
     */
    fun setAppCategory(userId: String, request: SetAppCategoryRequest): UserAppCategory {
        if (request.category !in listOf("PRODUCTIVE", "DISTRACTIVE")) {
            throw IllegalArgumentException("Category must be PRODUCTIVE or DISTRACTIVE")
        }
        
        return repository.setAppCategory(userId, request.packageName, request.appName, request.category)
    }
    
    /**
     * Get user's app categories
     */
    fun getUserAppCategories(userId: String): List<UserAppCategory> {
        return repository.getUserAppCategories(userId)
    }
    
    /**
     * Sync app category usage from Android UsageStatsManager
     */
    fun syncAppCategoryUsage(userId: String, request: AppCategoryUsageSyncRequest): AppCategoryUsageResponse {
        repository.syncAppCategoryUsage(
            userId = userId,
            date = request.date,
            productiveTime = request.productiveTime,
            distractiveTime = request.distractiveTime,
            packageNames = request.packageNames
        )
        
        // Update clan education leaderboard if user is in a clan
        updateClanEducationLeaderboards(userId, request.date, request.productiveTime)
        
        return repository.getAppCategoryUsage(userId, request.date)
            ?: AppCategoryUsageResponse(
                userId = userId,
                date = request.date,
                productiveTime = request.productiveTime,
                distractiveTime = request.distractiveTime,
                lastSyncedAt = null
            )
    }
    
    /**
     * Get app category usage for a user
     */
    fun getAppCategoryUsage(userId: String, date: String): AppCategoryUsageResponse? {
        return repository.getAppCategoryUsage(userId, date)
    }
    
    /**
     * Update clan education leaderboards when user syncs productive time
     */
    private fun updateClanEducationLeaderboards(userId: String, date: String, productiveTime: Long) {
        val userClans = clanRepository.getUserClans(userId)
        
        for ((clan, member) in userClans) {
            val clanId = member.clanId
            val dateObj = java.time.LocalDate.parse(date)
            
            // Update daily leaderboard
            val dailyDate = date
            repository.updateClanEducationLeaderboard(
                clanId = clanId,
                userId = userId,
                period = "daily",
                periodDate = dailyDate,
                productiveTime = productiveTime
            )
            
            // Update weekly leaderboard
            val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())
            val week = dateObj.get(weekFields.weekOfWeekBasedYear())
            val year = dateObj.get(weekFields.weekBasedYear())
            val weekDate = "${year}-W${String.format("%02d", week)}"
            
            // Sum all daily productive time for this week
            val weekStart = dateObj.with(weekFields.dayOfWeek(), 1)
            val weekEnd = weekStart.plusDays(6)
            var currentDate = weekStart
            var weeklyTotal = 0L
            
            while (!currentDate.isAfter(weekEnd)) {
                val dateStr = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val usage = repository.getAppCategoryUsage(userId, dateStr)
                weeklyTotal += usage?.productiveTime ?: 0L
                currentDate = currentDate.plusDays(1)
            }
            
            repository.updateClanEducationLeaderboard(
                clanId = clanId,
                userId = userId,
                period = "weekly",
                periodDate = weekDate,
                productiveTime = weeklyTotal
            )
            
            // Update monthly leaderboard
            val monthDate = dateObj.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val monthStart = java.time.LocalDate.of(dateObj.year, dateObj.month, 1)
            val monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth())
            currentDate = monthStart
            var monthlyTotal = 0L
            
            while (!currentDate.isAfter(monthEnd)) {
                val dateStr = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val usage = repository.getAppCategoryUsage(userId, dateStr)
                monthlyTotal += usage?.productiveTime ?: 0L
                currentDate = currentDate.plusDays(1)
            }
            
            repository.updateClanEducationLeaderboard(
                clanId = clanId,
                userId = userId,
                period = "monthly",
                periodDate = monthDate,
                productiveTime = monthlyTotal
            )
        }
    }
    
    // ========== CLAN VAULT METHODS ==========
    
    /**
     * Get clan vault info
     */
    fun getClanVault(clanId: Long): ClanVaultInfo {
        // Verify user is member of clan
        return repository.getOrCreateClanVault(clanId)
    }
    
    // ========== CLAN CHALLENGE METHODS ==========
    
    /**
     * Create a clan challenge
     */
    fun createClanChallenge(userId: String, request: CreateClanChallengeRequest): ClanChallenge {
        // Verify user is admin/moderator of clan
        val membership = clanRepository.getUserClanMembership(userId, request.clanId)
            ?: throw IllegalStateException("Not a member of this clan")
        
        if (membership.role !in listOf("ADMIN", "MODERATOR")) {
            throw IllegalStateException("Only admins and moderators can create challenges")
        }
        
        if (request.challengeType !in listOf("NEGATIVE_STAKE", "POSITIVE_STAKE")) {
            throw IllegalArgumentException("Challenge type must be NEGATIVE_STAKE or POSITIVE_STAKE")
        }
        
        if (request.challengeType == "NEGATIVE_STAKE" && request.timeLimit == null) {
            throw IllegalArgumentException("Negative stake challenges require timeLimit")
        }
        
        if (request.challengeType == "POSITIVE_STAKE" && request.timeGoal == null) {
            throw IllegalArgumentException("Positive stake challenges require timeGoal")
        }
        
        val startTime = Instant.parse(request.startTime)
        val endTime = Instant.parse(request.endTime)
        
        if (endTime <= startTime) {
            throw IllegalArgumentException("End time must be after start time")
        }
        
        val challengeId = repository.createClanChallenge(
            clanId = request.clanId,
            title = request.title,
            description = request.description,
            challengeType = request.challengeType,
            category = request.category,
            packageNames = request.packageNames,
            timeLimit = request.timeLimit,
            timeGoal = request.timeGoal,
            coinMultiplier = request.coinMultiplier,
            buyInAmount = request.buyInAmount,
            startTime = startTime,
            endTime = endTime
        )
        
        return repository.getClanChallenge(challengeId, userId)!!
    }
    
    /**
     * Get clan challenge by ID
     */
    fun getClanChallenge(challengeId: Long, userId: String?): ClanChallenge {
        return repository.getClanChallenge(challengeId, userId)
            ?: throw IllegalStateException("Challenge not found")
    }
    
    /**
     * Join a clan challenge (pay buy-in)
     */
    fun joinClanChallenge(userId: String, request: JoinClanChallengeRequest): ClanChallengeParticipant {
        val challenge = repository.getClanChallenge(request.challengeId, userId)
            ?: throw IllegalStateException("Challenge not found")
        
        // Verify user is member of clan
        val membership = clanRepository.getUserClanMembership(userId, challenge.clanId)
            ?: throw IllegalStateException("Not a member of this clan")
        
        // Check if user has enough coins
        val totalCoins = rewardRepository.getTotalCoins(userId)
        if (totalCoins < challenge.buyInAmount) {
            throw IllegalStateException("Insufficient coins. You need ${challenge.buyInAmount} coins but only have $totalCoins")
        }
        
        // Deduct buy-in from user
        rewardRepository.addCoins(
            userId = userId,
            amount = -challenge.buyInAmount,
            source = CoinSource.OTHER,
            description = "Buy-in for clan challenge: ${challenge.title}",
            metadata = "{\"challengeId\": ${challenge.id}, \"type\": \"clan_challenge_buyin\"}"
        )
        
        // Join challenge
        return repository.joinClanChallenge(request.challengeId, userId, challenge.buyInAmount)
    }
    
    /**
     * Sync challenge participant stats (from app)
     */
    fun syncChallengeStats(challengeId: Long, userId: String, totalTime: Long) {
        repository.updateChallengeParticipantStats(challengeId, userId, totalTime)
        
        // Check for coin leakage (negative stake)
        val challenge = repository.getClanChallenge(challengeId, userId)
            ?: throw IllegalStateException("Challenge not found")
        
        if (challenge.challengeType == "NEGATIVE_STAKE" && challenge.timeLimit != null) {
            if (totalTime > challenge.timeLimit) {
                // Calculate leakage (e.g., 10 coins per minute over limit)
                val overLimit = totalTime - challenge.timeLimit
                val minutesOver = overLimit / (60 * 1000)
                val leakedAmount = minutesOver * 10 // 10 coins per minute over
                
                if (leakedAmount > 0) {
                    // Deduct from user
                    rewardRepository.addCoins(
                        userId = userId,
                        amount = -leakedAmount,
                        source = CoinSource.OTHER,
                        description = "Coin leakage for exceeding limit in challenge: ${challenge.title}",
                        metadata = "{\"challengeId\": ${challenge.id}, \"type\": \"coin_leakage\"}"
                    )
                    
                    // Add to clan vault
                    repository.processCoinLeakage(challengeId, userId, leakedAmount)
                }
            }
        }
        
        // Check for bonus coins (positive stake)
        if (challenge.challengeType == "POSITIVE_STAKE" && challenge.timeGoal != null) {
            if (totalTime >= challenge.timeGoal) {
                // Award bonus coins (2x multiplier)
                val baseCoins = totalTime / (60 * 1000) // 1 coin per minute
                val bonusCoins = (baseCoins * challenge.coinMultiplier).toLong()
                
                rewardRepository.addCoins(
                    userId = userId,
                    amount = bonusCoins,
                    source = CoinSource.OTHER,
                    description = "Bonus coins for meeting goal in challenge: ${challenge.title}",
                    metadata = "{\"challengeId\": ${challenge.id}, \"type\": \"positive_stake_bonus\", \"multiplier\": ${challenge.coinMultiplier}}"
                )
            }
        }
    }
    
    /**
     * Get clan challenge leaderboard
     */
    fun getClanChallengeLeaderboard(challengeId: Long, userId: String?): ClanChallengeLeaderboardResponse {
        return repository.getClanChallengeLeaderboard(challengeId, userId)
    }
    
    /**
     * Complete challenge and distribute jackpot
     */
    fun completeClanChallenge(challengeId: Long, userId: String) {
        val challenge = repository.getClanChallenge(challengeId, userId)
            ?: throw IllegalStateException("Challenge not found")
        
        // Verify user is admin/moderator
        val membership = clanRepository.getUserClanMembership(userId, challenge.clanId)
            ?: throw IllegalStateException("Not a member of this clan")
        
        if (membership.role !in listOf("ADMIN", "MODERATOR")) {
            throw IllegalStateException("Only admins and moderators can complete challenges")
        }
        
        // Distribute jackpot
        repository.distributeChallengeJackpot(challengeId)
        
        // Award coins to winners
        val leaderboard = repository.getClanChallengeLeaderboard(challengeId, null)
        for (entry in leaderboard.entries) {
            if (entry.isSuccessful && entry.coinsEarned > 0) {
                rewardRepository.addCoins(
                    userId = entry.userId,
                    amount = entry.coinsEarned,
                    source = CoinSource.OTHER,
                    description = "Jackpot winnings from clan challenge: ${challenge.title}",
                    metadata = "{\"challengeId\": ${challenge.id}, \"type\": \"clan_challenge_jackpot\"}"
                )
            }
        }
    }
    
    // ========== CLAN WAR METHODS ==========
    
    /**
     * Create a clan war
     */
    fun createClanWar(userId: String, request: CreateClanWarRequest): ClanWar {
        // Verify user is admin of clan1
        val membership = clanRepository.getUserClanMembership(userId, request.clan1Id)
            ?: throw IllegalStateException("Not a member of clan 1")
        
        if (membership.role != "ADMIN") {
            throw IllegalStateException("Only admins can create clan wars")
        }
        
        // Verify clan2 exists
        val clan2 = clanRepository.getClanById(request.clan2Id, userId)
            ?: throw IllegalStateException("Clan 2 not found")
        
        val startTime = Instant.parse(request.startTime)
        val endTime = Instant.parse(request.endTime)
        
        if (endTime <= startTime) {
            throw IllegalArgumentException("End time must be after start time")
        }
        
        val warId = repository.createClanWar(
            clan1Id = request.clan1Id,
            clan2Id = request.clan2Id,
            season = request.season,
            startTime = startTime,
            endTime = endTime
        )
        
        return repository.getClanWar(warId)!!
    }
    
    /**
     * Get clan war by ID
     */
    fun getClanWar(warId: Long): ClanWar {
        return repository.getClanWar(warId)
            ?: throw IllegalStateException("Clan war not found")
    }
    
    /**
     * Update clan war stats (should be called periodically)
     */
    fun updateClanWarStats(warId: Long) {
        repository.updateClanWarStats(warId)
    }
    
    // ========== EDUCATION LEADERBOARD METHODS ==========
    
    /**
     * Get clan education leaderboard
     */
    fun getClanEducationLeaderboard(
        clanId: Long,
        period: String,
        periodDate: String?,
        userId: String?
    ): ClanEducationLeaderboardResponse {
        // Verify user is member if provided
        if (userId != null) {
            val membership = clanRepository.getUserClanMembership(userId, clanId)
                ?: throw IllegalStateException("Not a member of this clan")
        }
        
        // If periodDate not provided, use current period
        val finalPeriodDate = periodDate ?: when (period) {
            "daily" -> java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            "weekly" -> {
                val date = java.time.LocalDate.now()
                val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())
                val week = date.get(weekFields.weekOfWeekBasedYear())
                val year = date.get(weekFields.weekBasedYear())
                "${year}-W${String.format("%02d", week)}"
            }
            "monthly" -> java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
            else -> throw IllegalArgumentException("Invalid period: $period")
        }
        
        return repository.getClanEducationLeaderboard(clanId, period, finalPeriodDate, userId)
    }
}

