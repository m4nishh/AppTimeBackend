package com.apptime.code.focus

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Focus sessions table - stores focus mode tracking data
 */
object FocusSessions : Table("focus_sessions") {
    val id = long("id").autoIncrement()
    val userId = varchar("user_id", 255).index()
    val focusDuration = long("focus_duration") // milliseconds
    val startTime = timestamp("start_time")
    val endTime = timestamp("end_time").nullable()
    val sessionType = varchar("session_type", 50).nullable() // "work", "study", "break", etc.
    val completed = bool("completed").default(false)
    val countdownMode = bool("countdown_mode").default(false)
    val countdownDuration = long("countdown_duration").default(0L)
    
    val createdAt = timestamp("created_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(isUnique = false, userId, startTime)
    }
}

