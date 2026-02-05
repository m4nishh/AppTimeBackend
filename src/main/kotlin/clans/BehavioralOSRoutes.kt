package com.apptime.code.clans

import com.apptime.code.common.respondApi
import com.apptime.code.common.respondError
import com.apptime.code.common.userId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Application.configureBehavioralOSRoutes() {
    val service = BehavioralOSService()
    
    routing {
        route("/api/behavioral-os") {
            authenticate("auth-bearer") {
                // ========== APP CATEGORY ENDPOINTS ==========
                
                // Set app category
                post("/app-categories") {
                    try {
                        val userId = call.userId.toString()
                        val request = call.receive<SetAppCategoryRequest>()
                        val category = service.setAppCategory(userId, request)
                        call.respondApi(category, "App category set successfully", HttpStatusCode.Created)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
                        )
                    }
                }
                
                // Get user's app categories
                get("/app-categories") {
                    try {
                        val userId = call.userId.toString()
                        val categories = service.getUserAppCategories(userId)
                        call.respondApi(categories, "App categories retrieved successfully")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
                        )
                    }
                }
                
                // Sync app category usage (from Android UsageStatsManager)
                post("/app-category-usage/sync") {
                    try {
                        val userId = call.userId.toString()
                        val request = call.receive<AppCategoryUsageSyncRequest>()
                        val response = service.syncAppCategoryUsage(userId, request)
                        call.respondApi(response, "App category usage synced successfully")
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
                        )
                    }
                }
                
                // Get app category usage
                get("/app-category-usage") {
                    try {
                        val userId = call.userId.toString()
                        val date = call.request.queryParameters["date"]
                            ?: throw IllegalArgumentException("Date parameter is required (YYYY-MM-DD)")
                        
                        val usage = service.getAppCategoryUsage(userId, date)
                        if (usage == null) {
                            call.respondApi(
                                AppCategoryUsageResponse(
                                    userId = userId,
                                    date = date,
                                    productiveTime = 0L,
                                    distractiveTime = 0L,
                                    lastSyncedAt = null
                                ),
                                "No usage data found for this date"
                            )
                        } else {
                            call.respondApi(usage, "App category usage retrieved successfully")
                        }
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
                        )
                    }
                }
                
                // ========== CLAN VAULT ENDPOINTS ==========
                
                // Get clan vault
                get("/clans/{clanId}/vault") {
                    try {
                        val userId = call.userId.toString()
                        val clanId = call.parameters["clanId"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid clan ID")
                        
                        val vault = service.getClanVault(clanId)
                        call.respondApi(vault, "Clan vault retrieved successfully")
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
                        )
                    }
                }
                
                // ========== CLAN CHALLENGE ENDPOINTS ==========
                
                // Create clan challenge
                post("/clans/{clanId}/challenges") {
                    try {
                        val userId = call.userId.toString()
                        val clanId = call.parameters["clanId"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid clan ID")
                        
                        val request = call.receive<CreateClanChallengeRequest>()
                        val challenge = service.createClanChallenge(userId, request.copy(clanId = clanId))
                        call.respondApi(challenge, "Clan challenge created successfully", HttpStatusCode.Created)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: IllegalStateException) {
                        call.respondError(HttpStatusCode.Forbidden, e.message ?: "Forbidden")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
                        )
                    }
                }
                
                // Get clan challenge
                get("/clan-challenges/{challengeId}") {
                    try {
                        val userId = call.userId.toString()
                        val challengeId = call.parameters["challengeId"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid challenge ID")
                        
                        val challenge = service.getClanChallenge(challengeId, userId)
                        call.respondApi(challenge, "Clan challenge retrieved successfully")
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: IllegalStateException) {
                        call.respondError(HttpStatusCode.NotFound, e.message ?: "Not found")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
                        )
                    }
                }
                
                // Join clan challenge
                post("/clan-challenges/{challengeId}/join") {
                    try {
                        val userId = call.userId.toString()
                        val challengeId = call.parameters["challengeId"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid challenge ID")
                        
                        val participant = service.joinClanChallenge(userId, JoinClanChallengeRequest(challengeId))
                        call.respondApi(participant, "Joined clan challenge successfully", HttpStatusCode.Created)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: IllegalStateException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Cannot join challenge")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
                        )
                    }
                }
                
                // Sync challenge stats (from app)
                post("/clan-challenges/{challengeId}/sync") {
                    try {
                        val userId = call.userId.toString()
                        val challengeId = call.parameters["challengeId"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid challenge ID")
                        
                        val totalTime = call.request.queryParameters["totalTime"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("totalTime parameter is required (milliseconds)")
                        
                        service.syncChallengeStats(challengeId, userId, totalTime)
                        call.respondApi(
                            mapOf("message" to "Challenge stats synced successfully"),
                            "Stats synced successfully"
                        )
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: IllegalStateException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Cannot sync stats")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
                        )
                    }
                }
                
                // Get clan challenge leaderboard
                get("/clan-challenges/{challengeId}/leaderboard") {
                    try {
                        val userId = call.userId.toString()
                        val challengeId = call.parameters["challengeId"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid challenge ID")
                        
                        val leaderboard = service.getClanChallengeLeaderboard(challengeId, userId)
                        call.respondApi(leaderboard, "Leaderboard retrieved successfully")
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
                        )
                    }
                }
                
                // Complete clan challenge (admin/moderator only)
                post("/clan-challenges/{challengeId}/complete") {
                    try {
                        val userId = call.userId.toString()
                        val challengeId = call.parameters["challengeId"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid challenge ID")
                        
                        service.completeClanChallenge(challengeId, userId)
                        call.respondApi(
                            mapOf("message" to "Challenge completed and jackpot distributed"),
                            "Challenge completed successfully"
                        )
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: IllegalStateException) {
                        call.respondError(HttpStatusCode.Forbidden, e.message ?: "Forbidden")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
                        )
                    }
                }
                
                // ========== CLAN WAR ENDPOINTS ==========
                
                // Create clan war
                post("/clan-wars") {
                    try {
                        val userId = call.userId.toString()
                        val request = call.receive<CreateClanWarRequest>()
                        val war = service.createClanWar(userId, request)
                        call.respondApi(war, "Clan war created successfully", HttpStatusCode.Created)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: IllegalStateException) {
                        call.respondError(HttpStatusCode.Forbidden, e.message ?: "Forbidden")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
                        )
                    }
                }
                
                // Get clan war
                get("/clan-wars/{warId}") {
                    try {
                        val warId = call.parameters["warId"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid war ID")
                        
                        val war = service.getClanWar(warId)
                        call.respondApi(war, "Clan war retrieved successfully")
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: IllegalStateException) {
                        call.respondError(HttpStatusCode.NotFound, e.message ?: "Not found")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
                        )
                    }
                }
                
                // Update clan war stats (can be called periodically)
                post("/clan-wars/{warId}/update-stats") {
                    try {
                        val warId = call.parameters["warId"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid war ID")
                        
                        service.updateClanWarStats(warId)
                        call.respondApi(
                            mapOf("message" to "Clan war stats updated successfully"),
                            "Stats updated successfully"
                        )
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
                        )
                    }
                }
                
                // ========== EDUCATION LEADERBOARD ENDPOINTS ==========
                
                // Get clan education leaderboard
                get("/clans/{clanId}/education-leaderboard") {
                    try {
                        val userId = call.userId.toString()
                        val clanId = call.parameters["clanId"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid clan ID")
                        
                        val period = call.request.queryParameters["period"] ?: "weekly"
                        val periodDate = call.request.queryParameters["periodDate"]
                        
                        val leaderboard = service.getClanEducationLeaderboard(clanId, period, periodDate, userId)
                        call.respondApi(leaderboard, "Education leaderboard retrieved successfully")
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: IllegalStateException) {
                        call.respondError(HttpStatusCode.Forbidden, e.message ?: "Forbidden")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
                        )
                    }
                }
            }
        }
    }
}

