package com.apptime.code.leaderboard

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Leaderboard stats table - pre-aggregated leaderboard data
 * Can be refreshed periodically or computed on-demand
 */
object LeaderboardStats : Table("leaderboard_stats") {
    val id = long("id").autoIncrement()
    val userId = varchar("user_id", 255).index()
    val username = varchar("username", 255).nullable()
    val period = varchar("period", 20).index() // "daily", "weekly", "monthly"
    val periodDate = varchar("period_date", 10).index() // YYYY-MM-DD for daily, YYYY-WW for weekly, YYYY-MM for monthly
    val totalScreenTime = long("total_screen_time").default(0L) // milliseconds
    val rank = integer("rank").nullable()
    
    val createdAt = timestamp("created_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        uniqueIndex(userId, period, periodDate)
    }
}

