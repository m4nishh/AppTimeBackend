package com.apptime.code.focus

import com.apptime.code.leaderboard.LeaderboardRepository
import kotlinx.datetime.Instant

/**
 * Focus service layer - handles business logic
 */
class FocusService(
    private val repository: FocusRepository,
    private val leaderboardRepository: LeaderboardRepository
) {
    
    /**
     * Submit multiple focus sessions (array)
     */
    suspend fun submitFocusSessions(
        userId: String,
        requests: List<FocusDurationSubmissionRequest>
    ): FocusDurationSubmissionResponse {
        if (requests.isEmpty()) {
            throw IllegalArgumentException("At least one focus session is required")
        }
        
        val submittedSessions = mutableListOf<FocusDurationHistoryItem>()
        var totalFocusTime = 0L
        
        for (request in requests) {
            val result = submitFocusSession(userId, request)
            submittedSessions.add(result)
            totalFocusTime += result.focusDuration
        }
        
        return FocusDurationSubmissionResponse(
            submittedSessions = submittedSessions,
            totalSubmitted = submittedSessions.size,
            totalFocusTime = totalFocusTime
        )
    }
    
    /**
     * Submit a single focus session
     */
    private suspend fun submitFocusSession(
        userId: String,
        request: FocusDurationSubmissionRequest
    ): FocusDurationHistoryItem {
        // Validate request
        if (request.focusDuration <= 0) {
            throw IllegalArgumentException("Focus duration must be greater than 0")
        }
        
        // Parse timestamps
        val startTime = try {
            Instant.parse(request.startTime)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid startTime format. Use ISO 8601 format (e.g., 2024-01-15T10:00:00Z)")
        }
        
        val endTime = try {
            if (request.endTime.isNotBlank()) {
                Instant.parse(request.endTime)
            } else {
                null
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid endTime format. Use ISO 8601 format (e.g., 2024-01-15T10:30:00Z)")
        }
        
        // Validate endTime is after startTime
        if (endTime != null && endTime <= startTime) {
            throw IllegalArgumentException("endTime must be after startTime")
        }
        
        // Validate duration matches time difference
        if (endTime != null) {
            val calculatedDuration = endTime.toEpochMilliseconds() - startTime.toEpochMilliseconds()
            if (kotlin.math.abs(calculatedDuration - request.focusDuration) > 1000) { // Allow 1 second difference
                throw IllegalArgumentException("focusDuration does not match the difference between startTime and endTime")
            }
        }
        
        // Submit focus session
        val result = repository.submitFocusSession(
            userId = userId,
            focusDuration = request.focusDuration,
            startTime = startTime,
            endTime = endTime,
            sessionType = request.sessionType
        )
        
        // Update leaderboard with focus time
        if (endTime != null) {
            val date = java.time.Instant.ofEpochMilli(startTime.toEpochMilliseconds())
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            
            leaderboardRepository.updateLeaderboardWithFocusTime(
                userId = userId,
                focusDuration = request.focusDuration,
                date = date
            )
        }
        
        return result
    }
    
    /**
     * Get focus history for a user
     * If startDate and endDate are provided, filter by time range
     * If not provided, return all sessions
     */
    suspend fun getFocusHistory(
        userId: String,
        startDate: String?,
        endDate: String?
    ): FocusDurationHistoryResponse {
        // If no dates provided, return all sessions
        if (startDate == null && endDate == null) {
            val allSessions = repository.getAllFocusSessions(userId)
            val totalFocusTime = allSessions.sumOf { it.focusDuration }
            val averageSessionDuration = if (allSessions.isNotEmpty()) {
                totalFocusTime / allSessions.size
            } else {
                0L
            }
            
            return FocusDurationHistoryResponse(
                focusSessions = allSessions,
                totalFocusTime = totalFocusTime,
                averageSessionDuration = averageSessionDuration
            )
        }
        
        // Parse dates if provided
        val start = if (startDate != null) {
            try {
                Instant.parse(startDate)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid startDate format. Use ISO 8601 format")
            }
        } else {
            // If only endDate is provided, start from beginning of time
            kotlinx.datetime.Instant.fromEpochMilliseconds(0)
        }
        
        val end = if (endDate != null) {
            try {
                Instant.parse(endDate)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid endDate format. Use ISO 8601 format")
            }
        } else {
            // If only startDate is provided, end at current time
            kotlinx.datetime.Clock.System.now()
        }
        
        if (end < start) {
            throw IllegalArgumentException("endDate must be after startDate")
        }
        
        return repository.getFocusHistory(userId, start, end)
    }
    
    /**
     * Get all focus sessions for a user
     */
    suspend fun getAllFocusSessions(userId: String): List<FocusDurationHistoryItem> {
        return repository.getAllFocusSessions(userId)
    }
    
    /**
     * Get focus stats for a user
     */
    suspend fun getFocusStats(userId: String): FocusDurationStatsResponse {
        return repository.getFocusStats(userId)
    }
}

