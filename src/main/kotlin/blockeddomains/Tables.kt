package com.apptime.code.blockeddomains

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Blocked domain groups table
 */
object BlockedDomainGroups : Table("blocked_domain_groups") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100) // "adults", "social media", "gaming"
    val description = text("description").nullable()
    
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
}

/**
 * Blocked domains table
 */
object BlockedDomains : Table("blocked_domains") {
    val id = integer("id").autoIncrement()
    val domain = varchar("domain", 255).uniqueIndex()
    val groupId = integer("group_id").nullable()
    
    init {
        foreignKey(groupId to BlockedDomainGroups.id, onDelete = org.jetbrains.exposed.sql.ReferenceOption.SET_NULL)
    }
    val isActive = bool("is_active").default(true)
    
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
}

