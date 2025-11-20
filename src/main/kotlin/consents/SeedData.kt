package com.apptime.code.consents

import com.apptime.code.common.dbTransaction
import org.jetbrains.exposed.sql.*

/**
 * Seed initial consent templates data
 */
object ConsentSeedData {
    
    fun seedConsentTemplates() {
        dbTransaction {
            // Check if templates already exist
            val existingCount = ConsentTemplates.selectAll().count()
            
            if (existingCount > 0) {
                println("✅ Consent templates already exist ($existingCount templates). Skipping seed.")
                return@dbTransaction
            }
            
            // Insert default consent templates
            ConsentTemplates.insert {
                it[name] = "Data Sharing"
                it[description] = "Allow sharing of usage data for analytics and improving the app experience"
                it[isMandatory] = false
            }
            
            ConsentTemplates.insert {
                it[name] = "Analytics"
                it[description] = "Allow collection of analytics data to understand app usage patterns"
                it[isMandatory] = false
            }
            
            ConsentTemplates.insert {
                it[name] = "Marketing Communications"
                it[description] = "Receive marketing emails and notifications about new features"
                it[isMandatory] = false
            }
            
            ConsentTemplates.insert {
                it[name] = "Leaderboard Participation"
                it[description] = "Allow your usage data to be included in public leaderboards"
                it[isMandatory] = false
            }
            
            ConsentTemplates.insert {
                it[name] = "Terms of Service"
                it[description] = "Acceptance of terms of service and privacy policy"
                it[isMandatory] = true
            }
            
            println("✅ Seeded ${ConsentTemplates.selectAll().count()} consent templates")
        }
    }
}

