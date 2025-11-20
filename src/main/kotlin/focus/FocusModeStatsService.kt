package com.apptime.code.focus

/**
 * Service for focus mode stats operations
 */
class FocusModeStatsService(
    private val repository: FocusModeStatsRepository
) {
    
    /**
     * Save focus mode stats
     */
    suspend fun saveFocusModeStats(
        userId: String,
        startTime: Long,
        endTime: Long
    ): FocusModeStatsResponse {
        // Validate inputs
        if (startTime <= 0) {
            throw IllegalArgumentException("startTime must be greater than 0")
        }
        
        if (endTime <= 0) {
            throw IllegalArgumentException("endTime must be greater than 0")
        }
        
        if (endTime <= startTime) {
            throw IllegalArgumentException("endTime must be greater than startTime")
        }
        
        return repository.saveFocusModeStats(userId, startTime, endTime)
    }
    
    /**
     * Get focus mode stats with optional time-based filtering
     */
    suspend fun getFocusModeStats(
        userId: String,
        startTimeMs: Long? = null,
        endTimeMs: Long? = null
    ): FocusModeStatsListResponse {
        // Validate time filters if provided
        if (startTimeMs != null && startTimeMs <= 0) {
            throw IllegalArgumentException("startTimeMs must be greater than 0")
        }
        
        if (endTimeMs != null && endTimeMs <= 0) {
            throw IllegalArgumentException("endTimeMs must be greater than 0")
        }
        
        if (startTimeMs != null && endTimeMs != null && endTimeMs < startTimeMs) {
            throw IllegalArgumentException("endTimeMs must be greater than or equal to startTimeMs")
        }
        
        return repository.getFocusModeStats(userId, startTimeMs, endTimeMs)
    }
}

