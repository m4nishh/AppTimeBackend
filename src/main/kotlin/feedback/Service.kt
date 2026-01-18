package feedback

import com.apptime.code.feedback.*

/**
 * Service for feedback business logic
 */
class FeedbackService(
    private val repository: FeedbackRepository
) {
    
    /**
     * Submit feedback
     */
    suspend fun submitFeedback(
        userId: String,
        request: FeedbackSubmissionRequest
    ): FeedbackSubmissionResponse {
        
        // Validate message
        if (request.message.isBlank()) {
            throw IllegalArgumentException("Message cannot be empty")
        }
        

        
        // Create feedback
        val feedbackId = repository.createFeedback(
            userId = userId,
            message = request.message.trim()
        )
        
        val feedback = repository.getFeedbackById(feedbackId, userId)!!
        
        return FeedbackSubmissionResponse(
            message = "Feedback submitted successfully",
        )
    }
    
    /**
     * Get user's feedback
     */
    suspend fun getUserFeedback(
        userId: String,
        limit: Int? = null,
        offset: Int = 0
    ): FeedbackListResponse {
        val feedbacks = repository.getUserFeedback(userId, limit, offset)
        val totalCount = repository.getFeedbackCount(userId)
        
        return FeedbackListResponse(
            feedbacks = feedbacks,
            totalCount = totalCount
        )
    }
    
    /**
     * Get feedback by ID
     */
    suspend fun getFeedbackById(feedbackId: Long, userId: String? = null): FeedbackResponse? {
        return repository.getFeedbackById(feedbackId, userId)
    }
    
    /**
     * Get all feedback (admin)
     */
    suspend fun getAllFeedback(
        status: String? = null,
        category: String? = null,
        limit: Int? = null,
        offset: Int = 0
    ): FeedbackListResponse {
        val feedbacks = repository.getAllFeedback(status, category, limit, offset)
        val totalCount = repository.getFeedbackCount(status = status)
        
        return FeedbackListResponse(
            feedbacks = feedbacks,
            totalCount = totalCount
        )
    }
    
    /**
     * Update feedback status (admin)
     */
    suspend fun updateFeedbackStatus(
        feedbackId: Long,
        request: UpdateFeedbackStatusRequest
    ): FeedbackResponse {
        val validStatuses = listOf("pending", "reviewed", "resolved", "closed")
        if (request.status !in validStatuses) {
            throw IllegalArgumentException("Invalid status. Must be one of: ${validStatuses.joinToString(", ")}")
        }
        
        val success = repository.updateFeedbackStatus(
            feedbackId = feedbackId,
            status = request.status,
            adminNotes = request.adminNotes
        )
        
        if (!success) {
            throw IllegalArgumentException("Feedback not found")
        }
        
        return repository.getFeedbackById(feedbackId)!!
    }
    
    /**
     * Delete feedback
     */
    suspend fun deleteFeedback(feedbackId: Long, userId: String? = null): Boolean {
        return repository.deleteFeedback(feedbackId, userId)
    }
}


