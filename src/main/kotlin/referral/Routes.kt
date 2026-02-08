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
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import java.net.URLEncoder

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
                 * GET /api/referrals/share
                 * Get shareable link for referral code (requires authentication)
                 * Returns: Share link and deeplink with encrypted token
                 */
                get("/share") {
                    try {
                        val userId = call.requireUserId()
                        
                        // Get or create referral code
                        val referralCode = referralRepository.ensureUserHasReferralCode(userId)
                        
                        // Get base URL from request
                        val scheme = call.request.origin.scheme
                        val host = call.request.host()
                        val port = call.request.port()
                        val baseUrl = if (port == 80 || port == 443) {
                            "$scheme://$host"
                        } else {
                            "$scheme://$host:$port"
                        }
                        
                        // Encode referral code into a secure token
                        val token = com.apptime.code.common.TokenEncoder.encodeReferral(referralCode)
                        val encodedToken = URLEncoder.encode(token, "UTF-8")
                        
                        // Use token instead of revealing referral code
                        val shareLink = "$baseUrl/referral/$encodedToken"
                        val deeplink = "apptime://screen/referral/$encodedToken"
                        
                        val response = ReferralShareLinkResponse(
                            referralCode = referralCode,
                            shareLink = shareLink,
                            deeplink = deeplink
                        )
                        
                        call.respondApi(response, messageKey = MessageKeys.REFERRAL_CODE_RETRIEVED)
                    } catch (e: Exception) {
                        call.respondError(
                            HttpStatusCode.InternalServerError,
                            messageKey = MessageKeys.REFERRAL_CODE_FAILED,
                            message = "Failed to get share link: ${e.message}"
                        )
                    }
                }
                
                /**
                 * POST /api/referrals/apply
                 * Apply a referral code (for new users during signup/onboarding)
                 * Request body: { "token": "encoded_token" } OR { "referralCode": "ABC123XYZ" }
                 */
                post("/apply") {
                    try {
                        val userId = call.requireUserId()
                        val request = call.receive<ApplyReferralCodeRequest>()
                        
                        // Get referral code from token or direct referralCode
                        val referralCode = when {
                            request.referralCode != null -> {
                                // Explicit referral code provided
                                request.referralCode
                            }
                            request.token != null -> {
                                // Try to decode token first
                                com.apptime.code.common.TokenEncoder.decodeReferral(request.token)
                                    ?: request.token // Fallback: treat token value as referral code (backward compatibility)
                            }
                            else -> {
                                throw IllegalArgumentException("Either token or referralCode must be provided")
                            }
                        }
                        
                        val response = service.applyReferralCode(userId, referralCode)
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

