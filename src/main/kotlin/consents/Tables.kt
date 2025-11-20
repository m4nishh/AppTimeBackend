package com.apptime.code.consents

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Consent templates table - stores available consent types
 */
object ConsentTemplates : Table("consent_templates") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val isMandatory = bool("is_mandatory").default(false)
    
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
}

/**
 * User consents table - stores user consent records
 */
object UserConsents : Table("user_consents") {
    val id = long("id").autoIncrement()
    val userId = varchar("user_id", 255).index()
    val consentId = integer("consent_id").references(ConsentTemplates.id)
    val value = varchar("value", 50) // "accepted" or "rejected"
    
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        uniqueIndex(userId, consentId)
    }
}

