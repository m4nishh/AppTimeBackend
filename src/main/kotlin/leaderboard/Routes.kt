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
             * If authenticated, includes userRank in response
             * Query param: date (optional) - YYYY-MM-DD format. Defaults to today
             */
            get("/daily") {
                try {
                    val date = call.request.queryParameters["date"]
                    val currentUserId = call.userId // Optional - will be null if not authenticated
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
             * If authenticated, includes userRank in response
             * Query param: weekDate (optional) - YYYY-WW format. Defaults to current week
             */
            get("/weekly") {
                try {
                    val weekDate = call.request.queryParameters["weekDate"]
                    val currentUserId = call.userId // Optional - will be null if not authenticated
                    val leaderboard = service.getWeeklyLeaderboard(weekDate, currentUserId)
                    call.respondApi(leaderboard, "Weekly leaderboard retrieved successfully")
                } catch (e: IllegalArgumentException) {
                    call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                } catch (e: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, "Failed to retrieve weekly leaderboard: ${e.message}")
                }
            }
        }
    }
}

