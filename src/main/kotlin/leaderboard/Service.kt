package com.apptime.code.leaderboard

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

/**
 * Leaderboard service - handles business logic for leaderboard operations
 */
class LeaderboardService(
    private val repository: LeaderboardRepository
) {
    
    /**
     * Get daily leaderboard
     * @param date Optional date in YYYY-MM-DD format. If not provided, uses today's date
     * @param currentUserId Optional current user ID to determine userRank
     */
    suspend fun getDailyLeaderboard(date: String? = null, currentUserId: String? = null): LeaderboardResponse {
        // Parse and validate date
        val periodDate = if (date != null) {
            try {
                // Validate date format
                LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                date
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid date format. Expected YYYY-MM-DD")
            }
        } else {
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }
        
        return repository.getDailyLeaderboard(periodDate, currentUserId)
    }
    
    /**
     * Get weekly leaderboard
     * @param weekDate Optional week date in YYYY-WW format. If not provided, uses current week
     * @param currentUserId Optional current user ID to determine userRank
     */
    suspend fun getWeeklyLeaderboard(weekDate: String? = null, currentUserId: String? = null): LeaderboardResponse {
        // Parse and validate week date
        val periodDate = if (weekDate != null) {
            // Validate week date format (YYYY-WW)
            if (!weekDate.matches(Regex("\\d{4}-W\\d{2}"))) {
                throw IllegalArgumentException("Invalid week date format. Expected YYYY-WW (e.g., 2024-W01)")
            }
            weekDate
        } else {
            getCurrentWeekDate()
        }
        
        return repository.getWeeklyLeaderboard(periodDate, currentUserId)
    }
    
    /**
     * Get current week date in YYYY-WW format
     */
    private fun getCurrentWeekDate(): String {
        val date = LocalDate.now()
        val weekFields = WeekFields.of(Locale.getDefault())
        val week = date.get(weekFields.weekOfWeekBasedYear())
        val year = date.get(weekFields.weekBasedYear())
        return "${year}-W${String.format("%02d", week)}"
    }
    
    /**
     * Sync data from app_usage_events to leaderboardstats
     * @param date Optional date in YYYY-MM-DD format. If not provided, syncs all dates
     */
    suspend fun syncFromAppUsageEvents(date: String? = null): LeaderboardSyncResponse {
        val localDate = date?.let {
            try {
                LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid date format. Expected YYYY-MM-DD")
            }
        }
        return repository.syncFromAppUsageEvents(localDate)
    }
}

