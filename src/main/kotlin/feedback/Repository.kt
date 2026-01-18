package com.apptime.code.feedback

import com.apptime.code.common.dbTransaction
import feedback.Feedback
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlinx.datetime.Clock

/**
 * Repository for feedback database operations
 */
class FeedbackRepository {
    
    /**
     * Create a feedback submission
     */
    fun createFeedback(
        userId: String,
        message: String,

    ): Long {
        return dbTransaction {
            Feedback.insert {
                it[Feedback.userId] = userId
                it[Feedback.message] = message
                it[Feedback.createdAt] = Clock.System.now()
                it[Feedback.updatedAt] = Clock.System.now()
            }[Feedback.id]
        }
    }
    
    /**
     * Get feedback by ID
     */
    fun getFeedbackById(feedbackId: Long, userId: String? = null): FeedbackResponse? {
        return dbTransaction {
            val query = if (userId != null) {
                Feedback.select {
                    (Feedback.id eq feedbackId) and (Feedback.userId eq userId)
                }
            } else {
                Feedback.select { Feedback.id eq feedbackId }
            }
            
            query.firstOrNull()?.let { row ->
                FeedbackResponse(
                    id = row[Feedback.id],
                    userId = row[Feedback.userId],
                    message = row[Feedback.message],
                    createdAt = row[Feedback.createdAt].toString(),
                    updatedAt = row[Feedback.updatedAt].toString()
                )
            }
        }
    }
    
    /**
     * Get user's feedback submissions
     */
    fun getUserFeedback(
        userId: String,
        limit: Int? = null,
        offset: Int = 0
    ): List<FeedbackResponse> {
        return dbTransaction {
            var query = Feedback.select {
                Feedback.userId eq userId
            }.orderBy(Feedback.createdAt to SortOrder.DESC)
            
            if (limit != null) {
                query = query.limit(limit, offset.toLong())
            }
            
            query.map { row ->
                FeedbackResponse(
                    id = row[Feedback.id],
                    userId = row[Feedback.userId],

                    message = row[Feedback.message],

                    createdAt = row[Feedback.createdAt].toString(),
                    updatedAt = row[Feedback.updatedAt].toString()
                )
            }
        }
    }
    
    /**
     * Get all feedback (admin)
     */
    fun getAllFeedback(
        status: String? = null,
        category: String? = null,
        limit: Int? = null,
        offset: Int = 0
    ): List<FeedbackResponse> {
        return dbTransaction {
            var query = Feedback.selectAll()
            

            
            query = query.orderBy(Feedback.createdAt to SortOrder.DESC)
            
            if (limit != null) {
                query = query.limit(limit, offset.toLong())
            }
            
            query.map { row ->
                FeedbackResponse(
                    id = row[Feedback.id],
                    userId = row[Feedback.userId],

                    message = row[Feedback.message],

                    createdAt = row[Feedback.createdAt].toString(),
                    updatedAt = row[Feedback.updatedAt].toString()
                )
            }
        }
    }
    
    /**
     * Get feedback count
     */
    fun getFeedbackCount(userId: String? = null, status: String? = null): Int {
        return dbTransaction {
            var query = Feedback.selectAll()
            
            if (userId != null) {
                query = query.andWhere { Feedback.userId eq userId }
            }

            query.count().toInt()
        }
    }
    
    /**
     * Update feedback status (admin)
     */
    fun updateFeedbackStatus(
        feedbackId: Long,
        status: String,
        adminNotes: String? = null
    ): Boolean {
        return dbTransaction {
            val updated = Feedback.update(
                where = { Feedback.id eq feedbackId }
            ) {

                it[Feedback.updatedAt] = Clock.System.now()
            }
            updated > 0
        }
    }
    
    /**
     * Delete feedback
     */
    fun deleteFeedback(feedbackId: Long, userId: String? = null): Boolean {
        return dbTransaction {
            val deleted = if (userId != null) {
                Feedback.deleteWhere {
                    (Feedback.id eq feedbackId) and (Feedback.userId eq userId)
                }
            } else {
                Feedback.deleteWhere { Feedback.id eq feedbackId }
            }
            deleted > 0
        }
    }
}


