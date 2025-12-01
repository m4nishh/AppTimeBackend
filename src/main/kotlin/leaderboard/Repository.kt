package com.apptime.code.leaderboard

import com.apptime.code.common.dbTransaction
import com.apptime.code.users.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import usage.AppUsageEvents
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

class LeaderboardRepository {
    
    /**
     * Update leaderboard stats with focus time
     */
    fun updateLeaderboardWithFocusTime(
        userId: String,
        focusDuration: Long,
        date: LocalDate
    ) {
        dbTransaction {
            // Get username
            val username = Users.select { Users.userId eq userId }
                .firstOrNull()
                ?.get(Users.username)
            
            // Update daily stats
            updateDailyStats(userId, username, date, focusDuration)
            
            // Update weekly stats
            val weekDate = getWeekDate(date)
            updateWeeklyStats(userId, username, weekDate, focusDuration)
            
            // Update monthly stats
            val monthDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            updateMonthlyStats(userId, username, monthDate, focusDuration)
        }
    }
    
    private fun updateDailyStats(userId: String, username: String?, date: LocalDate, focusDuration: Long) {
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        
        val existing = LeaderboardStats.select {
            (LeaderboardStats.userId eq userId) and
            (LeaderboardStats.period eq "daily") and
            (LeaderboardStats.periodDate eq dateStr)
        }.firstOrNull()
        
        if (existing != null) {
            // Update existing
            LeaderboardStats.update({
                (LeaderboardStats.userId eq userId) and
                (LeaderboardStats.period eq "daily") and
                (LeaderboardStats.periodDate eq dateStr)
            }) {
                it[LeaderboardStats.totalScreenTime] = existing[LeaderboardStats.totalScreenTime] + focusDuration
                it[LeaderboardStats.updatedAt] = kotlinx.datetime.Clock.System.now()
                if (username != null) {
                    it[LeaderboardStats.username] = username
                }
            }
        } else {
            // Insert new
            LeaderboardStats.insert {
                it[LeaderboardStats.userId] = userId
                it[LeaderboardStats.username] = username
                it[LeaderboardStats.period] = "daily"
                it[LeaderboardStats.periodDate] = dateStr
                it[LeaderboardStats.totalScreenTime] = focusDuration
            }
        }
    }
    
    private fun updateWeeklyStats(userId: String, username: String?, weekDate: String, focusDuration: Long) {
        val existing = LeaderboardStats.select {
            (LeaderboardStats.userId eq userId) and
            (LeaderboardStats.period eq "weekly") and
            (LeaderboardStats.periodDate eq weekDate)
        }.firstOrNull()
        
        if (existing != null) {
            LeaderboardStats.update({
                (LeaderboardStats.userId eq userId) and
                (LeaderboardStats.period eq "weekly") and
                (LeaderboardStats.periodDate eq weekDate)
            }) {
                it[LeaderboardStats.totalScreenTime] = existing[LeaderboardStats.totalScreenTime] + focusDuration
                it[LeaderboardStats.updatedAt] = kotlinx.datetime.Clock.System.now()
                if (username != null) {
                    it[LeaderboardStats.username] = username
                }
            }
        } else {
            LeaderboardStats.insert {
                it[LeaderboardStats.userId] = userId
                it[LeaderboardStats.username] = username
                it[LeaderboardStats.period] = "weekly"
                it[LeaderboardStats.periodDate] = weekDate
                it[LeaderboardStats.totalScreenTime] = focusDuration
            }
        }
    }
    
    private fun updateMonthlyStats(userId: String, username: String?, monthDate: String, focusDuration: Long) {
        val existing = LeaderboardStats.select {
            (LeaderboardStats.userId eq userId) and
            (LeaderboardStats.period eq "monthly") and
            (LeaderboardStats.periodDate eq monthDate)
        }.firstOrNull()
        
        if (existing != null) {
            LeaderboardStats.update({
                (LeaderboardStats.userId eq userId) and
                (LeaderboardStats.period eq "monthly") and
                (LeaderboardStats.periodDate eq monthDate)
            }) {
                it[LeaderboardStats.totalScreenTime] = existing[LeaderboardStats.totalScreenTime] + focusDuration
                it[LeaderboardStats.updatedAt] = kotlinx.datetime.Clock.System.now()
                if (username != null) {
                    it[LeaderboardStats.username] = username
                }
            }
        } else {
            LeaderboardStats.insert {
                it[LeaderboardStats.userId] = userId
                it[LeaderboardStats.username] = username
                it[LeaderboardStats.period] = "monthly"
                it[LeaderboardStats.periodDate] = monthDate
                it[LeaderboardStats.totalScreenTime] = focusDuration
            }
        }
    }
    
    private fun getWeekDate(date: LocalDate): String {
        val weekFields = WeekFields.of(Locale.getDefault())
        val week = date.get(weekFields.weekOfWeekBasedYear())
        val year = date.get(weekFields.weekBasedYear())
        return "${year}-W${String.format("%02d", week)}"
    }
    
    /**
     * Sync data from app_usage_events to leaderboardstats
     * Aggregates screen time by user and period (daily, weekly, monthly)
     * @param date Optional date to sync. If null, syncs all dates from app_usage_events
     * @return LeaderboardSyncResponse with sync statistics
     */
    fun syncFromAppUsageEvents(date: LocalDate? = null): LeaderboardSyncResponse {
        return dbTransaction {
            // Get all events with duration (these represent actual screen time)
            val eventsQuery = if (date != null) {
                // Sync for specific date
                val startOfDay = date.atStartOfDay(ZoneId.of("UTC")).toInstant()
                val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant()
                val startInstant = kotlinx.datetime.Instant.fromEpochMilliseconds(startOfDay.toEpochMilli())
                val endInstant = kotlinx.datetime.Instant.fromEpochMilliseconds(endOfDay.toEpochMilli())
                
                AppUsageEvents.select {
                    (AppUsageEvents.duration.isNotNull()) and
                    (AppUsageEvents.eventTimestamp greaterEq startInstant) and
                    (AppUsageEvents.eventTimestamp less endInstant)
                }
            } else {
                // Sync all events
                AppUsageEvents.select {
                    AppUsageEvents.duration.isNotNull()
                }
            }
            
            val events = eventsQuery.toList()
            
            if (events.isEmpty()) {
                return@dbTransaction LeaderboardSyncResponse(
                    message = "No events found to sync",
                    eventsProcessed = 0,
                    usersUpdated = 0,
                    statsCreated = 0,
                    statsUpdated = 0,
                    dateSynced = date?.toString()
                )
            }
            
            // Group events by userId and date
            val userDateStats = events.groupBy { event ->
                val eventTimestamp = event[AppUsageEvents.eventTimestamp]
                val localDate = java.time.Instant.ofEpochMilli(eventTimestamp.toEpochMilliseconds())
                    .atZone(ZoneId.of("UTC"))
                    .toLocalDate()
                Pair(event[AppUsageEvents.userId], localDate)
            }.mapValues { (_, eventList) ->
                eventList.sumOf { it[AppUsageEvents.duration] ?: 0L }
            }
            
            var statsCreated = 0
            var statsUpdated = 0
            val usersUpdated = mutableSetOf<String>()
            
            // Group by period for aggregation
            val dailyStats = mutableMapOf<Pair<String, String>, Long>() // (userId, dateStr) -> totalTime
            val weeklyStats = mutableMapOf<Pair<String, String>, Long>() // (userId, weekDate) -> totalTime
            val monthlyStats = mutableMapOf<Pair<String, String>, Long>() // (userId, monthDate) -> totalTime
            
            // Process each user-date combination
            userDateStats.forEach { (userDatePair, totalScreenTime) ->
                val (userId, eventDate) = userDatePair
                usersUpdated.add(userId)
                
                // Aggregate daily stats
                val dateStr = eventDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                dailyStats[Pair(userId, dateStr)] = 
                    dailyStats.getOrDefault(Pair(userId, dateStr), 0L) + totalScreenTime
                
                // Only aggregate weekly/monthly stats if syncing all dates
                // (to avoid double-counting when syncing specific dates multiple times)
                if (date == null) {
                    // Aggregate weekly stats
                    val weekDate = getWeekDate(eventDate)
                    weeklyStats[Pair(userId, weekDate)] = 
                        weeklyStats.getOrDefault(Pair(userId, weekDate), 0L) + totalScreenTime
                    
                    // Aggregate monthly stats
                    val monthDate = eventDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                    monthlyStats[Pair(userId, monthDate)] = 
                        monthlyStats.getOrDefault(Pair(userId, monthDate), 0L) + totalScreenTime
                }
            }
            
            // Get all usernames at once for efficiency
            val userIds = usersUpdated.toList()
            val usernameMap = Users.select { Users.userId inList userIds }
                .associate { it[Users.userId] to it[Users.username] }
            
            // Update daily stats
            dailyStats.forEach { (key, totalScreenTime) ->
                val (userId, dateStr) = key
                val username = usernameMap[userId]
                val result = updateOrCreateStats(userId, username, "daily", dateStr, totalScreenTime, true)
                if (result == "created") statsCreated++ else statsUpdated++
            }
            
            // Update weekly stats (only if syncing all dates)
            if (date == null) {
                weeklyStats.forEach { (key, totalScreenTime) ->
                    val (userId, weekDate) = key
                    val username = usernameMap[userId]
                    val result = updateOrCreateStats(userId, username, "weekly", weekDate, totalScreenTime, true)
                    if (result == "created") statsCreated++ else statsUpdated++
                }
                
                // Update monthly stats
                monthlyStats.forEach { (key, totalScreenTime) ->
                    val (userId, monthDate) = key
                    val username = usernameMap[userId]
                    val result = updateOrCreateStats(userId, username, "monthly", monthDate, totalScreenTime, true)
                    if (result == "created") statsCreated++ else statsUpdated++
                }
            }
            
            LeaderboardSyncResponse(
                message = "Sync completed successfully",
                eventsProcessed = events.size,
                usersUpdated = usersUpdated.size,
                statsCreated = statsCreated,
                statsUpdated = statsUpdated,
                dateSynced = date?.toString()
            )
        }
    }
    
    /**
     * Update or create leaderboard stats entry
     * @param replace If true, replaces existing total. If false, adds to existing total.
     * @return "created" if new entry was created, "updated" if existing was updated
     */
    private fun updateOrCreateStats(
        userId: String,
        username: String?,
        period: String,
        periodDate: String,
        screenTime: Long,
        replace: Boolean = false
    ): String {
        // First, check if there are any existing entries (handle potential duplicates)
        val existingEntries = LeaderboardStats.select {
            (LeaderboardStats.userId eq userId) and
            (LeaderboardStats.period eq period) and
            (LeaderboardStats.periodDate eq periodDate)
        }.toList()
        
        // If multiple entries exist (duplicates), merge them first
        if (existingEntries.size > 1) {
            val totalTime = existingEntries.sumOf { it[LeaderboardStats.totalScreenTime] }
            val firstId = existingEntries.first()[LeaderboardStats.id]
            
            // Delete all duplicates except the first one
            val duplicateIds = existingEntries.drop(1).map { it[LeaderboardStats.id] }
            if (duplicateIds.isNotEmpty()) {
                LeaderboardStats.deleteWhere {
                    LeaderboardStats.id inList duplicateIds
                }
            }
            
            // Update the first entry with merged total
            val newTotal = if (replace) {
                screenTime
            } else {
                totalTime + screenTime
            }
            
            LeaderboardStats.update({ LeaderboardStats.id eq firstId }) {
                it[LeaderboardStats.totalScreenTime] = newTotal
                it[LeaderboardStats.updatedAt] = kotlinx.datetime.Clock.System.now()
                if (username != null) {
                    it[LeaderboardStats.username] = username
                }
            }
            return "updated"
        }
        
        val existing = existingEntries.firstOrNull()
        
        return if (existing != null) {
            // Update existing
            val newTotal = if (replace) {
                screenTime
            } else {
                existing[LeaderboardStats.totalScreenTime] + screenTime
            }
            
            LeaderboardStats.update({
                (LeaderboardStats.userId eq userId) and
                (LeaderboardStats.period eq period) and
                (LeaderboardStats.periodDate eq periodDate)
            }) {
                it[LeaderboardStats.totalScreenTime] = newTotal
                it[LeaderboardStats.updatedAt] = kotlinx.datetime.Clock.System.now()
                if (username != null) {
                    it[LeaderboardStats.username] = username
                }
            }
            "updated"
        } else {
            // Insert new - use insertIgnore to handle race conditions
            try {
                LeaderboardStats.insert {
                    it[LeaderboardStats.userId] = userId
                    it[LeaderboardStats.username] = username
                    it[LeaderboardStats.period] = period
                    it[LeaderboardStats.periodDate] = periodDate
                    it[LeaderboardStats.totalScreenTime] = screenTime
                }
                "created"
            } catch (e: Exception) {
                // If insert fails due to unique constraint, update instead
                val existingAfterInsert = LeaderboardStats.select {
                    (LeaderboardStats.userId eq userId) and
                    (LeaderboardStats.period eq period) and
                    (LeaderboardStats.periodDate eq periodDate)
                }.firstOrNull()
                
                if (existingAfterInsert != null) {
                    val newTotal = if (replace) {
                        screenTime
                    } else {
                        existingAfterInsert[LeaderboardStats.totalScreenTime] + screenTime
                    }
                    
                    LeaderboardStats.update({
                        (LeaderboardStats.userId eq userId) and
                        (LeaderboardStats.period eq period) and
                        (LeaderboardStats.periodDate eq periodDate)
                    }) {
                        it[LeaderboardStats.totalScreenTime] = newTotal
                        it[LeaderboardStats.updatedAt] = kotlinx.datetime.Clock.System.now()
                        if (username != null) {
                            it[LeaderboardStats.username] = username
                        }
                    }
                    "updated"
                } else {
                    throw e
                }
            }
        }
    }
    
    /**
     * Get daily leaderboard entries
     * @param date Date in YYYY-MM-DD format
     * @param currentUserId Optional current user ID to determine userRank
     * @return LeaderboardResponse with entries sorted by totalScreenTime descending
     */
    fun getDailyLeaderboard(date: String, currentUserId: String? = null): LeaderboardResponse {
        return dbTransaction {
            // Query leaderboard stats for the given date, joined with users table
            val entries = LeaderboardStats
                .join(Users, JoinType.INNER, LeaderboardStats.userId, Users.userId)
                .select {
                    (LeaderboardStats.period eq "daily") and
                    (LeaderboardStats.periodDate eq date)
                }
                .orderBy(LeaderboardStats.totalScreenTime to SortOrder.DESC)
                .mapIndexed { index, row ->
                    LeaderboardEntry(
                        userId = row[LeaderboardStats.userId],
                        username = row[LeaderboardStats.username] ?: row[Users.username],
                        name = row[Users.name],
                        avatar = null, // Avatar not stored in Users table
                        totalScreenTime = row[LeaderboardStats.totalScreenTime],
                        rank = index + 1
                    )
                }
            
            // Find current user's rank if userId is provided
            val userRank = currentUserId?.let { userId ->
                entries.find { it.userId == userId }?.rank
            }
            
            LeaderboardResponse(
                period = "daily",
                periodDate = date,
                entries = entries,
                userRank = userRank,
                totalUsers = entries.size
            )
        }
    }
    
    /**
     * Get weekly leaderboard entries
     * @param weekDate Week date in YYYY-WW format
     * @param currentUserId Optional current user ID to determine userRank
     * @return LeaderboardResponse with entries sorted by totalScreenTime descending
     */
    fun getWeeklyLeaderboard(weekDate: String, currentUserId: String? = null): LeaderboardResponse {
        return dbTransaction {
            // Query leaderboard stats for the given week, joined with users table
            val entries = LeaderboardStats
                .join(Users, JoinType.INNER, LeaderboardStats.userId, Users.userId)
                .select {
                    (LeaderboardStats.period eq "weekly") and
                    (LeaderboardStats.periodDate eq weekDate)
                }
                .orderBy(LeaderboardStats.totalScreenTime to SortOrder.DESC)
                .mapIndexed { index, row ->
                    LeaderboardEntry(
                        userId = row[LeaderboardStats.userId],
                        username = row[LeaderboardStats.username] ?: row[Users.username],
                        name = row[Users.name],
                        avatar = null, // Avatar not stored in Users table
                        totalScreenTime = row[LeaderboardStats.totalScreenTime],
                        rank = index + 1
                    )
                }
            
            // Find current user's rank if userId is provided
            val userRank = currentUserId?.let { userId ->
                entries.find { it.userId == userId }?.rank
            }
            
            LeaderboardResponse(
                period = "weekly",
                periodDate = weekDate,
                entries = entries,
                userRank = userRank,
                totalUsers = entries.size
            )
        }
    }
}

