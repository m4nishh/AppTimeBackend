package com.apptime.code.challenges

import com.apptime.code.common.MessageKeys
import com.apptime.code.common.respondApi
import com.apptime.code.common.respondError
import com.apptime.code.common.requireUserId
import com.apptime.code.common.userId
import com.apptime.code.rewards.RewardRepository
import com.apptime.code.rewards.RewardService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import java.net.URLEncoder

/**
 * Configure challenge-related routes
 */
fun Application.configureChallengeRoutes() {
    val repository = ChallengeRepository()
    val notificationRepository = com.apptime.code.notifications.NotificationRepository()
    val userRepository = users.UserRepository()
    val notificationService = com.apptime.code.notifications.NotificationService(notificationRepository, userRepository)
    val service = ChallengeService(repository, notificationService)
    val rewardRepository = RewardRepository()
    val rewardService = RewardService(rewardRepository, repository, notificationService)
    
    routing {
        route("/api/challenges") {
            authenticate("auth-bearer", optional = true) {
                /**
                 * GET /api/challenges/active
                 * Get all active challenges (public endpoint, auth optional)
                 * Returns: List of active challenges with title, description, reward, startTime, endTime, thumbnail
                 * If the user is authenticated, each challenge includes hasJoined flag.
                 */
                get("/active") {
                    try {
                        val response = service.getActiveChallenges(call.userId)
                        call.respondApi(response, messageKey = MessageKeys.ACTIVE_CHALLENGES_RETRIEVED)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.ACTIVE_CHALLENGES_FAILED, message = "Failed to retrieve active challenges: ${e.message}")
                    }
                }
            }
            
            authenticate("auth-bearer") {
                /**
                 * POST /api/challenges/join
                 * Register/join a challenge (requires authentication)
                 * Request body: { "challengeId": 1 }
                 */
                post("/join") {
                    try {
                        val userId = call.requireUserId()
                        val request = call.receive<JoinChallengeRequest>()
                        
                        val response = service.joinChallenge(userId, request.challengeId)
                        
                        // Award participation reward
                        try {
                            val challenge = repository.getChallengeById(request.challengeId)
                            if (challenge != null) {
                                rewardService.awardChallengeParticipationReward(
                                    userId = userId,
                                    challengeId = request.challengeId,
                                    challengeTitle = challenge.title
                                )
                            }
                        } catch (e: Exception) {
                            // Log error but don't fail the join request if reward fails
                            println("Failed to award participation reward: ${e.message}")
                        }
                        
                        call.respondApi(response, statusCode = HttpStatusCode.Created, message = response.message)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.CHALLENGE_JOIN_FAILED, message = "Failed to join challenge: ${e.message}")
                    }
                }
                
                /**
                 * GET /api/challenges/user
                 * Get all challenges for the authenticated user (including past ones)
                 * Returns: List of challenges with joinedAt timestamp and isPast flag
                 */
                get("/user") {
                    try {
                        val userId = call.requireUserId()
                        val response = service.getUserChallenges(userId)
                        call.respondApi(response, messageKey = MessageKeys.USER_CHALLENGES_RETRIEVED)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.USER_CHALLENGES_FAILED, message = "Failed to retrieve user challenges: ${e.message}")
                    }
                }
                
                /**
                 * GET /api/challenges/{challengeId}
                 * Get challenge details including participant count and hasJoined flag (requires authentication)
                 * Path parameter: challengeId
                 * Returns: Challenge details with hasJoined field indicating if current user has joined
                 */
                get("/{challengeId}") {
                    try {
                        val challengeId = call.parameters["challengeId"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid challenge ID")
                        
                        val userId = call.userId
                        val response = service.getChallengeDetail(challengeId, userId)
                        call.respondApi(response, messageKey = MessageKeys.CHALLENGE_DETAILS_RETRIEVED)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.CHALLENGE_DETAILS_FAILED, message = "Failed to retrieve challenge details: ${e.message}")
                    }
                }
                
                /**
                 * POST /api/challenges/stats
                 * Submit challenge participant stats (requires authentication)
                 * Request body: {
                 *   "challengeId": 1,
                 *   "appName": "Instagram",
                 *   "packageName": "com.instagram.android",
                 *   "startSyncTime": "2024-01-15T10:00:00Z",
                 *   "endSyncTime": "2024-01-15T10:30:00Z",
                 *   "duration": 1800000
                 * }
                 */
                post("/stats") {
                    try {
                        val userId = call.requireUserId()
                        val request = call.receive<ChallengeStatsSubmissionRequest>()
                        
                        service.submitChallengeStats(userId, request)
                        call.respondApi(
                            mapOf("message" to "Challenge stats submitted successfully"),
                            statusCode = HttpStatusCode.Created,
                            messageKey = MessageKeys.CHALLENGE_STATS_SUBMITTED
                        )
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.CHALLENGE_STATS_FAILED, message = "Failed to submit challenge stats: ${e.message}")
                    }
                }
                
                /**
                 * POST /api/challenges/stats/batch
                 * Submit multiple challenge participant stats (requires authentication)
                 * Request body: {
                 *   "challengeId": 1,
                 *   "stats": [
                 *     {
                 *       "challengeId": 1,
                 *       "appName": "Instagram",
                 *       "packageName": "com.instagram.android",
                 *       "startSyncTime": "2024-01-15T10:00:00Z",
                 *       "endSyncTime": "2024-01-15T10:30:00Z",
                 *       "duration": 1800000
                 *     }
                 *   ]
                 * }
                 */
                post("/stats/batch") {
                    try {
                        val userId = call.requireUserId()
                        val request = call.receive<BatchChallengeStatsSubmissionRequest>()
                        
                        val response = service.submitBatchChallengeStats(
                            userId,
                            request.challengeId,
                            request.stats
                        )
                        call.respondApi(response, statusCode = HttpStatusCode.Created, messageKey = MessageKeys.CHALLENGE_STATS_SUBMITTED)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.CHALLENGE_STATS_FAILED, message = "Failed to submit challenge stats: ${e.message}")
                    }
                }
                
                /**
                 * GET /api/challenges/{challengeId}/rankings
                 * Get challenge rankings - top 10 players with sum of duration (requires authentication)
                 * Ranking calculation:
                 * - For LESS_SCREENTIME challenges: ranked by total duration ascending (lower is better)
                 * - For MORE_SCREENTIME challenges: ranked by total duration descending (higher is better)
                 * Path parameter: challengeId
                 * Returns: Top 10 rankings and current user's rank if they're participating
                 */
                get("/{challengeId}/rankings") {
                    try {
                        val userId = call.requireUserId()
                        val challengeId = call.parameters["challengeId"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid challenge ID")
                        
                        val response = service.getChallengeRankings(challengeId, userId)
                        call.respondApi(response, messageKey = MessageKeys.CHALLENGE_RANKINGS_RETRIEVED)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.CHALLENGE_RANKINGS_FAILED, message = "Failed to retrieve challenge rankings: ${e.message}")
                    }
                }
                
                /**
                 * GET /api/challenges/{challengeId}/last-sync
                 * Get last sync time for the authenticated user in a specific challenge (requires authentication)
                 * Path parameter: challengeId
                 * Returns: Last sync time (endSyncTime of most recent stat submission) or null if no stats submitted
                 */
                get("/{challengeId}/last-sync") {
                    try {
                        val userId = call.requireUserId()
                        val challengeId = call.parameters["challengeId"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid challenge ID")
                        
                        val response = service.getLastSyncTime(userId, challengeId)
                        call.respondApi(response, messageKey = MessageKeys.CHALLENGE_LAST_SYNC_RETRIEVED)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.CHALLENGE_LAST_SYNC_FAILED, message = "Failed to retrieve last sync time: ${e.message}")
                    }
                }
                
                /**
                 * GET /api/challenges/{challengeId}/share
                 * Get shareable link for a challenge (requires authentication)
                 * Path parameter: challengeId
                 * Returns: Share link and deeplink for the challenge with tracking code
                 */
                get("/{challengeId}/share") {
                    try {
                        val userId = call.requireUserId()
                        val challengeId = call.parameters["challengeId"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid challenge ID")
                        
                        // Validate challenge exists
                        repository.getChallengeById(challengeId)
                            ?: throw IllegalArgumentException("Challenge not found")
                        
                        // Create or get share record
                        val shareCode = repository.createOrGetChallengeShare(challengeId, userId)
                        
                        // Get base URL from request
                        val scheme = call.request.origin.scheme
                        val host = call.request.host()
                        val port = call.request.port()
                        val baseUrl = if (port == 80 || port == 443) {
                            "$scheme://$host"
                        } else {
                            "$scheme://$host:$port"
                        }
                        
                        // Encode challenge ID and share code into a secure token
                        val token = com.apptime.code.common.TokenEncoder.encodeChallengeShare(challengeId, shareCode)
                        val encodedToken = URLEncoder.encode(token, "UTF-8")
                        
                        // Use token instead of revealing challenge ID and share code
                        val shareLink = "$baseUrl/challenge/$encodedToken"
                        val deeplink = "apptime://screen/challenge_detail/$encodedToken"
                        
                        val response = ChallengeShareLinkResponse(
                            challengeId = challengeId,
                            shareLink = shareLink,
                            deeplink = deeplink,
                            shareCode = shareCode
                        )
                        
                        call.respondApi(response, messageKey = MessageKeys.CHALLENGE_DETAILS_RETRIEVED)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.CHALLENGE_DETAILS_FAILED, message = "Failed to get share link: ${e.message}")
                    }
                }
                
                /**
                 * POST /api/challenges/share/track
                 * Track share event (install, app_open) (requires authentication)
                 * Request body: { "token": "encoded_token", "eventType": "INSTALL", "deviceId": "device123" }
                 * OR: { "shareCode": "ABC123", "eventType": "INSTALL", "deviceId": "device123" }
                 */
                post("/share/track") {
                    try {
                        val userId = call.requireUserId()
                        val request = call.receive<TrackShareEventRequest>()
                        
                        // Validate event type
                        if (request.eventType !in listOf("INSTALL", "APP_OPEN")) {
                            throw IllegalArgumentException("Invalid event type. Must be INSTALL or APP_OPEN")
                        }
                        
                        // Get share code from token or direct shareCode
                        val shareCode = if (request.token != null) {
                            // Decode token to get share code
                            val decoded = com.apptime.code.common.TokenEncoder.decodeChallengeShare(request.token)
                                ?: throw IllegalArgumentException("Invalid token")
                            decoded.second // Return shareCode from decoded token
                        } else if (request.shareCode != null) {
                            request.shareCode
                        } else {
                            throw IllegalArgumentException("Either token or shareCode must be provided")
                        }
                        
                        // Track the event
                        repository.trackShareEvent(
                            shareCode = shareCode,
                            eventType = request.eventType,
                            installerUserId = userId,
                            deviceId = request.deviceId
                        )
                        
                        call.respondApi(
                            mapOf("message" to "Event tracked successfully"),
                            statusCode = HttpStatusCode.Created,
                            messageKey = MessageKeys.CHALLENGE_STATS_SUBMITTED
                        )
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.CHALLENGE_STATS_FAILED, message = "Failed to track event: ${e.message}")
                    }
                }
                
                /**
                 * GET /api/challenges/share/stats
                 * Get share statistics for the authenticated user (requires authentication)
                 * Returns: Total shares, clicks, and installations
                 */
                get("/share/stats") {
                    try {
                        val userId = call.requireUserId()
                        val stats = repository.getUserShareStats(userId)
                        
                        val response = ShareStatsResponse(
                            totalShares = stats.totalShares,
                            totalClicks = stats.totalClicks,
                            totalInstalls = stats.totalInstalls
                        )
                        
                        call.respondApi(response, messageKey = MessageKeys.CHALLENGE_DETAILS_RETRIEVED)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.CHALLENGE_DETAILS_FAILED, message = "Failed to get share stats: ${e.message}")
                    }
                }
            }
        }
    }
}

