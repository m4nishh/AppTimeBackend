package com.apptime.code.feedback

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackSubmissionRequest(
    val message: String
)

@Serializable
data class FeedbackResponse(
    val id: Long,
    val userId: String,
    val message: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class FeedbackSubmissionResponse(
    val message: String
)

@Serializable
data class FeedbackListResponse(
    val feedbacks: List<FeedbackResponse>,
    val totalCount: Int
)

@Serializable
data class UpdateFeedbackStatusRequest(
    val status: String, // "pending", "reviewed", "resolved", "closed"
    val adminNotes: String? = null
)


