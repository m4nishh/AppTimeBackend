package usage

import com.apptime.code.common.MessageKeys
import com.apptime.code.common.respondApi
import com.apptime.code.common.respondError
import com.apptime.code.common.requireUserId
import users.UserRepository
import users.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

/**
 * Configure app usage event routes
 */
fun Application.configureAppUsageEventRoutes() {
    val repository = AppUsageEventRepository()
    val userRepository = UserRepository()
    val eventService = AppUsageEventService(repository, userRepository)
    
    val statsRepository = UsageStatsRepository()
    val statsService = UsageStatsService(statsRepository)
    val eventStatsService = AppUsageEventStatsService(repository)
    
    routing {
        route("/api/usage/events") {
            authenticate("auth-bearer") {
                /**
                 * POST /api/usage/events
                 * Submit a single app usage event with sync time (requires authentication)
                 * Request body includes syncTime at top level and event object
                 */
                post {
                    try {
                        val userId = call.requireUserId()
                        val request = call.receive<AppUsageEventSubmissionRequest>()
                        
                        val event = eventService.submitEvent(userId, request)
                        call.respondApi(event, statusCode = HttpStatusCode.Created, messageKey = MessageKeys.USAGE_EVENT_SUBMITTED)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.USAGE_EVENT_FAILED, message = "Failed to submit event: ${e.message}")
                    }
                }
                
                /**
                 * POST /api/usage/events/batch
                 * Submit multiple app usage events in batch (requires authentication)
                 * Maximum 100 events per batch
                 */
                post("/batch") {
                    try {
                        val userId = call.requireUserId()
                        val request = call.receive<BatchAppUsageEventRequest>()
                        
                        val events = eventService.submitBatchEvents(userId, request)
                        call.respondApi(
                            BatchAppUsageEventResponse(events = events, count = events.size),
                            statusCode = HttpStatusCode.Created,
                            messageKey = MessageKeys.BATCH_EVENTS_SUBMITTED
                        )
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.BATCH_EVENTS_FAILED, message = "Failed to submit batch events: ${e.message}")
                    }
                }
            }
        }
        
        route("/api/usage/stats") {
            /**
             * GET /api/usage/stats/users
             * Get all userIds that have data in the database (no auth required for debugging)
             * Returns list of all userIds with their data counts
             */
            get("/users") {
                try {
                    val allUsersInfo = statsRepository.getAllUserIdsWithData()
                    call.respondApi(allUsersInfo, messageKey = MessageKeys.ALL_USERS_RETRIEVED)
                } catch (e: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.ALL_USERS_FAILED, message = "Failed to get users: ${e.message}")
                }
            }
            
            authenticate("auth-bearer") {
                /**
                 * GET /api/usage/stats/daily
                 * Get daily usage stats for a user (requires authentication)
                 * Query params:
                 *   - date (required) - YYYY-MM-DD format
                 *   - targetUserId (optional) - User ID or username. If provided, checks TOTP verification session
                 *                                If not provided, returns authenticated user's own data
                 * Returns list of daily usage stats (unencrypted for now)
                 */
                get("/daily") {
                    try {
                        val requestingUserId = call.requireUserId() // User A (authenticated)
                        val date = call.request.queryParameters["date"]
                            ?: throw IllegalArgumentException("Date parameter is required (format: YYYY-MM-DD)")
                        val targetUserIdOrUsername = call.request.queryParameters["targetUserId"] // User B (optional - can be ID or username)
                        
                        // If targetUserId is provided, check TOTP verification session
                        if (targetUserIdOrUsername != null && targetUserIdOrUsername != requestingUserId) {
                            val userService = UserService(userRepository)
                            
                            // Convert username to user ID if needed (check if it's a username by format)
                            val actualTargetUserId = if (targetUserIdOrUsername.contains("-") && targetUserIdOrUsername.length > 30) {
                                // Looks like a UUID (user ID)
                                targetUserIdOrUsername
                            } else {
                                // Looks like a username, convert to user ID
                                userRepository.getUserIdByUsername(targetUserIdOrUsername)
                                    ?: throw IllegalArgumentException("User not found: $targetUserIdOrUsername")
                            }
                            
                            // Check if valid session exists
                            val sessionDetails = userService.getTOTPVerificationSessionDetails(
                                requestingUserId,
                                actualTargetUserId
                            )
                            
                            if (sessionDetails == null) {
                                call.respondError(
                                    HttpStatusCode.Forbidden,
                                    "No valid TOTP verification session. Please verify TOTP code first to access this user's data."
                                )
                                return@get
                            }
                            
                            // Get stats for target user
                            val stats = statsService.getDailyUsageStats(actualTargetUserId, date)
                            
                            // Convert session details to serializable object
                            val sessionInfo = TOTPVerificationSessionInfo(
                                requestingUserId = sessionDetails["requestingUserId"] as String,
                                targetUserId = sessionDetails["targetUserId"] as String,
                                targetUsername = sessionDetails["targetUsername"] as String,
                                verifiedAt = sessionDetails["verifiedAt"] as String,
                                expiresAt = sessionDetails["expiresAt"] as String,
                                remainingSeconds = sessionDetails["remainingSeconds"] as Int,
                                remainingMinutes = sessionDetails["remainingMinutes"] as Int,
                                isValid = sessionDetails["isValid"] as Boolean
                            )
                            
                            // Return stats with session info
                            call.respondApi(
                                DailyUsageStatsWithSessionResponse(
                                    stats = stats,
                                    session = sessionInfo
                                ),
                                messageKey = MessageKeys.DAILY_STATS_RETRIEVED,
                                message = "Daily usage stats retrieved successfully. Session expires in ${sessionInfo.remainingMinutes} minutes."
                            )
                        } else {
                            // Return requesting user's own data (no session check needed)
                            val stats = statsService.getDailyUsageStats(requestingUserId, date)
                            call.respondApi(stats, messageKey = MessageKeys.DAILY_STATS_RETRIEVED)
                        }
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.DAILY_STATS_FAILED, message = "Failed to retrieve daily usage stats: ${e.message}")
                    }
                }
                
                /**
                 * GET /api/usage/stats/debug
                 * Get debug information about user's app usage data (requires authentication)
                 * Query param: date (optional) - YYYY-MM-DD format to filter by specific date
                 * Returns debug info including available dates and sample records
                 */
                get("/debug") {
                    try {
                        val userId = call.requireUserId()
                        val date = call.request.queryParameters["date"]
                        
                        val debugInfo = statsRepository.getDebugInfo(userId, date)
                        call.respondApi(debugInfo, messageKey = MessageKeys.DEBUG_INFO_RETRIEVED)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.DEBUG_INFO_FAILED, message = "Failed to get debug info: ${e.message}")
                    }
                }
                
                /**
                 * GET /api/usage/stats/raw
                 * Get all raw events from database for authenticated user (requires authentication)
                 * Returns all events stored in app_usage_events table
                 */
                get("/raw") {
                    try {
                        val userId = call.requireUserId()
                        val rawEvents = statsRepository.getAllRawEventsForUser(userId)
                        call.respondApi(
                            RawEventsResponse(
                                userId = userId,
                                totalEvents = rawEvents.size,
                                events = rawEvents
                            ),
                            messageKey = MessageKeys.RAW_EVENTS_RETRIEVED
                        )
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.RAW_EVENTS_FAILED, message = "Failed to get raw events: ${e.message}")
                    }
                }
                
                /**
                 * GET /api/usage/stats/last-sync
                 * Get the last sync time for app usage stats (requires authentication)
                 * Returns the most recent eventTimestamp from app_usage_events table
                 */
                get("/last-sync") {
                    try {
                        val userId = call.requireUserId()
                        val response = eventStatsService.getLastSyncTime(userId)
                        call.respondApi(response, messageKey = MessageKeys.LAST_SYNC_RETRIEVED)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.LAST_SYNC_FAILED, message = "Failed to retrieve last sync time: ${e.message}")
                    }
                }
                
                /**
                 * DELETE /api/usage/stats/delete
                 * Delete all app usage events for the authenticated user (requires authentication)
                 * This will permanently delete all events from app_usage_events table for the user
                 */
                delete("/delete") {
                    try {
                        val userId = call.requireUserId()
                        val response = eventStatsService.deleteUserEvents(userId)
                        call.respondApi(response, messageKey = MessageKeys.USER_EVENTS_DELETED)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.USER_EVENTS_DELETE_FAILED, message = "Failed to delete user events: ${e.message}")
                    }
                }
            }
        }
    }
}

