package com.apptime.code.leaderboard

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntry(
    val userId: String,
    val username: String? = null,
    val name: String? = null,
    val avatar: String? = null,
    val totalScreenTime: Long, // milliseconds
    val rank: Int
)

@Serializable
data class LeaderboardResponse(
    val period: String, // "daily", "weekly", "monthly"
    val periodDate: String,
    val entries: List<LeaderboardEntry>,
    val userRank: Int? = null,
    val totalUsers: Int
)

