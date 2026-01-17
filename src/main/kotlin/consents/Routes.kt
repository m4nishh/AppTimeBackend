package com.apptime.code.consents

import com.apptime.code.common.MessageKeys
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
                    call.respondApi(templates, messageKey = MessageKeys.CONSENT_TEMPLATES_RETRIEVED)
                } catch (e: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.CONSENT_TEMPLATES_FAILED, message = "Failed to retrieve consent templates: ${e.message}")
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
                        call.respondApi(userConsents, messageKey = MessageKeys.USER_CONSENTS_RETRIEVED)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.USER_CONSENTS_FAILED, message = "Failed to retrieve user consents: ${e.message}")
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
                        call.respondApi(results, statusCode = HttpStatusCode.Created, messageKey = MessageKeys.CONSENTS_SUBMITTED)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.CONSENTS_SUBMIT_FAILED, message = "Failed to submit consents: ${e.message}")
                    }
                }
            }
        }
    }
}

