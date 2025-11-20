package com.apptime.code.challenges

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Challenges table - stores challenge information
 */
object Challenges : Table("challenges") {
    val id = long("id").autoIncrement()
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val reward = varchar("reward", 255).nullable()
    val startTime = timestamp("start_time")
    val endTime = timestamp("end_time")
    val thumbnail = varchar("thumbnail", 500).nullable()
    val challengeType = varchar("challenge_type", 50) // "LESS_SCREENTIME" or "MORE_SCREENTIME"
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(isUnique = false, isActive, startTime)
        index(isUnique = false, challengeType)
    }
}

/**
 * Challenge participants table - tracks which users joined which challenges
 */
object ChallengeParticipants : Table("challenge_participants") {
    val id = long("id").autoIncrement()
    val challengeId = long("challenge_id").references(Challenges.id, onDelete = org.jetbrains.exposed.sql.ReferenceOption.CASCADE)
    val userId = varchar("user_id", 255)
    val joinedAt = timestamp("joined_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(isUnique = false, challengeId)
        index(isUnique = false, userId)
        // Ensure a user can only join a challenge once
        uniqueIndex(challengeId, userId)
    }
}

/**
 * Challenge participant stats table - tracks app usage for challenge participants
 * Stores individual app usage sessions during challenge participation
 */
object ChallengeParticipantStats : Table("challenge_participant_stats") {
    val id = long("id").autoIncrement()
    val challengeId = long("challenge_id").references(Challenges.id, onDelete = org.jetbrains.exposed.sql.ReferenceOption.CASCADE)
    val userId = varchar("user_id", 255)
    val appName = varchar("app_name", 255)
    val packageName = varchar("package_name", 255)
    val startSyncTime = timestamp("start_sync_time")
    val endSyncTime = timestamp("end_sync_time")
    val duration = long("duration") // milliseconds
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(isUnique = false, challengeId, userId)
        index(isUnique = false, userId, startSyncTime)
        index(isUnique = false, packageName)
    }
}

