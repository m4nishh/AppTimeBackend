package com.apptime.code.focus

import kotlinx.serialization.Serializable

/**
 * Request to submit focus mode stats
 */
@Serializable
data class FocusModeStatsRequest(
    val startTime: Long, // milliseconds
    val endTime: Long    // milliseconds
)

/**
 * Response after submitting focus mode stats
 */
@Serializable
data class FocusModeStatsResponse(
    val id: Long,
    val userId: String,
    val startTime: Long, // milliseconds
    val endTime: Long,   // milliseconds
    val duration: Long,  // milliseconds (endTime - startTime)
    val createdAt: String // ISO 8601 format
)

/**
 * Response for GET request with time-based filtering
 */
@Serializable
data class FocusModeStatsListResponse(
    val stats: List<FocusModeStatsItem>,
    val totalCount: Int,
    val totalDuration: Long // milliseconds
)

/**
 * Individual focus mode stats item
 */
@Serializable
data class FocusModeStatsItem(
    val id: Long,
    val startTime: Long, // milliseconds
    val endTime: Long,   // milliseconds
    val duration: Long,  // milliseconds
    val createdAt: String // ISO 8601 format
)

