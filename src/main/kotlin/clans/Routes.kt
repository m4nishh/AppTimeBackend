package com.apptime.code.clans

import com.apptime.code.common.userId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class SuccessResponse<T>(
    val message: String,
    val data: T? = null
)


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
                        call.respond(HttpStatusCode.Created, clan)
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

                        call.respond(HttpStatusCode.OK, response)
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
                        call.respond(HttpStatusCode.OK, response)
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
                        call.respond(HttpStatusCode.OK, response)
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
                            call.respond(HttpStatusCode.OK, response)
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
                            call.respond(HttpStatusCode.OK, clan)
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
                            call.respond(HttpStatusCode.OK, member)
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
                            call.respond(HttpStatusCode.OK, members)
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
                            call.respond(HttpStatusCode.Created, invite)
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
                            call.respond(HttpStatusCode.OK, member)
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
                            call.respond(HttpStatusCode.OK, requests)
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
                            call.respond(HttpStatusCode.OK, badges)
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
                            call.respond(HttpStatusCode.OK, stats)
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
            }
        }
    }}

