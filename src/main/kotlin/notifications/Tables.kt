package com.apptime.code.notifications

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Notifications table
 */
object Notifications : Table("notifications") {
    val id = long("id").autoIncrement()
    val userId = varchar("user_id", 255).index()
    val title = varchar("title", 255)
    val image = text("image").nullable()
    val text = text("text")
    val deeplink = text("deeplink").nullable()
    val type = varchar("type", 50).nullable() // "daily_limit", "break_reminder", etc.
    val isRead = bool("is_read").default(false)
    
    val createdAt = timestamp("created_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    val readAt = timestamp("read_at").nullable()
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(isUnique = false, userId, isRead)
    }
}

