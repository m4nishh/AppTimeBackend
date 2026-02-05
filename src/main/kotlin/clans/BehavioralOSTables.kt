package com.apptime.code.clans

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * User App Categories table - stores user-defined app categories
 * Users can mark apps as "PRODUCTIVE" (EdTech/Learning) or "DISTRACTIVE" (Social Media/Entertainment)
 */
object UserAppCategories : LongIdTable("user_app_categories") {
    val userId = varchar("user_id", 255).index()
    val packageName = varchar("package_name", 255).index()
    val appName = varchar("app_name", 255).nullable()
    val category = varchar("category", 50) // "PRODUCTIVE" or "DISTRACTIVE"
    val createdAt = timestamp("created_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    
    init {
        // One category per user per package
        uniqueIndex(userId, packageName)
        index(false, userId, category)
    }
}

/**
 * App Category Usage table - tracks daily usage by category per user
 * Used for syncing app usage from Android UsageStatsManager
 */
object AppCategoryUsage : Table("app_category_usage") {
    val id = long("id").autoIncrement()
    val userId = varchar("user_id", 255).index()
    val date = varchar("date", 10) // YYYY-MM-DD format
    val category = varchar("category", 50).index() // "PRODUCTIVE" or "DISTRACTIVE"
    val totalTime = long("total_time").default(0L) // Total time in milliseconds
    val packageNames = text("package_names").nullable() // Comma-separated list of packages used (for privacy, can be null)
    val lastSyncedAt = timestamp("last_synced_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    val createdAt = timestamp("created_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        // One entry per user per date per category
        uniqueIndex(userId, date, category)
        index(false, userId, date)
        index(false, date, category)
    }
}

/**
 * Clan Vault table - stores coin pools for clans
 * Used for challenge buy-ins and jackpot distributions
 */
object ClanVault : LongIdTable("clan_vault") {
    val clanId = long("clan_id")
        .references(Clans.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val totalCoins = long("total_coins").default(0L) // Total coins in vault
    val lockedCoins = long("locked_coins").default(0L) // Coins locked in active challenges
    val availableCoins = long("available_coins").default(0L) // Available for withdrawal/distribution
    val lastUpdatedAt = timestamp("last_updated_at")
        .clientDefault { kotlinx.datetime.Clock.System.now() }
    
    init {
        uniqueIndex(clanId)
    }
}

/**
 * Clan Challenges table - stores clan-specific challenges with buy-ins
 * Supports both "Negative Stake" (Scroll-Less Sprint) and "Positive Stake" (Knowledge Surge) challenges
 */
object ClanChallenges : LongIdTable("clan_challenges") {
    val clanId = long("clan_id")
        .references(Clans.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val challengeType = varchar("challenge_type", 50) // "NEGATIVE_STAKE" or "POSITIVE_STAKE"
    val category = varchar("category", 50) // "DISTRACTIVE" for negative, "PRODUCTIVE" for positive
    val packageNames = text("package_names").nullable() // Comma-separated package names to track
    val timeLimit = long("time_limit").nullable() // For negative: max time in ms (e.g., 30 min for Instagram)
    val timeGoal = long("time_goal").nullable() // For positive: goal time in ms
    val coinMultiplier = double("coin_multiplier").default(2.0) // For positive: bonus multiplier (e.g., 2x)
    val buyInAmount = long("buy_in_amount").default(0L) // Coins required to join
    val jackpotPool = long("jackpot_pool").default(0L) // Total coins in jackpot
    val startTime = timestamp("start_time")
    val endTime = timestamp("end_time")
    val status = varchar("status", 50).default("ACTIVE") // ACTIVE, COMPLETED, CANCELLED
    val createdAt = timestamp("created_at")
        .clientDefault { kotlinx.datetime.Clock.System.now() }
    val updatedAt = timestamp("updated_at")
        .clientDefault { kotlinx.datetime.Clock.System.now() }
    
    init {
        index(false, clanId, status)
        index(false, status, startTime, endTime)
    }
}

/**
 * Clan Challenge Participants table - tracks members in clan challenges
 */
object ClanChallengeParticipants : LongIdTable("clan_challenge_participants") {
    val challengeId = long("challenge_id")
        .references(ClanChallenges.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val userId = varchar("user_id", 255).index()
    val buyInPaid = long("buy_in_paid").default(0L)
    val totalTime = long("total_time").default(0L) // Total time in milliseconds
    val isSuccessful = bool("is_successful").default(false) // Met the challenge goal
    val coinsEarned = long("coins_earned").default(0L) // Coins earned from jackpot
    val coinsLeaked = long("coins_leaked").default(0L) // Coins leaked to vault (for negative stake)
    val joinedAt = timestamp("joined_at")
        .clientDefault { kotlinx.datetime.Clock.System.now() }
    val lastSyncedAt = timestamp("last_synced_at").nullable()
    
    init {
        uniqueIndex(challengeId, userId)
        index(false, challengeId, isSuccessful)
    }
}

/**
 * Clan Wars table - competitive mode between clans
 */
object ClanWars : LongIdTable("clan_wars") {
    val clan1Id = long("clan1_id")
        .references(Clans.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val clan2Id = long("clan2_id")
        .references(Clans.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val season = varchar("season", 50).index() // e.g., "2024-Q1"
    val startTime = timestamp("start_time")
    val endTime = timestamp("end_time")
    val clan1ProductiveTime = long("clan1_productive_time").default(0L) // Total productive time in ms
    val clan1DistractiveTime = long("clan1_distractive_time").default(0L) // Total distractive time in ms
    val clan2ProductiveTime = long("clan2_productive_time").default(0L)
    val clan2DistractiveTime = long("clan2_distractive_time").default(0L)
    val clan1Ratio = double("clan1_ratio").default(0.0) // Learning Time / Distraction Time
    val clan2Ratio = double("clan2_ratio").default(0.0)
    val winnerClanId = long("winner_clan_id").nullable()
    val status = varchar("status", 50).default("ACTIVE") // ACTIVE, COMPLETED, CANCELLED
    val createdAt = timestamp("created_at")
        .clientDefault { kotlinx.datetime.Clock.System.now() }
    val updatedAt = timestamp("updated_at")
        .clientDefault { kotlinx.datetime.Clock.System.now() }
    
    init {
        index(false, status, startTime, endTime)
        index(false, season, status)
    }
}

/**
 * Clan Education Leaderboard table - tracks productive app usage within clans
 * Used for "Scholar of the Week" leaderboard
 */
object ClanEducationLeaderboard : Table("clan_education_leaderboard") {
    val id = long("id").autoIncrement()
    val clanId = long("clan_id")
        .references(Clans.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val userId = varchar("user_id", 255).index()
    val period = varchar("period", 20) // "daily", "weekly", "monthly"
    val periodDate = varchar("period_date", 20) // YYYY-MM-DD, YYYY-WW, or YYYY-MM
    val productiveTime = long("productive_time").default(0L) // Total productive time in ms
    val rank = integer("rank").nullable() // Rank within clan for this period
    val createdAt = timestamp("created_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        uniqueIndex(clanId, userId, period, periodDate)
        index(false, clanId, period, periodDate)
    }
}

