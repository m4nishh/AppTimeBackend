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
     * Get daily leaderboard with dummy data
     * @param date Optional date in YYYY-MM-DD format. If not provided, uses today's date
     * @param currentUserId Optional current user ID to determine userRank
     */
    suspend fun getDailyLeaderboard(date: String? = null, currentUserId: String? = null): LeaderboardResponse {
        // For now, return dummy data
        val periodDate = date ?: LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        
        val dummyEntries = listOf(
            LeaderboardEntry(
                userId = "user-001",
                username = "john_doe",
                name = "John Doe",
                avatar = null,
                totalScreenTime = 7200000L, // 2 hours in milliseconds
                rank = 1
            ),
            LeaderboardEntry(
                userId = "user-002",
                username = "jane_smith",
                name = "Jane Smith",
                avatar = null,
                totalScreenTime = 5400000L, // 1.5 hours
                rank = 2
            ),
            LeaderboardEntry(
                userId = "user-003",
                username = "alex_brown",
                name = "Alex Brown",
                avatar = null,
                totalScreenTime = 3600000L, // 1 hour
                rank = 3
            ),
            LeaderboardEntry(
                userId = "user-004",
                username = "sarah_wilson",
                name = "Sarah Wilson",
                avatar = null,
                totalScreenTime = 2700000L, // 45 minutes
                rank = 4
            ),
            LeaderboardEntry(
                userId = "user-005",
                username = "mike_jones",
                name = "Mike Jones",
                avatar = null,
                totalScreenTime = 1800000L, // 30 minutes
                rank = 5
            ),
            LeaderboardEntry(
                userId = "user-006",
                username = "emily_davis",
                name = "Emily Davis",
                avatar = null,
                totalScreenTime = 1500000L, // 25 minutes
                rank = 6
            ),
            LeaderboardEntry(
                userId = "user-007",
                username = "david_miller",
                name = "David Miller",
                avatar = null,
                totalScreenTime = 1200000L, // 20 minutes
                rank = 7
            ),
            LeaderboardEntry(
                userId = "user-008",
                username = "lisa_anderson",
                name = "Lisa Anderson",
                avatar = null,
                totalScreenTime = 900000L, // 15 minutes
                rank = 8
            ),
            LeaderboardEntry(
                userId = "user-009",
                username = "chris_taylor",
                name = "Chris Taylor",
                avatar = null,
                totalScreenTime = 600000L, // 10 minutes
                rank = 9
            ),
            LeaderboardEntry(
                userId = "user-010",
                username = "amanda_white",
                name = "Amanda White",
                avatar = null,
                totalScreenTime = 300000L, // 5 minutes
                rank = 10
            )
        )
        
        // Find current user's rank if userId is provided
        val userRank = currentUserId?.let { userId ->
            dummyEntries.find { it.userId == userId }?.rank
        }
        
        return LeaderboardResponse(
            period = "daily",
            periodDate = periodDate,
            entries = dummyEntries,
            userRank = userRank,
            totalUsers = dummyEntries.size
        )
    }
    
    /**
     * Get weekly leaderboard with dummy data
     * @param weekDate Optional week date in YYYY-WW format. If not provided, uses current week
     * @param currentUserId Optional current user ID to determine userRank
     */
    suspend fun getWeeklyLeaderboard(weekDate: String? = null, currentUserId: String? = null): LeaderboardResponse {
        // For now, return dummy data
        val periodDate = weekDate ?: getCurrentWeekDate()
        
        val dummyEntries = listOf(
            LeaderboardEntry(
                userId = "user-001",
                username = "john_doe",
                name = "John Doe",
                avatar = null,
                totalScreenTime = 50400000L, // 14 hours in milliseconds
                rank = 1
            ),
            LeaderboardEntry(
                userId = "user-002",
                username = "jane_smith",
                name = "Jane Smith",
                avatar = null,
                totalScreenTime = 43200000L, // 12 hours
                rank = 2
            ),
            LeaderboardEntry(
                userId = "user-003",
                username = "alex_brown",
                name = "Alex Brown",
                avatar = null,
                totalScreenTime = 36000000L, // 10 hours
                rank = 3
            ),
            LeaderboardEntry(
                userId = "user-004",
                username = "sarah_wilson",
                name = "Sarah Wilson",
                avatar = null,
                totalScreenTime = 28800000L, // 8 hours
                rank = 4
            ),
            LeaderboardEntry(
                userId = "user-005",
                username = "mike_jones",
                name = "Mike Jones",
                avatar = null,
                totalScreenTime = 25200000L, // 7 hours
                rank = 5
            ),
            LeaderboardEntry(
                userId = "user-006",
                username = "emily_davis",
                name = "Emily Davis",
                avatar = null,
                totalScreenTime = 21600000L, // 6 hours
                rank = 6
            ),
            LeaderboardEntry(
                userId = "user-007",
                username = "david_miller",
                name = "David Miller",
                avatar = null,
                totalScreenTime = 18000000L, // 5 hours
                rank = 7
            ),
            LeaderboardEntry(
                userId = "user-008",
                username = "lisa_anderson",
                name = "Lisa Anderson",
                avatar = null,
                totalScreenTime = 14400000L, // 4 hours
                rank = 8
            ),
            LeaderboardEntry(
                userId = "user-009",
                username = "chris_taylor",
                name = "Chris Taylor",
                avatar = null,
                totalScreenTime = 10800000L, // 3 hours
                rank = 9
            ),
            LeaderboardEntry(
                userId = "user-010",
                username = "amanda_white",
                name = "Amanda White",
                avatar = null,
                totalScreenTime = 7200000L, // 2 hours
                rank = 10
            )
        )
        
        // Find current user's rank if userId is provided
        val userRank = currentUserId?.let { userId ->
            dummyEntries.find { it.userId == userId }?.rank
        }
        
        return LeaderboardResponse(
            period = "weekly",
            periodDate = periodDate,
            entries = dummyEntries,
            userRank = userRank,
            totalUsers = dummyEntries.size
        )
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
}

