package com.apptime.code.admin

import kotlinx.datetime.Clock
import java.util.*

class AdminService(private val repository: StatsRepository) {
    
    fun getAdminStats(): AdminStatsResponse {
        val userStats = repository.getUserStats()
        val challengeStats = repository.getChallengeStats()
        val usageStats = repository.getUsageStats()
        val focusStats = repository.getFocusStats()
        val leaderboardStats = repository.getLeaderboardStats()
        
        val systemStats = SystemStats(
            databaseConnected = true,
            serverTime = Clock.System.now().toString(),
            timezone = TimeZone.getDefault().id
        )
        
        return AdminStatsResponse(
            users = userStats,
            challenges = challengeStats,
            usage = usageStats,
            focus = focusStats,
            leaderboard = leaderboardStats,
            system = systemStats
        )
    }
    
    fun getUsersWithPagination(
        username: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): PaginatedUserStats {
        // Validate pagination parameters
        val validPage = if (page < 1) 1 else page
        val validPageSize = when {
            pageSize < 1 -> 20
            pageSize > 100 -> 100
            else -> pageSize
        }
        
        return repository.getUsersWithPagination(username, validPage, validPageSize)
    }
}

