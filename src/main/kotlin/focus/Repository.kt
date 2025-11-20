package com.apptime.code.focus

import com.apptime.code.common.dbTransaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlinx.datetime.Instant

class FocusRepository {
    
    /**
     * Submit a focus session
     */
    fun submitFocusSession(
        userId: String,
        focusDuration: Long,
        startTime: Instant,
        endTime: Instant?,
        sessionType: String?
    ): FocusDurationHistoryItem {
        return dbTransaction {
            FocusSessions.insert {
                it[FocusSessions.userId] = userId
                it[FocusSessions.focusDuration] = focusDuration
                it[FocusSessions.startTime] = startTime
                it[FocusSessions.endTime] = endTime
                it[FocusSessions.sessionType] = sessionType
                it[FocusSessions.completed] = endTime != null
            }
            
            // Get the inserted record (most recent for this user)
            val record = FocusSessions.select { FocusSessions.userId eq userId }
                .orderBy(FocusSessions.id to SortOrder.DESC)
                .first()
            
            FocusDurationHistoryItem(
                id = record[FocusSessions.id],
                focusDuration = record[FocusSessions.focusDuration],
                startTime = record[FocusSessions.startTime].toString(),
                endTime = record[FocusSessions.endTime]?.toString(),
                sessionType = record[FocusSessions.sessionType],
                createdAt = record[FocusSessions.createdAt].toString()
            )
        }
    }
    
    /**
     * Get focus history for a user within date range
     */
    fun getFocusHistory(
        userId: String,
        startDate: Instant,
        endDate: Instant
    ): FocusDurationHistoryResponse {
        return dbTransaction {
            val sessions = FocusSessions.select {
                (FocusSessions.userId eq userId) and
                (FocusSessions.startTime greaterEq startDate) and
                (FocusSessions.startTime lessEq endDate)
            }
            .orderBy(FocusSessions.startTime to SortOrder.DESC)
            .map { row ->
                FocusDurationHistoryItem(
                    id = row[FocusSessions.id],
                    focusDuration = row[FocusSessions.focusDuration],
                    startTime = row[FocusSessions.startTime].toString(),
                    endTime = row[FocusSessions.endTime]?.toString(),
                    sessionType = row[FocusSessions.sessionType],
                    createdAt = row[FocusSessions.createdAt].toString()
                )
            }
            
            val totalFocusTime = sessions.sumOf { it.focusDuration }
            val averageSessionDuration = if (sessions.isNotEmpty()) {
                totalFocusTime / sessions.size
            } else {
                0L
            }
            
            FocusDurationHistoryResponse(
                focusSessions = sessions,
                totalFocusTime = totalFocusTime,
                averageSessionDuration = averageSessionDuration
            )
        }
    }
    
    /**
     * Get all focus sessions for a user (no date filter)
     */
    fun getAllFocusSessions(userId: String): List<FocusDurationHistoryItem> {
        return dbTransaction {
            FocusSessions.select { FocusSessions.userId eq userId }
                .orderBy(FocusSessions.startTime to SortOrder.DESC)
                .map { row ->
                    FocusDurationHistoryItem(
                        id = row[FocusSessions.id],
                        focusDuration = row[FocusSessions.focusDuration],
                        startTime = row[FocusSessions.startTime].toString(),
                        endTime = row[FocusSessions.endTime]?.toString(),
                        sessionType = row[FocusSessions.sessionType],
                        createdAt = row[FocusSessions.createdAt].toString()
                    )
                }
        }
    }
    
    /**
     * Get focus stats for a user
     */
    fun getFocusStats(userId: String): FocusDurationStatsResponse {
        return dbTransaction {
            val allSessions = FocusSessions.select { FocusSessions.userId eq userId }
                .map { row ->
                    FocusDurationHistoryItem(
                        id = row[FocusSessions.id],
                        focusDuration = row[FocusSessions.focusDuration],
                        startTime = row[FocusSessions.startTime].toString(),
                        endTime = row[FocusSessions.endTime]?.toString(),
                        sessionType = row[FocusSessions.sessionType],
                        createdAt = row[FocusSessions.createdAt].toString()
                    )
                }
            
            val totalFocusTime = allSessions.sumOf { it.focusDuration }
            val totalSessions = allSessions.size
            val averageSessionDuration = if (totalSessions > 0) {
                totalFocusTime / totalSessions
            } else {
                0L
            }
            
            // Calculate today's focus time
            val today = kotlinx.datetime.Clock.System.now()
            val todayStart = kotlinx.datetime.Instant.fromEpochMilliseconds(
                today.toEpochMilliseconds() - (today.toEpochMilliseconds() % (24 * 60 * 60 * 1000))
            )
            val todaySessions = allSessions.filter {
                kotlinx.datetime.Instant.parse(it.startTime) >= todayStart
            }
            val todayFocusTime = todaySessions.sumOf { it.focusDuration }
            
            // Calculate weekly focus time (last 7 days)
            val weekAgo = kotlinx.datetime.Instant.fromEpochMilliseconds(
                today.toEpochMilliseconds() - (7 * 24 * 60 * 60 * 1000)
            )
            val weeklySessions = allSessions.filter {
                kotlinx.datetime.Instant.parse(it.startTime) >= weekAgo
            }
            val weeklyFocusTime = weeklySessions.sumOf { it.focusDuration }
            
            // Calculate monthly focus time (last 30 days)
            val monthAgo = kotlinx.datetime.Instant.fromEpochMilliseconds(
                today.toEpochMilliseconds() - (30 * 24 * 60 * 60 * 1000)
            )
            val monthlySessions = allSessions.filter {
                kotlinx.datetime.Instant.parse(it.startTime) >= monthAgo
            }
            val monthlyFocusTime = monthlySessions.sumOf { it.focusDuration }
            
            FocusDurationStatsResponse(
                totalFocusTime = totalFocusTime,
                todayFocusTime = todayFocusTime,
                weeklyFocusTime = weeklyFocusTime,
                monthlyFocusTime = monthlyFocusTime,
                averageSessionDuration = averageSessionDuration,
                totalSessions = totalSessions
            )
        }
    }
}

