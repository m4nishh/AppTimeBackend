package com.apptime.code.clans

import com.apptime.code.common.dbTransaction
import com.apptime.code.users.Users
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

class BehavioralOSRepository {
    
    // ========== APP CATEGORY METHODS ==========
    
    /**
     * Set app category for a user
     */
    fun setAppCategory(userId: String, packageName: String, appName: String?, category: String): UserAppCategory {
        return dbTransaction {
            val existing = UserAppCategories.select {
                (UserAppCategories.userId eq userId) and
                (UserAppCategories.packageName eq packageName)
            }.firstOrNull()
            
            if (existing != null) {
                UserAppCategories.update({
                    (UserAppCategories.userId eq userId) and
                    (UserAppCategories.packageName eq packageName)
                }) {
                    it[UserAppCategories.category] = category
                    it[UserAppCategories.appName] = appName
                    it[UserAppCategories.updatedAt] = Clock.System.now()
                }
                
                val updated = UserAppCategories.select {
                    (UserAppCategories.userId eq userId) and
                    (UserAppCategories.packageName eq packageName)
                }.first()
                
                UserAppCategory(
                    id = updated[UserAppCategories.id].value,
                    userId = updated[UserAppCategories.userId],
                    packageName = updated[UserAppCategories.packageName],
                    appName = updated[UserAppCategories.appName],
                    category = updated[UserAppCategories.category],
                    createdAt = updated[UserAppCategories.createdAt].toString(),
                    updatedAt = updated[UserAppCategories.updatedAt].toString()
                )
            } else {
                val id = UserAppCategories.insertAndGetId {
                    it[UserAppCategories.userId] = userId
                    it[UserAppCategories.packageName] = packageName
                    it[UserAppCategories.appName] = appName
                    it[UserAppCategories.category] = category
                }.value
                
                val new = UserAppCategories.select { UserAppCategories.id eq id }.first()
                UserAppCategory(
                    id = new[UserAppCategories.id].value,
                    userId = new[UserAppCategories.userId],
                    packageName = new[UserAppCategories.packageName],
                    appName = new[UserAppCategories.appName],
                    category = new[UserAppCategories.category],
                    createdAt = new[UserAppCategories.createdAt].toString(),
                    updatedAt = new[UserAppCategories.updatedAt].toString()
                )
            }
        }
    }
    
    /**
     * Get user's app categories
     */
    fun getUserAppCategories(userId: String): List<UserAppCategory> {
        return dbTransaction {
            UserAppCategories.select { UserAppCategories.userId eq userId }
                .map { row ->
                    UserAppCategory(
                        id = row[UserAppCategories.id].value,
                        userId = row[UserAppCategories.userId],
                        packageName = row[UserAppCategories.packageName],
                        appName = row[UserAppCategories.appName],
                        category = row[UserAppCategories.category],
                        createdAt = row[UserAppCategories.createdAt].toString(),
                        updatedAt = row[UserAppCategories.updatedAt].toString()
                    )
                }
        }
    }
    
    /**
     * Sync app category usage (from Android UsageStatsManager)
     */
    fun syncAppCategoryUsage(
        userId: String,
        date: String,
        productiveTime: Long,
        distractiveTime: Long,
        packageNames: String?
    ) {
        dbTransaction {
            // Update or create productive usage
            val productiveExisting = AppCategoryUsage.select {
                (AppCategoryUsage.userId eq userId) and
                (AppCategoryUsage.date eq date) and
                (AppCategoryUsage.category eq "PRODUCTIVE")
            }.firstOrNull()
            
            if (productiveExisting != null) {
                AppCategoryUsage.update({
                    (AppCategoryUsage.userId eq userId) and
                    (AppCategoryUsage.date eq date) and
                    (AppCategoryUsage.category eq "PRODUCTIVE")
                }) {
                    it[AppCategoryUsage.totalTime] = productiveTime
                    it[AppCategoryUsage.packageNames] = packageNames
                    it[AppCategoryUsage.lastSyncedAt] = Clock.System.now()
                    it[AppCategoryUsage.updatedAt] = Clock.System.now()
                }
            } else {
                AppCategoryUsage.insert {
                    it[AppCategoryUsage.userId] = userId
                    it[AppCategoryUsage.date] = date
                    it[AppCategoryUsage.category] = "PRODUCTIVE"
                    it[AppCategoryUsage.totalTime] = productiveTime
                    it[AppCategoryUsage.packageNames] = packageNames
                }
            }
            
            // Update or create distractive usage
            val distractiveExisting = AppCategoryUsage.select {
                (AppCategoryUsage.userId eq userId) and
                (AppCategoryUsage.date eq date) and
                (AppCategoryUsage.category eq "DISTRACTIVE")
            }.firstOrNull()
            
            if (distractiveExisting != null) {
                AppCategoryUsage.update({
                    (AppCategoryUsage.userId eq userId) and
                    (AppCategoryUsage.date eq date) and
                    (AppCategoryUsage.category eq "DISTRACTIVE")
                }) {
                    it[AppCategoryUsage.totalTime] = distractiveTime
                    it[AppCategoryUsage.packageNames] = packageNames
                    it[AppCategoryUsage.lastSyncedAt] = Clock.System.now()
                    it[AppCategoryUsage.updatedAt] = Clock.System.now()
                }
            } else {
                AppCategoryUsage.insert {
                    it[AppCategoryUsage.userId] = userId
                    it[AppCategoryUsage.date] = date
                    it[AppCategoryUsage.category] = "DISTRACTIVE"
                    it[AppCategoryUsage.totalTime] = distractiveTime
                    it[AppCategoryUsage.packageNames] = packageNames
                }
            }
        }
    }
    
    /**
     * Get app category usage for a user
     */
    fun getAppCategoryUsage(userId: String, date: String): AppCategoryUsageResponse? {
        return dbTransaction {
            val productive = AppCategoryUsage.select {
                (AppCategoryUsage.userId eq userId) and
                (AppCategoryUsage.date eq date) and
                (AppCategoryUsage.category eq "PRODUCTIVE")
            }.firstOrNull()
            
            val distractive = AppCategoryUsage.select {
                (AppCategoryUsage.userId eq userId) and
                (AppCategoryUsage.date eq date) and
                (AppCategoryUsage.category eq "DISTRACTIVE")
            }.firstOrNull()
            
            if (productive == null && distractive == null) {
                return@dbTransaction null
            }
            
            AppCategoryUsageResponse(
                userId = userId,
                date = date,
                productiveTime = productive?.get(AppCategoryUsage.totalTime) ?: 0L,
                distractiveTime = distractive?.get(AppCategoryUsage.totalTime) ?: 0L,
                lastSyncedAt = productive?.get(AppCategoryUsage.lastSyncedAt)?.toString() 
                    ?: distractive?.get(AppCategoryUsage.lastSyncedAt)?.toString()
            )
        }
    }
    
    // ========== CLAN VAULT METHODS ==========
    
    /**
     * Get or create clan vault
     */
    fun getOrCreateClanVault(clanId: Long): ClanVaultInfo {
        return dbTransaction {
            val vault = ClanVault.select { ClanVault.clanId eq clanId }.firstOrNull()
            
            if (vault == null) {
                ClanVault.insert {
                    it[ClanVault.clanId] = clanId
                    it[ClanVault.totalCoins] = 0L
                    it[ClanVault.lockedCoins] = 0L
                    it[ClanVault.availableCoins] = 0L
                }
                
                ClanVaultInfo(
                    clanId = clanId,
                    totalCoins = 0L,
                    lockedCoins = 0L,
                    availableCoins = 0L,
                    lastUpdatedAt = Clock.System.now().toString()
                )
            } else {
                ClanVaultInfo(
                    clanId = vault[ClanVault.clanId],
                    totalCoins = vault[ClanVault.totalCoins],
                    lockedCoins = vault[ClanVault.lockedCoins],
                    availableCoins = vault[ClanVault.availableCoins],
                    lastUpdatedAt = vault[ClanVault.lastUpdatedAt].toString()
                )
            }
        }
    }
    
    /**
     * Add coins to clan vault
     */
    fun addCoinsToVault(clanId: Long, amount: Long, lock: Boolean = false) {
        dbTransaction {
            val vault = ClanVault.select { ClanVault.clanId eq clanId }.firstOrNull()
                ?: throw IllegalStateException("Clan vault not found")
            
            ClanVault.update({ ClanVault.clanId eq clanId }) {
                it[ClanVault.totalCoins] = vault[ClanVault.totalCoins] + amount
                if (lock) {
                    it[ClanVault.lockedCoins] = vault[ClanVault.lockedCoins] + amount
                } else {
                    it[ClanVault.availableCoins] = vault[ClanVault.availableCoins] + amount
                }
                it[ClanVault.lastUpdatedAt] = Clock.System.now()
            }
        }
    }
    
    /**
     * Remove coins from clan vault
     */
    fun removeCoinsFromVault(clanId: Long, amount: Long, fromLocked: Boolean = false) {
        dbTransaction {
            val vault = ClanVault.select { ClanVault.clanId eq clanId }.firstOrNull()
                ?: throw IllegalStateException("Clan vault not found")
            
            if (fromLocked) {
                if (vault[ClanVault.lockedCoins] < amount) {
                    throw IllegalStateException("Insufficient locked coins in vault")
                }
                ClanVault.update({ ClanVault.clanId eq clanId }) {
                    it[ClanVault.totalCoins] = vault[ClanVault.totalCoins] - amount
                    it[ClanVault.lockedCoins] = vault[ClanVault.lockedCoins] - amount
                    it[ClanVault.lastUpdatedAt] = Clock.System.now()
                }
            } else {
                if (vault[ClanVault.availableCoins] < amount) {
                    throw IllegalStateException("Insufficient available coins in vault")
                }
                ClanVault.update({ ClanVault.clanId eq clanId }) {
                    it[ClanVault.totalCoins] = vault[ClanVault.totalCoins] - amount
                    it[ClanVault.availableCoins] = vault[ClanVault.availableCoins] - amount
                    it[ClanVault.lastUpdatedAt] = Clock.System.now()
                }
            }
        }
    }
    
    /**
     * Unlock coins from vault (move from locked to available)
     */
    fun unlockCoinsFromVault(clanId: Long, amount: Long) {
        dbTransaction {
            val vault = ClanVault.select { ClanVault.clanId eq clanId }.firstOrNull()
                ?: throw IllegalStateException("Clan vault not found")
            
            if (vault[ClanVault.lockedCoins] < amount) {
                throw IllegalStateException("Insufficient locked coins in vault")
            }
            
            ClanVault.update({ ClanVault.clanId eq clanId }) {
                it[ClanVault.lockedCoins] = vault[ClanVault.lockedCoins] - amount
                it[ClanVault.availableCoins] = vault[ClanVault.availableCoins] + amount
                it[ClanVault.lastUpdatedAt] = Clock.System.now()
            }
        }
    }
    
    // ========== CLAN CHALLENGE METHODS ==========
    
    /**
     * Create a clan challenge
     */
    fun createClanChallenge(
        clanId: Long,
        title: String,
        description: String?,
        challengeType: String,
        category: String,
        packageNames: String?,
        timeLimit: Long?,
        timeGoal: Long?,
        coinMultiplier: Double,
        buyInAmount: Long,
        startTime: kotlinx.datetime.Instant,
        endTime: kotlinx.datetime.Instant
    ): Long {
        return dbTransaction {
            val challengeId = ClanChallenges.insertAndGetId {
                it[ClanChallenges.clanId] = clanId
                it[ClanChallenges.title] = title
                it[ClanChallenges.description] = description
                it[ClanChallenges.challengeType] = challengeType
                it[ClanChallenges.category] = category
                it[ClanChallenges.packageNames] = packageNames
                it[ClanChallenges.timeLimit] = timeLimit
                it[ClanChallenges.timeGoal] = timeGoal
                it[ClanChallenges.coinMultiplier] = coinMultiplier
                it[ClanChallenges.buyInAmount] = buyInAmount
                it[ClanChallenges.jackpotPool] = 0L // Starts at 0, grows as members join
                it[ClanChallenges.startTime] = startTime
                it[ClanChallenges.endTime] = endTime
            }.value
            
            challengeId
        }
    }
    
    /**
     * Get clan challenge by ID
     */
    fun getClanChallenge(challengeId: Long, userId: String? = null): ClanChallenge? {
        return dbTransaction {
            val challenge = ClanChallenges.select { ClanChallenges.id eq challengeId }.firstOrNull()
                ?: return@dbTransaction null
            
            val participantCount = ClanChallengeParticipants.select {
                ClanChallengeParticipants.challengeId eq challengeId
            }.count().toInt()
            
            val hasJoined = if (userId != null) {
                ClanChallengeParticipants.select {
                    (ClanChallengeParticipants.challengeId eq challengeId) and
                    (ClanChallengeParticipants.userId eq userId)
                }.count() > 0
            } else {
                false
            }
            
            ClanChallenge(
                id = challenge[ClanChallenges.id].value,
                clanId = challenge[ClanChallenges.clanId],
                title = challenge[ClanChallenges.title],
                description = challenge[ClanChallenges.description],
                challengeType = challenge[ClanChallenges.challengeType],
                category = challenge[ClanChallenges.category],
                packageNames = challenge[ClanChallenges.packageNames],
                timeLimit = challenge[ClanChallenges.timeLimit],
                timeGoal = challenge[ClanChallenges.timeGoal],
                coinMultiplier = challenge[ClanChallenges.coinMultiplier],
                buyInAmount = challenge[ClanChallenges.buyInAmount],
                jackpotPool = challenge[ClanChallenges.jackpotPool],
                startTime = challenge[ClanChallenges.startTime].toString(),
                endTime = challenge[ClanChallenges.endTime].toString(),
                status = challenge[ClanChallenges.status],
                participantCount = participantCount,
                hasJoined = hasJoined,
                createdAt = challenge[ClanChallenges.createdAt].toString(),
                updatedAt = challenge[ClanChallenges.updatedAt].toString()
            )
        }
    }
    
    /**
     * Join a clan challenge (pay buy-in)
     */
    fun joinClanChallenge(challengeId: Long, userId: String, buyInAmount: Long): ClanChallengeParticipant {
        return dbTransaction {
            // Check if already joined
            val existing = ClanChallengeParticipants.select {
                (ClanChallengeParticipants.challengeId eq challengeId) and
                (ClanChallengeParticipants.userId eq userId)
            }.firstOrNull()
            
            if (existing != null) {
                throw IllegalStateException("Already joined this challenge")
            }
            
            // Get challenge
            val challenge = ClanChallenges.select { ClanChallenges.id eq challengeId }.firstOrNull()
                ?: throw IllegalStateException("Challenge not found")
            
            // Check if challenge is active
            if (challenge[ClanChallenges.status] != "ACTIVE") {
                throw IllegalStateException("Challenge is not active")
            }
            
            // Add participant
            val participantId = ClanChallengeParticipants.insertAndGetId {
                it[ClanChallengeParticipants.challengeId] = challengeId
                it[ClanChallengeParticipants.userId] = userId
                it[ClanChallengeParticipants.buyInPaid] = buyInAmount
            }.value
            
            // Update challenge jackpot
            ClanChallenges.update({ ClanChallenges.id eq challengeId }) {
                it[ClanChallenges.jackpotPool] = challenge[ClanChallenges.jackpotPool] + buyInAmount
            }
            
            // Add buy-in to clan vault (locked)
            val clanId = challenge[ClanChallenges.clanId]
            getOrCreateClanVault(clanId)
            addCoinsToVault(clanId, buyInAmount, lock = true)
            
            val participant = ClanChallengeParticipants.select {
                ClanChallengeParticipants.id eq participantId
            }.first()
            
            val username = Users.select { Users.userId eq userId }
                .firstOrNull()
                ?.get(Users.username)
            
            ClanChallengeParticipant(
                challengeId = participant[ClanChallengeParticipants.challengeId],
                userId = participant[ClanChallengeParticipants.userId],
                username = username,
                buyInPaid = participant[ClanChallengeParticipants.buyInPaid],
                totalTime = participant[ClanChallengeParticipants.totalTime],
                isSuccessful = participant[ClanChallengeParticipants.isSuccessful],
                coinsEarned = participant[ClanChallengeParticipants.coinsEarned],
                coinsLeaked = participant[ClanChallengeParticipants.coinsLeaked],
                joinedAt = participant[ClanChallengeParticipants.joinedAt].toString(),
                lastSyncedAt = participant[ClanChallengeParticipants.lastSyncedAt]?.toString()
            )
        }
    }
    
    /**
     * Update challenge participant stats (sync from app)
     */
    fun updateChallengeParticipantStats(
        challengeId: Long,
        userId: String,
        totalTime: Long
    ) {
        dbTransaction {
            val participant = ClanChallengeParticipants.select {
                (ClanChallengeParticipants.challengeId eq challengeId) and
                (ClanChallengeParticipants.userId eq userId)
            }.firstOrNull() ?: throw IllegalStateException("Not a participant")
            
            val challenge = ClanChallenges.select { ClanChallenges.id eq challengeId }.first()
            
            val isSuccessful = when (challenge[ClanChallenges.challengeType]) {
                "NEGATIVE_STAKE" -> {
                    // For negative stake, success means staying under limit
                    val limit = challenge[ClanChallenges.timeLimit] ?: Long.MAX_VALUE
                    totalTime <= limit
                }
                "POSITIVE_STAKE" -> {
                    // For positive stake, success means meeting goal
                    val goal = challenge[ClanChallenges.timeGoal] ?: 0L
                    totalTime >= goal
                }
                else -> false
            }
            
            ClanChallengeParticipants.update({
                (ClanChallengeParticipants.challengeId eq challengeId) and
                (ClanChallengeParticipants.userId eq userId)
            }) {
                it[ClanChallengeParticipants.totalTime] = totalTime
                it[ClanChallengeParticipants.isSuccessful] = isSuccessful
                it[ClanChallengeParticipants.lastSyncedAt] = Clock.System.now()
            }
        }
    }
    
    /**
     * Distribute jackpot after challenge ends
     */
    fun distributeChallengeJackpot(challengeId: Long) {
        dbTransaction {
            val challenge = ClanChallenges.select { ClanChallenges.id eq challengeId }.firstOrNull()
                ?: throw IllegalStateException("Challenge not found")
            
            val participants = ClanChallengeParticipants.select {
                ClanChallengeParticipants.challengeId eq challengeId
            }
            
            val successfulParticipants = participants.filter {
                it[ClanChallengeParticipants.isSuccessful]
            }
            
            val jackpot = challenge[ClanChallenges.jackpotPool]
            val clanId = challenge[ClanChallenges.clanId]
            
            if (successfulParticipants.isEmpty()) {
                // No winners, coins stay in vault
                unlockCoinsFromVault(clanId, jackpot)
                return@dbTransaction
            }
            
            // Distribute equally among successful participants
            val coinsPerWinner = jackpot / successfulParticipants.size
            val remainder = jackpot % successfulParticipants.size
            
            for (participant in successfulParticipants) {
                val userId = participant[ClanChallengeParticipants.userId]
                val coinsEarned = coinsPerWinner + if (participant == successfulParticipants.first()) remainder else 0
                
                ClanChallengeParticipants.update({
                    ClanChallengeParticipants.id eq participant[ClanChallengeParticipants.id]
                }) {
                    it[ClanChallengeParticipants.coinsEarned] = coinsEarned
                }
                
                // Award coins to user (this would need to integrate with rewards system)
                // For now, we'll just unlock from vault
            }
            
            // Unlock coins from vault for distribution
            unlockCoinsFromVault(clanId, jackpot)
            
            // Mark challenge as completed
            ClanChallenges.update({ ClanChallenges.id eq challengeId }) {
                it[ClanChallenges.status] = "COMPLETED"
            }
        }
    }
    
    /**
     * Process coin leakage for negative stake challenges
     */
    fun processCoinLeakage(challengeId: Long, userId: String, leakedAmount: Long) {
        dbTransaction {
            val participant = ClanChallengeParticipants.select {
                (ClanChallengeParticipants.challengeId eq challengeId) and
                (ClanChallengeParticipants.userId eq userId)
            }.firstOrNull() ?: throw IllegalStateException("Not a participant")
            
            val challenge = ClanChallenges.select { ClanChallenges.id eq challengeId }.first()
            val clanId = challenge[ClanChallenges.clanId]
            
            // Update participant's leaked coins
            ClanChallengeParticipants.update({
                (ClanChallengeParticipants.challengeId eq challengeId) and
                (ClanChallengeParticipants.userId eq userId)
            }) {
                it[ClanChallengeParticipants.coinsLeaked] = participant[ClanChallengeParticipants.coinsLeaked] + leakedAmount
            }
            
            // Add leaked coins to clan vault
            addCoinsToVault(clanId, leakedAmount, lock = false)
        }
    }
    
    /**
     * Get clan challenge leaderboard
     */
    fun getClanChallengeLeaderboard(challengeId: Long, userId: String? = null): ClanChallengeLeaderboardResponse {
        return dbTransaction {
            val challenge = ClanChallenges.select { ClanChallenges.id eq challengeId }.firstOrNull()
                ?: throw IllegalStateException("Challenge not found")
            
            val participants = ClanChallengeParticipants.select {
                ClanChallengeParticipants.challengeId eq challengeId
            }
                .orderBy(
                    ClanChallengeParticipants.isSuccessful to SortOrder.DESC,
                    ClanChallengeParticipants.totalTime to SortOrder.DESC
                )
                .map { row ->
                    val participantUserId = row[ClanChallengeParticipants.userId]
                    val username = Users.select { Users.userId eq participantUserId }
                        .firstOrNull()
                        ?.get(Users.username)
                    
                    Pair(
                        row[ClanChallengeParticipants.id].value,
                        ClanChallengeLeaderboardEntry(
                            rank = 0, // Will be set below
                            userId = participantUserId,
                            username = username,
                            totalTime = row[ClanChallengeParticipants.totalTime],
                            isSuccessful = row[ClanChallengeParticipants.isSuccessful],
                            coinsEarned = row[ClanChallengeParticipants.coinsEarned],
                            coinsLeaked = row[ClanChallengeParticipants.coinsLeaked]
                        )
                    )
                }
            
            val entries = participants.mapIndexed { index, (_, entry) ->
                entry.copy(rank = index + 1)
            }
            
            val userEntry = if (userId != null) {
                entries.find { it.userId == userId }
            } else {
                null
            }
            
            ClanChallengeLeaderboardResponse(
                challengeId = challengeId,
                challengeTitle = challenge[ClanChallenges.title],
                challengeType = challenge[ClanChallenges.challengeType],
                jackpotPool = challenge[ClanChallenges.jackpotPool],
                entries = entries,
                userEntry = userEntry,
                totalParticipants = entries.size
            )
        }
    }
    
    // ========== CLAN WAR METHODS ==========
    
    /**
     * Create a clan war
     */
    fun createClanWar(
        clan1Id: Long,
        clan2Id: Long,
        season: String,
        startTime: kotlinx.datetime.Instant,
        endTime: kotlinx.datetime.Instant
    ): Long {
        return dbTransaction {
            ClanWars.insertAndGetId {
                it[ClanWars.clan1Id] = clan1Id
                it[ClanWars.clan2Id] = clan2Id
                it[ClanWars.season] = season
                it[ClanWars.startTime] = startTime
                it[ClanWars.endTime] = endTime
            }.value
        }
    }
    
    /**
     * Update clan war stats
     */
    fun updateClanWarStats(warId: Long) {
        dbTransaction {
            val war = ClanWars.select { ClanWars.id eq warId }.firstOrNull()
                ?: throw IllegalStateException("Clan war not found")
            
            val clan1Id = war[ClanWars.clan1Id]
            val clan2Id = war[ClanWars.clan2Id]
            val startTime = war[ClanWars.startTime]
            val endTime = war[ClanWars.endTime]
            
            // Get date range
            val startDate = LocalDate.parse(startTime.toString().substring(0, 10))
            val endDate = LocalDate.parse(endTime.toString().substring(0, 10))
            
            // Calculate productive and distractive time for each clan
            val clan1Members = ClanMembers.select {
                (ClanMembers.clanId eq clan1Id) and (ClanMembers.isActive eq true)
            }.map { it[ClanMembers.userId] }
            
            val clan2Members = ClanMembers.select {
                (ClanMembers.clanId eq clan2Id) and (ClanMembers.isActive eq true)
            }.map { it[ClanMembers.userId] }
            
            var clan1Productive = 0L
            var clan1Distractive = 0L
            var clan2Productive = 0L
            var clan2Distractive = 0L
            
            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                val dateStr = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                
                // Clan 1 stats
                for (memberId in clan1Members) {
                    val productive = AppCategoryUsage.select {
                        (AppCategoryUsage.userId eq memberId) and
                        (AppCategoryUsage.date eq dateStr) and
                        (AppCategoryUsage.category eq "PRODUCTIVE")
                    }.firstOrNull()?.get(AppCategoryUsage.totalTime) ?: 0L
                    
                    val distractive = AppCategoryUsage.select {
                        (AppCategoryUsage.userId eq memberId) and
                        (AppCategoryUsage.date eq dateStr) and
                        (AppCategoryUsage.category eq "DISTRACTIVE")
                    }.firstOrNull()?.get(AppCategoryUsage.totalTime) ?: 0L
                    
                    clan1Productive += productive
                    clan1Distractive += distractive
                }
                
                // Clan 2 stats
                for (memberId in clan2Members) {
                    val productive = AppCategoryUsage.select {
                        (AppCategoryUsage.userId eq memberId) and
                        (AppCategoryUsage.date eq dateStr) and
                        (AppCategoryUsage.category eq "PRODUCTIVE")
                    }.firstOrNull()?.get(AppCategoryUsage.totalTime) ?: 0L
                    
                    val distractive = AppCategoryUsage.select {
                        (AppCategoryUsage.userId eq memberId) and
                        (AppCategoryUsage.date eq dateStr) and
                        (AppCategoryUsage.category eq "DISTRACTIVE")
                    }.firstOrNull()?.get(AppCategoryUsage.totalTime) ?: 0L
                    
                    clan2Productive += productive
                    clan2Distractive += distractive
                }
                
                currentDate = currentDate.plusDays(1)
            }
            
            // Calculate ratios (Learning Time / Distraction Time)
            val clan1Ratio = if (clan1Distractive > 0) {
                clan1Productive.toDouble() / clan1Distractive.toDouble()
            } else {
                if (clan1Productive > 0) Double.MAX_VALUE else 0.0
            }
            
            val clan2Ratio = if (clan2Distractive > 0) {
                clan2Productive.toDouble() / clan2Distractive.toDouble()
            } else {
                if (clan2Productive > 0) Double.MAX_VALUE else 0.0
            }
            
            // Determine winner
            val winnerClanId = when {
                clan1Ratio > clan2Ratio -> clan1Id
                clan2Ratio > clan1Ratio -> clan2Id
                else -> null // Tie
            }
            
            // Update war stats
            ClanWars.update({ ClanWars.id eq warId }) {
                it[ClanWars.clan1ProductiveTime] = clan1Productive
                it[ClanWars.clan1DistractiveTime] = clan1Distractive
                it[ClanWars.clan2ProductiveTime] = clan2Productive
                it[ClanWars.clan2DistractiveTime] = clan2Distractive
                it[ClanWars.clan1Ratio] = clan1Ratio
                it[ClanWars.clan2Ratio] = clan2Ratio
                it[ClanWars.winnerClanId] = winnerClanId
                it[ClanWars.updatedAt] = Clock.System.now()
                
                // Check if war period has ended
                if (Clock.System.now() >= endTime) {
                    it[ClanWars.status] = "COMPLETED"
                }
            }
        }
    }
    
    /**
     * Get clan war by ID
     */
    fun getClanWar(warId: Long): ClanWar? {
        return dbTransaction {
            val war = ClanWars.select { ClanWars.id eq warId }.firstOrNull()
                ?: return@dbTransaction null
            
            val clan1 = Clans.select { Clans.id eq war[ClanWars.clan1Id] }.firstOrNull()
            val clan2 = Clans.select { Clans.id eq war[ClanWars.clan2Id] }.firstOrNull()
            
            ClanWar(
                id = war[ClanWars.id].value,
                clan1Id = war[ClanWars.clan1Id],
                clan1Name = clan1?.get(Clans.name),
                clan2Id = war[ClanWars.clan2Id],
                clan2Name = clan2?.get(Clans.name),
                season = war[ClanWars.season],
                startTime = war[ClanWars.startTime].toString(),
                endTime = war[ClanWars.endTime].toString(),
                clan1ProductiveTime = war[ClanWars.clan1ProductiveTime],
                clan1DistractiveTime = war[ClanWars.clan1DistractiveTime],
                clan2ProductiveTime = war[ClanWars.clan2ProductiveTime],
                clan2DistractiveTime = war[ClanWars.clan2DistractiveTime],
                clan1Ratio = war[ClanWars.clan1Ratio],
                clan2Ratio = war[ClanWars.clan2Ratio],
                winnerClanId = war[ClanWars.winnerClanId],
                status = war[ClanWars.status],
                createdAt = war[ClanWars.createdAt].toString(),
                updatedAt = war[ClanWars.updatedAt].toString()
            )
        }
    }
    
    // ========== EDUCATION LEADERBOARD METHODS ==========
    
    /**
     * Update clan education leaderboard
     */
    fun updateClanEducationLeaderboard(clanId: Long, userId: String, period: String, periodDate: String, productiveTime: Long) {
        dbTransaction {
            val existing = ClanEducationLeaderboard.select {
                (ClanEducationLeaderboard.clanId eq clanId) and
                (ClanEducationLeaderboard.userId eq userId) and
                (ClanEducationLeaderboard.period eq period) and
                (ClanEducationLeaderboard.periodDate eq periodDate)
            }.firstOrNull()
            
            if (existing != null) {
                ClanEducationLeaderboard.update({
                    (ClanEducationLeaderboard.clanId eq clanId) and
                    (ClanEducationLeaderboard.userId eq userId) and
                    (ClanEducationLeaderboard.period eq period) and
                    (ClanEducationLeaderboard.periodDate eq periodDate)
                }) {
                    it[ClanEducationLeaderboard.productiveTime] = productiveTime
                    it[ClanEducationLeaderboard.updatedAt] = Clock.System.now()
                }
            } else {
                ClanEducationLeaderboard.insert {
                    it[ClanEducationLeaderboard.clanId] = clanId
                    it[ClanEducationLeaderboard.userId] = userId
                    it[ClanEducationLeaderboard.period] = period
                    it[ClanEducationLeaderboard.periodDate] = periodDate
                    it[ClanEducationLeaderboard.productiveTime] = productiveTime
                }
            }
            
            // Recalculate ranks
            recalculateEducationLeaderboardRanks(clanId, period, periodDate)
        }
    }
    
    /**
     * Recalculate ranks for education leaderboard
     */
    private fun recalculateEducationLeaderboardRanks(clanId: Long, period: String, periodDate: String) {
        val entries = ClanEducationLeaderboard.select {
            (ClanEducationLeaderboard.clanId eq clanId) and
            (ClanEducationLeaderboard.period eq period) and
            (ClanEducationLeaderboard.periodDate eq periodDate)
        }
            .orderBy(ClanEducationLeaderboard.productiveTime to SortOrder.DESC)
            .mapIndexed { index, row ->
                Pair(row[ClanEducationLeaderboard.id].value, index + 1)
            }
        
        for ((entryId, rank) in entries) {
            ClanEducationLeaderboard.update({ ClanEducationLeaderboard.id eq entryId }) {
                it[ClanEducationLeaderboard.rank] = rank
            }
        }
    }
    
    /**
     * Get clan education leaderboard
     */
    fun getClanEducationLeaderboard(clanId: Long, period: String, periodDate: String, userId: String? = null): ClanEducationLeaderboardResponse {
        return dbTransaction {
            val entries = ClanEducationLeaderboard.select {
                (ClanEducationLeaderboard.clanId eq clanId) and
                (ClanEducationLeaderboard.period eq period) and
                (ClanEducationLeaderboard.periodDate eq periodDate)
            }
                .orderBy(ClanEducationLeaderboard.rank to SortOrder.ASC)
                .map { row ->
                    val entryUserId = row[ClanEducationLeaderboard.userId]
                    val username = Users.select { Users.userId eq entryUserId }
                        .firstOrNull()
                        ?.get(Users.username)
                    
                    // Calculate daily average based on period
                    val dailyAverage = when (period) {
                        "daily" -> row[ClanEducationLeaderboard.productiveTime]
                        "weekly" -> row[ClanEducationLeaderboard.productiveTime] / 7
                        "monthly" -> row[ClanEducationLeaderboard.productiveTime] / 30
                        else -> null
                    }
                    
                    ClanEducationLeaderboardEntry(
                        rank = row[ClanEducationLeaderboard.rank] ?: 0,
                        userId = entryUserId,
                        username = username,
                        productiveTime = row[ClanEducationLeaderboard.productiveTime],
                        dailyAverage = dailyAverage
                    )
                }
            
            val userEntry = if (userId != null) {
                entries.find { it.userId == userId }
            } else {
                null
            }
            
            ClanEducationLeaderboardResponse(
                clanId = clanId,
                period = period,
                periodDate = periodDate,
                entries = entries,
                userEntry = userEntry,
                totalParticipants = entries.size
            )
        }
    }
}

