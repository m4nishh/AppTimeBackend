package com.apptime.code.consents

/**
 * Consent service layer - handles business logic
 */
class ConsentService(private val repository: ConsentRepository) {
    
    /**
     * Get all available consent templates
     */
    suspend fun getConsentTemplates(): List<ConsentTemplate> {
        return repository.getAllConsentTemplates()
    }
    
    /**
     * Get user's submitted consents
     */
    suspend fun getUserConsents(userId: String): List<ConsentSubmissionResponseItem> {
        if (userId.isBlank()) {
            throw IllegalArgumentException("User ID is required")
        }
        
        return repository.getUserConsents(userId)
    }
    
    /**
     * Submit user consents
     */
    suspend fun submitConsents(userId: String, request: ConsentSubmissionRequest): List<ConsentSubmissionResponseItem> {
        if (userId.isBlank()) {
            throw IllegalArgumentException("User ID is required")
        }
        
        if (request.consents.isEmpty()) {
            throw IllegalArgumentException("At least one consent is required")
        }
        
        return repository.submitConsents(userId, request.consents)
    }
}

