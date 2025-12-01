package com.apptime.code.leaderboard

import com.apptime.code.common.respondApi
import com.apptime.code.common.respondError
import com.apptime.code.common.userId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

/**
 * Configure leaderboard-related routes
 */
fun Application.configureLeaderboardRoutes() {
    val repository = LeaderboardRepository()
    val service = LeaderboardService(repository)
    
    routing {
        route("/api/leaderboard") {
            /**
             * GET /api/leaderboard/daily
             * Get daily leaderboard (public, no auth required)
             * If userId provided or authenticated, includes userRank in response
             * Query params:
             *   - date (optional) - YYYY-MM-DD format. Defaults to today
             *   - userId (optional) - User ID to get rank for. If not provided, uses authenticated user's ID
             */
            get("/daily") {
                try {
                    val date = call.request.queryParameters["date"]
                    // Priority: query param userId > authenticated userId > null
                    val currentUserId = call.request.queryParameters["userId"] ?: call.userId
                    val leaderboard = service.getDailyLeaderboard(date, currentUserId)
                    call.respondApi(leaderboard, "Daily leaderboard retrieved successfully")
                } catch (e: IllegalArgumentException) {
                    call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                } catch (e: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, "Failed to retrieve daily leaderboard: ${e.message}")
                }
            }
            
            /**
             * GET /api/leaderboard/weekly
             * Get weekly leaderboard (public, no auth required)
             * If userId provided or authenticated, includes userRank in response
             * Query params:
             *   - weekDate (optional) - YYYY-WW format. Defaults to current week
             *   - userId (optional) - User ID to get rank for. If not provided, uses authenticated user's ID
             */
            get("/weekly") {
                try {
                    val weekDate = call.request.queryParameters["weekDate"]
                    // Priority: query param userId > authenticated userId > null
                    val currentUserId = call.request.queryParameters["userId"] ?: call.userId
                    val leaderboard = service.getWeeklyLeaderboard(weekDate, currentUserId)
                    call.respondApi(leaderboard, "Weekly leaderboard retrieved successfully")
                } catch (e: IllegalArgumentException) {
                    call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                } catch (e: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, "Failed to retrieve weekly leaderboard: ${e.message}")
                }
            }
            
            /**
             * POST /api/leaderboard/sync
             * Sync data from app_usage_events to leaderboardstats
             * Query param: date (optional) - YYYY-MM-DD format. If not provided, syncs all dates
             * This endpoint aggregates screen time from app_usage_events and stores it in leaderboardstats
             */
            post("/sync") {
                try {
                    val date = call.request.queryParameters["date"]
                    val result = service.syncFromAppUsageEvents(date)
                    call.respondApi(result, "Leaderboard stats synced successfully")
                } catch (e: IllegalArgumentException) {
                    call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                } catch (e: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, "Failed to sync leaderboard stats: ${e.message}")
                }
            }
        }
    }
}

