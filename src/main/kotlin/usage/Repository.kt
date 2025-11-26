package usage

import com.apptime.code.common.dbTransaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlinx.datetime.Instant

class AppUsageEventRepository {
    
    /**
     * Submit a single app usage event
     */
    fun submitEvent(
        userId: String,
        packageName: String,
        appName: String?,
        isSystemApp: Boolean,
        eventType: String,
        eventTimestamp: Instant,
        duration: Long? = null
    ): AppUsageEvent {
        return dbTransaction {
            AppUsageEvents.insert {
                it[AppUsageEvents.userId] = userId
                it[AppUsageEvents.packageName] = packageName
                it[AppUsageEvents.appName] = appName
                it[AppUsageEvents.isSystemApp] = isSystemApp
                it[AppUsageEvents.eventType] = eventType
                it[AppUsageEvents.eventTimestamp] = eventTimestamp
                it[AppUsageEvents.duration] = duration
            }
            
            // Get the inserted record
            val record = AppUsageEvents.select { 
                (AppUsageEvents.userId eq userId) and
                (AppUsageEvents.packageName eq packageName) and
                (AppUsageEvents.eventTimestamp eq eventTimestamp)
            }
            .orderBy(AppUsageEvents.id to SortOrder.DESC)
            .first()
            
            AppUsageEvent(
                id = record[AppUsageEvents.id],
                userId = record[AppUsageEvents.userId],
                packageName = record[AppUsageEvents.packageName],
                appName = record[AppUsageEvents.appName],
                isSystemApp = record[AppUsageEvents.isSystemApp],
                eventType = record[AppUsageEvents.eventType],
                eventTimestamp = record[AppUsageEvents.eventTimestamp].toString(),
                duration = record[AppUsageEvents.duration],
                createdAt = record[AppUsageEvents.createdAt].toString()
            )
        }
    }
    
    /**
     * Submit multiple app usage events in batch
     */
    fun submitBatchEvents(
        userId: String,
        events: List<AppUsageEventRequest>
    ): List<AppUsageEvent> {
        return dbTransaction {
            events.map { event ->
                val eventTimestamp = Instant.parse(event.eventTimestamp)
                
                AppUsageEvents.insert {
                    it[AppUsageEvents.userId] = userId
                    it[AppUsageEvents.packageName] = event.packageName
                    it[AppUsageEvents.appName] = event.appName
                    it[AppUsageEvents.isSystemApp] = event.isSystemApp
                    it[AppUsageEvents.eventType] = event.eventType
                    it[AppUsageEvents.eventTimestamp] = eventTimestamp
                    it[AppUsageEvents.duration] = event.duration
                }
                
                // Get the inserted record
                val record = AppUsageEvents.select { 
                    (AppUsageEvents.userId eq userId) and
                    (AppUsageEvents.packageName eq event.packageName) and
                    (AppUsageEvents.eventTimestamp eq eventTimestamp)
                }
                .orderBy(AppUsageEvents.id to SortOrder.DESC)
                .first()
                
                AppUsageEvent(
                    id = record[AppUsageEvents.id],
                    userId = record[AppUsageEvents.userId],
                    packageName = record[AppUsageEvents.packageName],
                    appName = record[AppUsageEvents.appName],
                    isSystemApp = record[AppUsageEvents.isSystemApp],
                    eventType = record[AppUsageEvents.eventType],
                    eventTimestamp = record[AppUsageEvents.eventTimestamp].toString(),
                    duration = record[AppUsageEvents.duration],
                    createdAt = record[AppUsageEvents.createdAt].toString()
                )
            }
        }
    }
    
    /**
     * Get the last sync time for a user (most recent eventTimestamp)
     */
    fun getLastSyncTime(userId: String): Instant? {
        return dbTransaction {
            AppUsageEvents.select {
                AppUsageEvents.userId eq userId
            }
            .orderBy(AppUsageEvents.eventTimestamp to SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.get(AppUsageEvents.eventTimestamp)
        }
    }
}

