package com.apptime.code.focus

import kotlinx.serialization.Serializable

@Serializable
data class FocusDurationSubmissionRequest(
    val focusDuration: Long, // milliseconds
    val startTime: String, // ISO 8601 format
    val endTime: String,   // ISO 8601 format
    val sessionType: String? = null // "work", "study", "break"
)

@Serializable
data class FocusDurationSubmissionArrayRequest(
    val sessions: List<FocusDurationSubmissionRequest>
)

@Serializable
data class FocusDurationSubmissionResponse(
    val submittedSessions: List<FocusDurationHistoryItem>,
    val totalSubmitted: Int,
    val totalFocusTime: Long
)

@Serializable
data class FocusDurationHistoryRequest(
    val startDate: String,
    val endDate: String
)

@Serializable
data class FocusDurationHistoryItem(
    val id: Long? = null,
    val focusDuration: Long,
    val startTime: String,
    val endTime: String? = null,
    val sessionType: String? = null,
    val createdAt: String? = null
)

@Serializable
data class FocusDurationHistoryResponse(
    val focusSessions: List<FocusDurationHistoryItem>,
    val totalFocusTime: Long,
    val averageSessionDuration: Long
)

@Serializable
data class FocusDurationStatsResponse(
    val totalFocusTime: Long,
    val todayFocusTime: Long,
    val weeklyFocusTime: Long,
    val monthlyFocusTime: Long,
    val averageSessionDuration: Long,
    val totalSessions: Int
)

