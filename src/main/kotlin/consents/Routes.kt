package com.apptime.code.consents

import com.apptime.code.common.respondApi
import com.apptime.code.common.respondError
import com.apptime.code.common.requireUserId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

/**
 * Configure consent-related routes
 */
fun Application.configureConsentRoutes() {
    val repository = ConsentRepository()
    val service = ConsentService(repository)
    
    routing {
        route("/api/consents") {
            /**
             * GET /api/consents
             * Get all available consent templates (public, no auth required)
             */
            get {
                try {
                    val templates = service.getConsentTemplates()
                    call.respondApi(templates, "Consent templates retrieved successfully")
                } catch (e: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, "Failed to retrieve consent templates: ${e.message}")
                }
            }
            
            /**
             * GET /api/consents/user
             * Get authenticated user's submitted consents (requires authentication)
             */
            authenticate("auth-bearer") {
                get("/user") {
                    try {
                        val userId = call.requireUserId()
                        val userConsents = service.getUserConsents(userId)
                        call.respondApi(userConsents, "User consents retrieved successfully")
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, "Failed to retrieve user consents: ${e.message}")
                    }
                }
                
                /**
                 * POST /api/consents
                 * Submit user consents (requires authentication)
                 */
                post {
                    try {
                        val userId = call.requireUserId()
                        val request = call.receive<ConsentSubmissionRequest>()
                        val results = service.submitConsents(userId, request)
                        call.respondApi(results, "Consents submitted successfully", HttpStatusCode.Created)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, "Failed to submit consents: ${e.message}")
                    }
                }
            }
        }
    }
}

