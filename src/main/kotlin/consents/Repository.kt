package com.apptime.code.consents

import com.apptime.code.common.dbTransaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class ConsentRepository {
    
    /**
     * Get all consent templates
     */
    fun getAllConsentTemplates(): List<ConsentTemplate> {
        return dbTransaction {
            ConsentTemplates.selectAll()
                .map { row ->
                    ConsentTemplate(
                        id = row[ConsentTemplates.id],
                        name = row[ConsentTemplates.name],
                        description = row[ConsentTemplates.description],
                        isMandatory = row[ConsentTemplates.isMandatory]
                    )
                }
        }
    }
    
    /**
     * Get user's consents
     */
    fun getUserConsents(userId: String): List<ConsentSubmissionResponseItem> {
        return dbTransaction {
            UserConsents
                .join(ConsentTemplates, JoinType.INNER, UserConsents.consentId, ConsentTemplates.id)
                .select { UserConsents.userId eq userId }
                .map { row ->
                    ConsentSubmissionResponseItem(
                        id = row[UserConsents.id].toInt(),
                        consentId = row[ConsentTemplates.id],
                        consentName = row[ConsentTemplates.name],
                        value = row[UserConsents.value],
                        submittedAt = row[UserConsents.createdAt].toString()
                    )
                }
        }
    }
    
    /**
     * Submit user consents
     */
    fun submitConsents(userId: String, consents: List<ConsentSubmissionItem>): List<ConsentSubmissionResponseItem> {
        return dbTransaction {
            val results = mutableListOf<ConsentSubmissionResponseItem>()
            
            consents.forEach { consentItem ->
                // Check if consent template exists
                val template = ConsentTemplates.select { ConsentTemplates.id eq consentItem.id }
                    .firstOrNull()
                
                if (template == null) {
                    throw IllegalArgumentException("Consent template with id ${consentItem.id} not found")
                }
                
                // Validate value
                if (consentItem.value !in listOf("accepted", "rejected")) {
                    throw IllegalArgumentException("Consent value must be 'accepted' or 'rejected'")
                }
                
                // Check if consent already exists
                val existing = UserConsents.select { 
                    (UserConsents.userId eq userId) and (UserConsents.consentId eq consentItem.id)
                }.firstOrNull()
                
                val now = kotlinx.datetime.Clock.System.now()
                
                if (existing == null) {
                    // Insert new consent
                    UserConsents.insert {
                        it[UserConsents.userId] = userId
                        it[UserConsents.consentId] = consentItem.id
                        it[UserConsents.value] = consentItem.value
                        it[UserConsents.createdAt] = now
                        it[UserConsents.updatedAt] = now
                    }
                } else {
                    // Update existing consent
                    UserConsents.update({ 
                        (UserConsents.userId eq userId) and (UserConsents.consentId eq consentItem.id)
                    }) {
                        it[UserConsents.value] = consentItem.value
                        it[UserConsents.updatedAt] = now
                    }
                }
                
                // Get the final record
                val finalRecord = UserConsents
                    .join(ConsentTemplates, JoinType.INNER, UserConsents.consentId, ConsentTemplates.id)
                    .select { 
                        (UserConsents.userId eq userId) and (UserConsents.consentId eq consentItem.id)
                    }
                    .firstOrNull()
                
                finalRecord?.let { row ->
                    results.add(
                        ConsentSubmissionResponseItem(
                            id = row[UserConsents.id].toInt(),
                            consentId = row[ConsentTemplates.id],
                            consentName = row[ConsentTemplates.name],
                            value = row[UserConsents.value],
                            submittedAt = row[UserConsents.updatedAt].toString()
                        )
                    )
                }
            }
            
            results
        }
    }
    
    /**
     * Get consent status for a user
     */
    fun getConsentStatus(userId: String, consentId: Int): String? {
        return dbTransaction {
            UserConsents.select { 
                (UserConsents.userId eq userId) and (UserConsents.consentId eq consentId)
            }
            .firstOrNull()
            ?.let { it[UserConsents.value] }
        }
    }
}

