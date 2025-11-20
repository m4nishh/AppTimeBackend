package com.apptime.code.focus

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Focus Mode Stats table - stores focus mode statistics
 * startTime and endTime are stored as milliseconds (Long)
 */
object FocusModeStats : Table("focus_mode_stats") {
    val id = long("id").autoIncrement()
    val userId = varchar("user_id", 255).index()
    val startTime = long("start_time") // milliseconds
    val endTime = long("end_time") // milliseconds
    val createdAt = timestamp("created_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        // Index for querying by user and time range
        index(isUnique = false, userId, startTime)
        index(isUnique = false, userId, endTime)
    }
}

