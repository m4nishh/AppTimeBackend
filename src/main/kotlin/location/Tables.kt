package com.apptime.code.location

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * User locations table - stores location sync data from Android devices
 */
object UserLocations : Table("user_locations") {
    val id = long("id").autoIncrement()
    val userId = varchar("user_id", 255).uniqueIndex() // One location record per user
    val ipAddress = varchar("ip_address", 45).nullable() // IPv4 or IPv6
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
    val address = text("address").nullable() // Address derived from latlong
    val lastSyncTime = timestamp("last_sync_time")
    
    val createdAt = timestamp("created_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(isUnique = false, userId, lastSyncTime)
    }
}
