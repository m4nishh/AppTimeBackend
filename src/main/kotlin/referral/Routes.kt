package com.apptime.code.referral

import com.apptime.code.common.MessageKeys
import com.apptime.code.common.respondApi
import com.apptime.code.common.respondError
import com.apptime.code.common.requireUserId
import com.apptime.code.rewards.RewardRepository
import users.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

/**
 * Configure referral-related routes
 */
fun Application.configureReferralRoutes() {
    val referralRepository = ReferralRepository()
    val rewardRepository = RewardRepository()
    val userRepository = UserRepository()
    val service = ReferralService(referralRepository, rewardRepository, userRepository)
    
    routing {
        route("/api/referrals") {
            authenticate("auth-bearer") {
                /**
                 * GET /api/referrals/my-code
                 * Get or create the user's referral code
                 * Returns: UserReferralCode with code, stats, etc.
                 */
                get("/my-code") {
                    try {
                        val userId = call.requireUserId()
                        val response = service.getOrCreateReferralCode(userId)
                        call.respondApi(
                            response,
                            messageKey = MessageKeys.REFERRAL_CODE_RETRIEVED,
                            message = "Your referral code: ${response.referralCode}"
                        )
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            messageKey = MessageKeys.REFERRAL_CODE_FAILED,
                            message = "Failed to get referral code: ${e.message}"
                        )
                    }
                }
                
                /**
                 * POST /api/referrals/apply
                 * Apply a referral code (for new users during signup/onboarding)
                 * Request body: { "referralCode": "ABC123XYZ" }
                 */
                post("/apply") {
                    try {
                        val userId = call.requireUserId()
                        val request = call.receive<ApplyReferralCodeRequest>()
                        
                        val response = service.applyReferralCode(userId, request.referralCode)
                        call.respondApi(
                            response,
                            statusCode = HttpStatusCode.Created,
                            message = response.message
                        )
                    } catch (e: IllegalArgumentException) {
                        call.respondError(
                            HttpStatusCode.BadRequest,
                            messageKey = MessageKeys.INVALID_REQUEST,
                            message = e.message
                        )
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            messageKey = MessageKeys.REFERRAL_APPLY_FAILED,
                            message = "Failed to apply referral code: ${e.message}"
                        )
                    }
                }
                
                /**
                 * POST /api/referrals/complete
                 * Complete a referral (award coins to both users)
                 * This should be called when the referred user completes required action
                 * Request body: { "referredUserId": "user123" }
                 * 
                 * Note: In most cases, this will be called automatically by the system
                 * when the user completes onboarding or their first challenge
                 */
                post("/complete") {
                    try {
                        val request = call.receive<CompleteReferralRequest>()
                        
                        val response = service.completeReferral(request.referredUserId)
                        call.respondApi(
                            response,
                            message = response.message
                        )
                    } catch (e: IllegalArgumentException) {
                        call.respondError(
                            HttpStatusCode.BadRequest,
                            messageKey = MessageKeys.INVALID_REQUEST,
                            message = e.message
                        )
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            messageKey = MessageKeys.REFERRAL_COMPLETE_FAILED,
                            message = "Failed to complete referral: ${e.message}"
                        )
                    }
                }
                
                /**
                 * GET /api/referrals/my-info
                 * Get user's referral information and statistics
                 * Returns: MyReferralInfoResponse with code, stats, and list of referrals
                 */
                get("/my-info") {
                    try {
                        val userId = call.requireUserId()
                        val response = service.getMyReferralInfo(userId)
                        call.respondApi(
                            response,
                            messageKey = MessageKeys.REFERRAL_INFO_RETRIEVED
                        )
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            messageKey = MessageKeys.REFERRAL_INFO_FAILED,
                            message = "Failed to get referral info: ${e.message}"
                        )
                    }
                }
                
                /**
                 * GET /api/referrals/leaderboard
                 * Get referral leaderboard (top referrers)
                 * Query params:
                 *   - limit (optional) - Number of top referrers to return (default: 20, max: 20)
                 * Returns: ReferralLeaderboardResponse with top referrers and user's rank
                 */
                get("/leaderboard") {
                    try {
                        val userId = call.requireUserId()
                        val requestedLimit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                        // Cap the limit at 20
                        val limit = requestedLimit.coerceAtMost(20)
                        
                        val response = service.getReferralLeaderboard(userId, limit)
                        call.respondApi(
                            response,
                            messageKey = MessageKeys.LEADERBOARD_RETRIEVED
                        )
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            messageKey = MessageKeys.LEADERBOARD_FAILED,
                            message = "Failed to get referral leaderboard: ${e.message}"
                        )
                    }
                }
            }
            
            // Admin routes
            authenticate("auth-bearer") {
                route("/admin") {
                    /**
                     * GET /api/referrals/admin/all
                     * Get all referrals (admin only)
                     * Query params:
                     *   - status (optional) - Filter by status (PENDING, COMPLETED, REWARDED)
                     *   - limit (optional) - Limit results
                     *   - offset (optional) - Offset for pagination
                     */
                    get("/all") {
                        try {
                            // TODO: Add admin authorization check
                            val status = call.request.queryParameters["status"]?.let {
                                try {
                                    ReferralStatus.valueOf(it.uppercase())
                                } catch (e: IllegalArgumentException) {
                                    null
                                }
                            }
                            val limit = call.request.queryParameters["limit"]?.toIntOrNull()
                            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                            
                            val response = service.getAllReferrals(status, limit, offset)
                            call.respondApi(response)
                        } catch (e: Exception) {
                            call.respondError(
                                HttpStatusCode.InternalServerError,
                                message = "Failed to get all referrals: ${e.message}"
                            )
                        }
                    }
                    
                    /**
                     * POST /api/referrals/admin/complete/{referralId}
                     * Manually complete a referral (admin only)
                     * Path param: referralId
                     */
                    post("/complete/{referralId}") {
                        try {
                            // TODO: Add admin authorization check
                            val referralId = call.parameters["referralId"]?.toLongOrNull()
                                ?: throw IllegalArgumentException("Invalid referral ID")
                            
                            val response = service.adminCompleteReferral(referralId)
                            call.respondApi(
                                response,
                                message = response.message
                            )
                        } catch (e: IllegalArgumentException) {
                            call.respondError(
                                HttpStatusCode.BadRequest,
                                message = e.message
                            )
                        } catch (e: Exception) {
                            call.respondError(
                                HttpStatusCode.InternalServerError,
                                message = "Failed to complete referral: ${e.message}"
                            )
                        }
                    }
                }
            }
        }
    }
}

