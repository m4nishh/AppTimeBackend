package com.apptime.code.leaderboard

import com.apptime.code.common.dbTransaction
import com.apptime.code.users.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate
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
}

