package com.apptime.code.location

import com.apptime.code.common.dbTransaction
import com.apptime.code.location.UserLocations
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlinx.datetime.Instant

class LocationRepository {

    /**
     * Upsert location data for a user
     * If location exists, update it; otherwise, insert new record
     */
    fun upsertLocation(
        userId: String,
        ipAddress: String?,
        latitude: Double?,
        longitude: Double?,
        address: String?,
        lastSyncTime: Instant
    ) {
        dbTransaction {
            // Check if location exists for this user
            val existingLocation = UserLocations.select {
                UserLocations.userId eq userId
            }.firstOrNull()

            if (existingLocation != null) {
                // Update existing location
                UserLocations.update({ UserLocations.userId eq userId }) {
                    it[UserLocations.ipAddress] = ipAddress
                    it[UserLocations.latitude] = latitude
                    it[UserLocations.longitude] = longitude
                    it[UserLocations.address] = address
                    it[UserLocations.lastSyncTime] = lastSyncTime
                    it[UserLocations.updatedAt] = kotlinx.datetime.Clock.System.now()
                }
            } else {
                // Insert new location
                UserLocations.insert {
                    it[UserLocations.userId] = userId
                    it[UserLocations.ipAddress] = ipAddress
                    it[UserLocations.latitude] = latitude
                    it[UserLocations.longitude] = longitude
                    it[UserLocations.address] = address
                    it[UserLocations.lastSyncTime] = lastSyncTime
                }
            }
        }
    }

    /**
     * Get location data for a user by userId
     */
    fun getLocationByUserId(userId: String): UserLocation? {
        return dbTransaction {
            UserLocations.select { UserLocations.userId eq userId }
                .firstOrNull()
                ?.let { row ->
                    UserLocation(
                        id = row[UserLocations.id],
                        userId = row[UserLocations.userId],
                        ipAddress = row[UserLocations.ipAddress],
                        latitude = row[UserLocations.latitude],
                        longitude = row[UserLocations.longitude],
                        address = row[UserLocations.address],
                        lastSyncTime = row[UserLocations.lastSyncTime],
                        createdAt = row[UserLocations.createdAt],
                        updatedAt = row[UserLocations.updatedAt]
                    )
                }
        }
    }

    /**
     * Check if location exists for a user
     */
    fun locationExists(userId: String): Boolean {
        return dbTransaction {
            UserLocations.select { UserLocations.userId eq userId }.count() > 0
        }
    }
}

/**
 * Internal model for location data
 */
data class UserLocation(
    val id: Long,
    val userId: String,
    val ipAddress: String?,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val lastSyncTime: Instant,
    val createdAt: kotlinx.datetime.Instant,
    val updatedAt: kotlinx.datetime.Instant
)

