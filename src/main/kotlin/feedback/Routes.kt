package com.apptime.code.feedback

import com.apptime.code.common.MessageKeys
import com.apptime.code.common.respondApi
import com.apptime.code.common.respondError
import com.apptime.code.common.requireUserId
import feedback.FeedbackService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

/**
 * Configure feedback routes
 */
fun Application.configureFeedbackRoutes() {
    val repository = FeedbackRepository()
    val service = FeedbackService(repository)
    
    routing {
        route("/api/feedback") {
            authenticate("auth-bearer") {
                /**
                 * Submit feedback
                 * POST /api/feedback
                 */
                post {
                    try {
                        val userId = call.requireUserId()
                        val request = call.receive<FeedbackSubmissionRequest>()
                        
                        val response = service.submitFeedback(userId, request)
                        call.respondApi(response, statusCode = HttpStatusCode.Created, messageKey = MessageKeys.FEEDBACK_SUBMITTED)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.FEEDBACK_SUBMIT_FAILED, message = "Failed to submit feedback: ${e.message}")
                    }
                }
                
                /**
                 * Get user's feedback
                 * GET /api/feedback?limit=20&offset=0
                 */
                get {
                    try {
                        val userId = call.requireUserId()
                        val limit = call.request.queryParameters["limit"]?.toInt()
                        val offset = call.request.queryParameters["offset"]?.toInt() ?: 0
                        
                        val response = service.getUserFeedback(userId, limit, offset)
                        call.respondApi(response, messageKey = MessageKeys.FEEDBACK_RETRIEVED)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.FEEDBACK_RETRIEVAL_FAILED, message = "Failed to retrieve feedback: ${e.message}")
                    }
                }
                
                /**
                 * Get feedback by ID
                 * GET /api/feedback/{feedbackId}
                 */
                get("/{feedbackId}") {
                    try {
                        val userId = call.requireUserId()
                        val feedbackId = call.parameters["feedbackId"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid feedback ID")
                        
                        val feedback = service.getFeedbackById(feedbackId, userId)
                        if (feedback != null) {
                            call.respondApi(feedback, messageKey = MessageKeys.FEEDBACK_RETRIEVED)
                        } else {
                            call.respondError(HttpStatusCode.NotFound, messageKey = MessageKeys.FEEDBACK_NOT_FOUND, message = "Feedback not found")
                        }
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.FEEDBACK_RETRIEVAL_FAILED, message = "Failed to retrieve feedback: ${e.message}")
                    }
                }
                
                /**
                 * Delete feedback
                 * DELETE /api/feedback/{feedbackId}
                 */
                delete("/{feedbackId}") {
                    try {
                        val userId = call.requireUserId()
                        val feedbackId = call.parameters["feedbackId"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid feedback ID")
                        
                        val success = service.deleteFeedback(feedbackId, userId)
                        if (success) {
                            call.respondApi(
                                mapOf(
                                    "message" to "Feedback deleted successfully",
                                    "feedbackId" to feedbackId
                                ),
                                messageKey = MessageKeys.FEEDBACK_DELETED
                            )
                        } else {
                            call.respondError(HttpStatusCode.NotFound, messageKey = MessageKeys.FEEDBACK_NOT_FOUND, message = "Feedback not found")
                        }
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.FEEDBACK_DELETE_FAILED, message = "Failed to delete feedback: ${e.message}")
                    }
                }
            }
        }
    }
}

