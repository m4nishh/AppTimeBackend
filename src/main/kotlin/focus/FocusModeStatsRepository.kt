package com.apptime.code.focus

import com.apptime.code.common.dbTransaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq

class FocusModeStatsRepository {
    
    /**
     * Save focus mode stats
     */
    fun saveFocusModeStats(
        userId: String,
        startTime: Long,
        endTime: Long
    ): FocusModeStatsResponse {
        return dbTransaction {
            val duration = endTime - startTime
            
            FocusModeStats.insert {
                it[FocusModeStats.userId] = userId
                it[FocusModeStats.startTime] = startTime
                it[FocusModeStats.endTime] = endTime
            }
            
            // Get the inserted record
            val record = FocusModeStats.select { FocusModeStats.userId eq userId }
                .orderBy(FocusModeStats.id to SortOrder.DESC)
                .first()
            
            FocusModeStatsResponse(
                id = record[FocusModeStats.id],
                userId = record[FocusModeStats.userId],
                startTime = record[FocusModeStats.startTime],
                endTime = record[FocusModeStats.endTime],
                duration = duration,
                createdAt = record[FocusModeStats.createdAt].toString()
            )
        }
    }
    
    /**
     * Get focus mode stats for a user with time-based filtering
     * @param userId User ID
     * @param startTimeMs Optional start time filter (milliseconds) - only return stats with startTime >= this
     * @param endTimeMs Optional end time filter (milliseconds) - only return stats with endTime <= this
     */
    fun getFocusModeStats(
        userId: String,
        startTimeMs: Long? = null,
        endTimeMs: Long? = null
    ): FocusModeStatsListResponse {
        return dbTransaction {
            val query = FocusModeStats.select {
                val baseCondition = FocusModeStats.userId eq userId
                when {
                    startTimeMs != null && endTimeMs != null -> {
                        baseCondition and (FocusModeStats.startTime greaterEq startTimeMs) and (FocusModeStats.endTime lessEq endTimeMs)
                    }
                    startTimeMs != null -> {
                        baseCondition and (FocusModeStats.startTime greaterEq startTimeMs)
                    }
                    endTimeMs != null -> {
                        baseCondition and (FocusModeStats.endTime lessEq endTimeMs)
                    }
                    else -> baseCondition
                }
            }
            
            val records = query.orderBy(FocusModeStats.startTime to SortOrder.DESC).toList()
            
            val stats = records.map { record ->
                val duration = record[FocusModeStats.endTime] - record[FocusModeStats.startTime]
                FocusModeStatsItem(
                    id = record[FocusModeStats.id],
                    startTime = record[FocusModeStats.startTime],
                    endTime = record[FocusModeStats.endTime],
                    duration = duration,
                    createdAt = record[FocusModeStats.createdAt].toString()
                )
            }
            
            val totalDuration = stats.sumOf { it.duration }
            
            FocusModeStatsListResponse(
                stats = stats,
                totalCount = stats.size,
                totalDuration = totalDuration
            )
        }
    }
}

