package com.apptime.code.clans

import com.apptime.code.common.userId
import com.apptime.code.notifications.NotificationService
import com.apptime.code.notifications.NotificationRepository
import users.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.net.URLEncoder

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class SuccessResponse<T>(
    val message: String,
    val data: T? = null
)


fun Application.configureClanRoutes(
    notificationService: NotificationService? = null,
    userRepository: UserRepository? = null
) {
    val clanService = ClanService(
        notificationService = notificationService,
        userRepository = userRepository,
        notificationScope = this
    )
    
    routing {
        route("/api/clans") {
            authenticate("auth-bearer", optional = true) {
                // Create a clan
                post {
                    try {
                        val userId = call.userId.toString();
                        val request = call.receive<CreateClanRequest>()
                        val clan = clanService.createClan(userId, request)
                        call.respond(HttpStatusCode.Created, SuccessResponse("Clan created successfully", clan))
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Invalid request"))
                    } catch (e: IllegalStateException) {
                        call.respond(HttpStatusCode.Conflict, ErrorResponse(e.message ?: "Conflict"))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(e.message ?: "Internal server error")
                        )
                    }
                }
            }

            authenticate("auth-bearer", optional = true) {
                // List clans with filters
                get {
                    try {
                        val userId = try {
                           call.userId.toString()
                        } catch (e: Exception) {
                            null
                        }
                        val category = call.request.queryParameters["category"]
                        val country = call.request.queryParameters["country"]
                        val city = call.request.queryParameters["city"]
                        val searchQuery = call.request.queryParameters["q"]
                        val clanType = call.request.queryParameters["clanType"]
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                        val response = clanService.listClans(
                            category = category,
                            country = country,
                            city = city,
                            searchQuery = searchQuery,
                            clanType = clanType,
                            page = page,
                            pageSize = pageSize,
                            userId = userId
                        )

                        call.respond(HttpStatusCode.OK, SuccessResponse("Clans retrieved successfully", response))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(e.message ?: "Internal server error")
                        )
                    }
                }
            }

            authenticate("auth-bearer", optional = true) {
                // Get clan leaderboard
                get("/leaderboard") {
                    try {
                        val userId = try {
                            call.userId.toString()
                        } catch (e: Exception) {
                            null
                        }
                        val period = call.request.queryParameters["period"] ?: "weekly"
                        val periodDate = call.request.queryParameters["periodDate"]
                        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

                        val response = clanService.getClanLeaderboard(period, periodDate, limit, userId)
                        call.respond(HttpStatusCode.OK, SuccessResponse("Leaderboard retrieved successfully", response))
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Invalid request"))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(e.message ?: "Internal server error")
                        )
                    }
                }
            }

            authenticate("auth-bearer", optional = true) {
                // Get user's clan info
                get("/my-clan") {
                    try {
                        val userId = call.userId.toString()
                        val response = clanService.getUserClanInfo(userId)
                        call.respond(HttpStatusCode.OK, SuccessResponse("Clan info retrieved successfully", response))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(e.message ?: "Internal server error")
                        )
                    }
                }

                authenticate("auth-bearer", optional = true) {
                    // Get specific clan details
                    get("/{clanId}") {
                        try {
                            val userId = try {
                                call.userId.toString()
                            } catch (e: Exception) {
                                null
                            }
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid clan ID"))

                            val response = clanService.getClanDetails(clanId, userId)
                            call.respond(HttpStatusCode.OK, SuccessResponse("Clan details retrieved successfully", response))
                        } catch (e: IllegalStateException) {
                            call.respond(HttpStatusCode.NotFound, ErrorResponse(e.message ?: "Clan not found"))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(e.message ?: "Internal server error")
                            )
                        }
                    }
                }

                authenticate("auth-bearer", optional = true) {
                    // Update clan details
                    patch("/{clanId}") {
                        try {
                            val userId = call.userId.toString()
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@patch call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponse("Invalid clan ID")
                                )
                            val request = call.receive<UpdateClanRequest>()

                            val clan = clanService.updateClan(clanId, userId, request)
                            call.respond(HttpStatusCode.OK, SuccessResponse("Clan updated successfully", clan))
                        } catch (e: IllegalArgumentException) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Invalid request"))
                        } catch (e: IllegalStateException) {
                            call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "Forbidden"))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(e.message ?: "Internal server error")
                            )
                        }
                    }
                }


                authenticate("auth-bearer", optional = true) {
                    // Delete clan
                    delete("/{clanId}") {
                        try {
                            val userId = call.userId.toString()
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@delete call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponse("Invalid clan ID")
                                )

                            clanService.deleteClan(clanId, userId)
                            call.respond(HttpStatusCode.OK, SuccessResponse<Unit>("Clan deleted successfully"))
                        } catch (e: IllegalStateException) {
                            call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "Forbidden"))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(e.message ?: "Internal server error")
                            )
                        }
                    }
                }


                authenticate("auth-bearer", optional = true) {
                    // Join a clan
                    post("/join") {
                        try {
                            val userId = call.userId.toString()
                            val request = call.receive<JoinClanRequest>()
                            val member = clanService.joinClan(userId, request)
                            call.respond(HttpStatusCode.OK, SuccessResponse("Joined clan successfully", member))
                        } catch (e: IllegalArgumentException) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Invalid request"))
                        } catch (e: IllegalStateException) {
                            // Check if it's a join request creation message
                            if (e.message?.contains("Join request created") == true) {
                                call.respond(HttpStatusCode.Accepted, SuccessResponse<Unit>(e.message ?: "Join request created successfully"))
                            } else {
                                call.respond(HttpStatusCode.Conflict, ErrorResponse(e.message ?: "Conflict"))
                            }
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(e.message ?: "Internal server error")
                            )
                        }
                    }
                }

                authenticate("auth-bearer", optional = true) {
                    // Leave a clan
                    post("/{clanId}/leave") {
                        try {
                            val userId = call.userId.toString()
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid clan ID"))

                            clanService.leaveClan(userId, clanId)
                            call.respond(HttpStatusCode.OK, SuccessResponse<Unit>("Left clan successfully"))
                        } catch (e: IllegalStateException) {
                            call.respond(HttpStatusCode.Conflict, ErrorResponse(e.message ?: "Conflict"))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(e.message ?: "Internal server error")
                            )
                        }
                    }
                }

                authenticate("auth-bearer", optional = true) {
                    // Get clan members
                    get("/{clanId}/members") {
                        try {
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid clan ID"))

                            val members = clanService.getClanDetails(clanId, null).members
                            call.respond(HttpStatusCode.OK, SuccessResponse("Clan members retrieved successfully", members))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(e.message ?: "Internal server error")
                            )
                        }
                    }
                }

                authenticate("auth-bearer", optional = true) {
                    // Update member role
                    patch("/{clanId}/members/role") {
                        try {
                            val userId = call.userId.toString()
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@patch call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponse("Invalid clan ID")
                                )
                            val request = call.receive<UpdateMemberRoleRequest>()

                            clanService.updateMemberRole(clanId, userId, request)
                            call.respond(HttpStatusCode.OK, SuccessResponse<Unit>("Member role updated successfully"))
                        } catch (e: IllegalArgumentException) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Invalid request"))
                        } catch (e: IllegalStateException) {
                            call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "Forbidden"))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(e.message ?: "Internal server error")
                            )
                        }
                    }
                }

                authenticate("auth-bearer", optional = true) {
                    // Remove member
                    post("/{clanId}/members/remove") {
                        try {
                            val userId = call.userId.toString()
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid clan ID"))
                            val request = call.receive<RemoveMemberRequest>()

                            clanService.removeMember(clanId, userId, request)
                            call.respond(HttpStatusCode.OK, SuccessResponse<Unit>("Member removed successfully"))
                        } catch (e: IllegalStateException) {
                            call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "Forbidden"))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(e.message ?: "Internal server error")
                            )
                        }
                    }
                }

                authenticate("auth-bearer", optional = true) {
                    // Create invite
                    post("/{clanId}/invites") {
                        try {
                            val userId = call.userId.toString()
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid clan ID"))
                            val request = call.receive<CreateInviteRequest>()

                            val invite = clanService.createInvite(clanId, userId, request)
                            call.respond(HttpStatusCode.Created, SuccessResponse("Invite created successfully", invite))
                        } catch (e: IllegalStateException) {
                            call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "Forbidden"))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(e.message ?: "Internal server error")
                            )
                        }
                    }
                }

                authenticate("auth-bearer", optional = true) {
                    // Accept invite
                    post("/invites/accept") {
                        try {
                            val userId = call.userId.toString()
                            val request = call.receive<AcceptInviteRequest>()
                            val member = clanService.acceptInvite(userId, request.inviteCode)
                            call.respond(HttpStatusCode.OK, SuccessResponse("Invite accepted successfully", member))
                        } catch (e: IllegalStateException) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(e.message ?: "Invalid or expired invite")
                            )
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(e.message ?: "Internal server error")
                            )
                        }
                    }
                }

                authenticate("auth-bearer", optional = true) {
                    // Get pending join requests
                    get("/{clanId}/join-requests") {
                        try {
                            val userId = call.userId.toString()
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid clan ID"))

                            val requests = clanService.getPendingJoinRequests(clanId, userId)
                            call.respond(HttpStatusCode.OK, SuccessResponse("Join requests retrieved successfully", requests))
                        } catch (e: IllegalStateException) {
                            call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "Forbidden"))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(e.message ?: "Internal server error")
                            )
                        }
                    }
                }

                authenticate("auth-bearer", optional = true) {
                    // Review join request
                    post("/{clanId}/join-requests/{requestId}/review") {
                        try {
                            val userId = call.userId.toString()
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid clan ID"))
                            val requestId = call.parameters["requestId"]?.toLongOrNull()
                                ?: return@post call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponse("Invalid request ID")
                                )
                            val request = call.receive<ReviewJoinRequestRequest>()

                            val member = clanService.reviewJoinRequest(clanId, requestId, userId, request)
                            if (member != null) {
                                call.respond(HttpStatusCode.OK, SuccessResponse("Join request approved", member))
                            } else {
                                call.respond(HttpStatusCode.OK, SuccessResponse<Unit>("Join request rejected"))
                            }
                        } catch (e: IllegalStateException) {
                            call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "Forbidden"))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(e.message ?: "Internal server error")
                            )
                        }
                    }
                }

                authenticate("auth-bearer", optional = true) {
                    // Get clan badges
                    get("/{clanId}/badges") {
                        try {
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid clan ID"))

                            val badges = clanService.getClanDetails(clanId, null).badges
                            call.respond(HttpStatusCode.OK, SuccessResponse("Clan badges retrieved successfully", badges))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(e.message ?: "Internal server error")
                            )
                        }
                    }
                }

                authenticate("auth-bearer", optional = true) {
                    // Get clan stats
                    get("/{clanId}/stats") {
                        try {
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid clan ID"))

                            val stats = clanService.getClanStats(clanId)
                            call.respond(HttpStatusCode.OK, SuccessResponse("Clan stats retrieved successfully", stats))
                        } catch (e: IllegalStateException) {
                            call.respond(HttpStatusCode.NotFound, ErrorResponse(e.message ?: "Clan not found"))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(e.message ?: "Internal server error")
                            )
                        }
                    }
                }

                authenticate("auth-bearer") {
                    /**
                     * GET /api/clans/{clanId}/share
                     * Get shareable link for a clan (requires authentication)
                     * Path parameter: clanId
                     * Returns: Share link and deeplink for the clan with tracking code
                     */
                    get("/{clanId}/share") {
                        try {
                            val userId = call.userId.toString()
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: throw IllegalArgumentException("Invalid clan ID")
                            
                            // Validate clan exists
                            clanService.repository.getClanById(clanId)
                                ?: throw IllegalArgumentException("Clan not found")
                            
                            // Get base URL from request
                            val scheme = call.request.origin.scheme
                            val host = call.request.host()
                            val port = call.request.port()
                            val baseUrl = if (port == 80 || port == 443) {
                                "$scheme://$host"
                            } else {
                                "$scheme://$host:$port"
                            }
                            

                            val shareLink = clanService.getShareLink(clanId, userId, baseUrl)
                            call.respond(HttpStatusCode.OK, SuccessResponse("Share link retrieved successfully", shareLink))
                        } catch (e: IllegalArgumentException) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Invalid request"))
                        } catch (e: IllegalStateException) {
                            call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "Forbidden"))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse("Failed to get share link: ${e.message}")
                            )
                        }
                    }

                    /**
                     * GET /api/clans/{clanId}/invite-link
                     * Get permanent invite link for a clan (Admin/Moderator only)
                     */
                    get("/{clanId}/invite-link") {
                        try {
                            val userId = call.userId.toString()
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: throw IllegalArgumentException("Invalid clan ID")
                            
                            // Get base URL from request
                            val scheme = call.request.origin.scheme
                            val host = call.request.host()
                            val port = call.request.port()
                            val baseUrl = if (port == 80 || port == 443) {
                                "$scheme://$host"
                            } else {
                                "$scheme://$host:$port"
                            }
                            
                            val response = clanService.getPermanentInviteLink(clanId, userId, baseUrl)
                            call.respond(HttpStatusCode.OK, SuccessResponse("Invite link retrieved successfully", response))
                        } catch (e: IllegalArgumentException) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Invalid request"))
                        } catch (e: IllegalStateException) {
                            call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "Forbidden"))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse("Failed to get invite link: ${e.message}")
                            )
                        }
                    }
                    
                    /**
                     * POST /api/clans/share/track
                     * Track share event (join, app_open) (requires authentication)
                     * Request body: { "token": "encoded_token", "eventType": "JOIN", "deviceId": "device123" }
                     * OR: { "shareCode": "ABC123", "eventType": "JOIN", "deviceId": "device123" }
                     */
                    post("/share/track") {
                        try {
                            val userId = call.userId.toString()
                            val request = call.receive<TrackClanShareEventRequest>()
                            
                            val (clanId, shareCode) = if (request.token != null) {
                                val decoded = com.apptime.code.common.TokenEncoder.decodeClanShare(request.token)
                                    ?: throw IllegalArgumentException("Invalid token")
                                decoded
                            } else if (request.shareCode != null) {
                                val share = clanService.repository.getClanShareByCode(request.shareCode)
                                    ?: throw IllegalArgumentException("Invalid share code")
                                Pair(share.clanId, request.shareCode)
                            } else {
                                throw IllegalArgumentException("Either token or shareCode must be provided")
                            }
                            
                            // Track event
                            val userAgent = call.request.headers["User-Agent"]
                            val ipAddress = call.request.origin.remoteHost
                            clanService.repository.trackClanShareEvent(
                                shareCode = shareCode,
                                eventType = request.eventType,
                                joinerUserId = if (request.eventType == "JOIN") userId else null,
                                deviceId = request.deviceId,
                                userAgent = userAgent,
                                ipAddress = ipAddress
                            )
                            
                            //call.respond(HttpStatusCode.OK, SuccessResponse("Event tracked successfully"))
                        } catch (e: IllegalArgumentException) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Invalid request"))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse("Failed to track event: ${e.message}")
                            )
                        }
                    }
                    
                    /**
                     * GET /api/clans/share/stats
                     * Get share statistics for the authenticated user
                     */
                    get("/share/stats") {
                        try {
                            val userId = call.userId.toString()
                            val stats = clanService.repository.getUserClanShareStats(userId)
                            val response = ClanShareStatsResponse(
                                totalShares = stats.totalShares,
                                totalClicks = stats.totalClicks,
                                totalJoins = stats.totalJoins
                            )
                            call.respond(HttpStatusCode.OK, SuccessResponse("Share stats retrieved successfully", response))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse("Failed to get share stats: ${e.message}")
                            )
                        }
                    }
                }

                authenticate("auth-bearer", optional = true) {
                    /**
                     * GET /api/clans/{clanId}/analytics/app-usage
                     * Get app usage analytics for clan members
                     * Query params: period (optional, default: "daily") - "daily", "weekly", "monthly"
                     */
                    get("/{clanId}/analytics/app-usage") {
                        try {
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid clan ID"))
                            
                            val period = call.request.queryParameters["period"] ?: "daily"
                            
                            if (period !in listOf("daily", "weekly", "monthly")) {
                                return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid period. Must be daily, weekly, or monthly"))
                            }
                            
                            val analytics = clanService.getClanAppUsageAnalytics(clanId, period)
                            call.respond(HttpStatusCode.OK, SuccessResponse("App usage analytics retrieved successfully", analytics))
                        } catch (e: IllegalStateException) {
                            call.respond(HttpStatusCode.NotFound, ErrorResponse(e.message ?: "Clan not found"))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse("Failed to get analytics: ${e.message}")
                            )
                        }
                    }
                }
            }

        }
        

        // Root level routes (Web landing pages)
        get("/join-clan") {
            val token = call.request.queryParameters["token"]
            val inviteCode = call.request.queryParameters["inviteCode"]
            val clanId = call.request.queryParameters["clanId"]
            
            val deepLink = when {
                token != null && clanId != null -> "apptime://screen/clan_detail?clanId=$clanId&token=$token"
                token != null -> "apptime://screen/clan_detail?token=$token" // Fallback if clanId missing
                inviteCode != null -> "apptime://screen/join_clan?inviteCode=$inviteCode"
                else -> "apptime://screen/home"
            }
            
            call.respondText(
                contentType = ContentType.Text.Html,
                text = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Join Clan - AppTime</title>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
                            display: flex;
                            flex-direction: column;
                            align-items: center;
                            justify-content: center;
                            height: 100vh;
                            margin: 0;
                            background-color: #f5f5f7;
                            color: #1d1d1f;
                        }
                        .container {
                            background: white;
                            padding: 2rem;
                            border-radius: 1rem;
                            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                            text-align: center;
                            max-width: 90%;
                            width: 320px;
                        }
                        .logo {
                            margin-bottom: 1.5rem;
                            font-size: 3rem;
                        }
                        h1 {
                            font-size: 1.5rem;
                            margin-bottom: 0.5rem;
                        }
                        p {
                            color: #86868b;
                            margin-bottom: 2rem;
                        }
                        .btn {
                            display: inline-block;
                            background-color: #0071e3;
                            color: white;
                            padding: 12px 24px;
                            border-radius: 980px;
                            text-decoration: none;
                            font-weight: 500;
                            transition: background-color 0.2s;
                            width: 100%;
                            box-sizing: border-box;
                        }
                        .btn:hover {
                            background-color: #0077ed;
                        }
                    </style>
                    <script>
                        window.onload = function() {
                            // Try to open deep link immediately
                            window.location.href = "$deepLink";
                        }
                    </script>
                </head>
                <body>
                    <div class="container">
                        <div class="logo">🛡️</div>
                        <h1>Join Clan on AppTime</h1>
                        <p>You've been invited to join a clan. Tap the button below to open the app.</p>
                        <a href="$deepLink" class="btn">Open AppTime</a>
                    </div>
                </body>
                </html>
                """.trimIndent()
            )
        }
    }
}

