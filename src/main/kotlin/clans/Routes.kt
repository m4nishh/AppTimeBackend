package com.apptime.code.clans

import com.apptime.code.common.respondApi
import com.apptime.code.common.respondError
import com.apptime.code.common.userId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*


fun Application.configureClanRoutes() {
    val clanService = ClanService()
    
    routing {
        route("/api/clans") {
            authenticate("auth-bearer", optional = true) {
                // Create a clan
                post {
                    try {
                        val userId = call.userId.toString();
                        val request = call.receive<CreateClanRequest>()
                        val clan = clanService.createClan(userId, request)
                        call.respondApi(clan, "Clan created successfully", HttpStatusCode.Created)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: IllegalStateException) {
                        call.respondError(HttpStatusCode.Conflict, e.message ?: "Conflict")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
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

                        call.respondApi(response, "Clans retrieved successfully")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
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
                        call.respondApi(response, "Clan leaderboard retrieved successfully")
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
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
                        call.respondApi(response, "User clan info retrieved successfully")
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            e.message ?: "Internal server error"
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
                                ?: return@get call.respondError(HttpStatusCode.BadRequest, "Invalid clan ID")

                            val response = clanService.getClanDetails(clanId, userId)
                            call.respondApi(response, "Clan details retrieved successfully")
                        } catch (e: IllegalStateException) {
                            call.respondError(HttpStatusCode.NotFound, e.message ?: "Clan not found")
                        } catch (e: Exception) {
                            call.respondError(
                                HttpStatusCode.InternalServerError,
                                e.message ?: "Internal server error"
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
                                ?: return@patch call.respondError(HttpStatusCode.BadRequest, "Invalid clan ID")
                            val request = call.receive<UpdateClanRequest>()

                            val clan = clanService.updateClan(clanId, userId, request)
                            call.respondApi(clan, "Clan updated successfully")
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


                authenticate("auth-bearer", optional = true) {
                    // Delete clan
                    delete("/{clanId}") {
                        try {
                            val userId = call.userId.toString()
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@delete call.respondError(HttpStatusCode.BadRequest, "Invalid clan ID")

                            clanService.deleteClan(clanId, userId)
                            call.respondApi("", "Clan deleted successfully")
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


                authenticate("auth-bearer", optional = true) {
                    // Join a clan
                    post("/join") {
                        try {
                            val userId = call.userId.toString()
                            val request = call.receive<JoinClanRequest>()
                            val member = clanService.joinClan(userId, request)
                            call.respondApi(member, "Joined clan successfully")
                        } catch (e: IllegalArgumentException) {
                            call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                        } catch (e: IllegalStateException) {
                            call.respondError(HttpStatusCode.Conflict, e.message ?: "Conflict")
                        } catch (e: Exception) {
                            call.respondError(
                                HttpStatusCode.InternalServerError,
                                e.message ?: "Internal server error"
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
                                ?: return@post call.respondError(HttpStatusCode.BadRequest, "Invalid clan ID")

                            clanService.leaveClan(userId, clanId)
                            call.respondApi("", "Left clan successfully")
                        } catch (e: IllegalStateException) {
                            call.respondError(HttpStatusCode.Conflict, e.message ?: "Conflict")
                        } catch (e: Exception) {
                            call.respondError(
                                HttpStatusCode.InternalServerError,
                                e.message ?: "Internal server error"
                            )
                        }
                    }
                }

                authenticate("auth-bearer", optional = true) {
                    // Get clan members
                    get("/{clanId}/members") {
                        try {
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@get call.respondError(HttpStatusCode.BadRequest, "Invalid clan ID")

                            val members = clanService.getClanDetails(clanId, null).members
                            call.respondApi(members, "Clan members retrieved successfully")
                        } catch (e: Exception) {
                            call.respondError(
                                HttpStatusCode.InternalServerError,
                                e.message ?: "Internal server error"
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
                                ?: return@patch call.respondError(HttpStatusCode.BadRequest, "Invalid clan ID")
                            val request = call.receive<UpdateMemberRoleRequest>()

                            clanService.updateMemberRole(clanId, userId, request)
                            call.respondApi("", "Member role updated successfully")
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

                authenticate("auth-bearer", optional = true) {
                    // Remove member
                    post("/{clanId}/members/remove") {
                        try {
                            val userId = call.userId.toString()
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@post call.respondError(HttpStatusCode.BadRequest, "Invalid clan ID")
                            val request = call.receive<RemoveMemberRequest>()

                            clanService.removeMember(clanId, userId, request)
                            call.respondApi("", "Member removed successfully")
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

                authenticate("auth-bearer", optional = true) {
                    // Create invite
                    post("/{clanId}/invites") {
                        try {
                            val userId = call.userId.toString()
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@post call.respondError(HttpStatusCode.BadRequest, "Invalid clan ID")
                            val request = call.receive<CreateInviteRequest>()

                            val invite = clanService.createInvite(clanId, userId, request)
                            call.respondApi(invite, "Invite created successfully", HttpStatusCode.Created)
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

                authenticate("auth-bearer", optional = true) {
                    // Accept invite
                    post("/invites/accept") {
                        try {
                            val userId = call.userId.toString()
                            val request = call.receive<AcceptInviteRequest>()
                            val member = clanService.acceptInvite(userId, request.inviteCode)
                            call.respondApi(member, "Invite accepted successfully")
                        } catch (e: IllegalStateException) {
                            call.respondError(
                                HttpStatusCode.BadRequest,
                                e.message ?: "Invalid or expired invite"
                            )
                        } catch (e: Exception) {
                            call.respondError(
                                HttpStatusCode.InternalServerError,
                                e.message ?: "Internal server error"
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
                                ?: return@get call.respondError(HttpStatusCode.BadRequest, "Invalid clan ID")

                            val requests = clanService.getPendingJoinRequests(clanId, userId)
                            call.respondApi(requests, "Join requests retrieved successfully")
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

                authenticate("auth-bearer", optional = true) {
                    // Review join request
                    post("/{clanId}/join-requests/{requestId}/review") {
                        try {
                            val userId = call.userId.toString()
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@post call.respondError(HttpStatusCode.BadRequest, "Invalid clan ID")
                            val requestId = call.parameters["requestId"]?.toLongOrNull()
                                ?: return@post call.respondError(HttpStatusCode.BadRequest, "Invalid request ID")
                            val request = call.receive<ReviewJoinRequestRequest>()

                            val member = clanService.reviewJoinRequest(clanId, requestId, userId, request)
                            if (member != null) {
                                call.respondApi(member, "Join request approved")
                            } else {
                                call.respondApi("", "Join request rejected")
                            }
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

                authenticate("auth-bearer", optional = true) {
                    // Get clan badges
                    get("/{clanId}/badges") {
                        try {
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@get call.respondError(HttpStatusCode.BadRequest, "Invalid clan ID")

                            val badges = clanService.getClanDetails(clanId, null).badges
                            call.respondApi(badges, "Clan badges retrieved successfully")
                        } catch (e: Exception) {
                            call.respondError(
                                HttpStatusCode.InternalServerError,
                                e.message ?: "Internal server error"
                            )
                        }
                    }
                }

                authenticate("auth-bearer", optional = true) {
                    // Get clan stats
                    get("/{clanId}/stats") {
                        try {
                            val clanId = call.parameters["clanId"]?.toLongOrNull()
                                ?: return@get call.respondError(HttpStatusCode.BadRequest, "Invalid clan ID")

                            val stats = clanService.getClanStats(clanId)
                            call.respondApi(stats, "Clan stats retrieved successfully")
                        } catch (e: IllegalStateException) {
                            call.respondError(HttpStatusCode.NotFound, e.message ?: "Clan not found")
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
    }}

