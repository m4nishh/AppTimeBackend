package com.apptime.code.features

import com.apptime.code.common.MessageKeys
import com.apptime.code.common.respondApi
import com.apptime.code.common.respondError
import com.apptime.code.common.userId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

/**
 * Configure feature flags routes
 */
fun Application.configureFeatureFlagsRoutes() {
    val repository = FeatureFlagsRepository()
    val service = FeatureFlagsService(repository)
    
    routing {
        route("/api/features") {
            /**
             * GET /api/features
             * Get all feature flags (public endpoint for frontend)
             * Query params: country, appVersion, language (optional)
             * Headers: X-App-Language, X-App-Version (used if query params not provided)
             * Returns a simple map of featureName -> isEnabled (evaluated based on conditions)
             */
            get {
                try {
                    // Get parameters from query params or headers
                    val country = call.request.queryParameters["country"]
                        ?: call.request.headers["X-Country"]
                    val appVersion = call.request.queryParameters["appVersion"]
                        ?: call.request.headers["X-App-Version"]
                    val language = call.request.queryParameters["language"]
                        ?: call.request.headers["X-App-Language"]
                    val userId = call.userId // Get from auth if available
                    
                    val params = FeatureEvaluationParams(
                        country = country,
                        appVersion = appVersion,
                        language = language,
                        userId = userId
                    )
                    
                    val response = service.getFeatureFlagsMap(params)
                    call.respondApi(response, messageKey = MessageKeys.FEATURE_FLAGS_RETRIEVED)
                } catch (e: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.FEATURE_FLAGS_FAILED, message = "Failed to retrieve feature flags: ${e.message}")
                }
            }
            
            /**
             * GET /api/features/{featureName}
             * Get a specific feature flag by name (public endpoint)
             * Query params: country, appVersion, language (optional)
             * Returns the evaluated isEnabled status based on conditions
             */
            get("/{featureName}") {
                try {
                    val featureName = call.parameters["featureName"]
                        ?: throw IllegalArgumentException("Feature name is required")
                    
                    // Get parameters from query params or headers
                    val country = call.request.queryParameters["country"]
                        ?: call.request.headers["X-Country"]
                    val appVersion = call.request.queryParameters["appVersion"]
                        ?: call.request.headers["X-App-Version"]
                    val language = call.request.queryParameters["language"]
                        ?: call.request.headers["X-App-Language"]
                    val userId = call.userId
                    
                    val params = FeatureEvaluationParams(
                        country = country,
                        appVersion = appVersion,
                        language = language,
                        userId = userId
                    )
                    
                    val feature = service.getFeatureFlagByName(featureName)
                    if (feature != null) {
                        // Evaluate the feature based on conditions
                        val isEnabled = ConditionEvaluator.evaluateFeature(
                            feature.isEnabled,
                            feature.conditions,
                            params
                        )
                        val evaluatedFeature = feature.copy(isEnabled = isEnabled)
                        call.respondApi(evaluatedFeature, messageKey = MessageKeys.FEATURE_FLAG_RETRIEVED)
                    } else {
                        call.respondError(HttpStatusCode.NotFound, messageKey = MessageKeys.FEATURE_FLAG_NOT_FOUND, message = "Feature flag '$featureName' not found")
                    }
                } catch (e: IllegalArgumentException) {
                    call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                } catch (e: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.FEATURE_FLAGS_FAILED, message = "Failed to retrieve feature flag: ${e.message}")
                }
            }
        }
        
        // Admin routes for managing feature flags
        route("/api/admin/features") {
            /**
             * GET /api/admin/features
             * Get all feature flags with details (admin endpoint)
             */
            get {
                try {
                    val features = service.getAllFeatureFlags()
                    call.respondApi(features, messageKey = MessageKeys.FEATURE_FLAGS_RETRIEVED)
                } catch (e: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.FEATURE_FLAGS_FAILED, message = "Failed to retrieve feature flags: ${e.message}")
                }
            }
            
            /**
             * GET /api/admin/features/{featureName}
             * Get a specific feature flag by name (admin endpoint)
             */
            get("/{featureName}") {
                try {
                    val featureName = call.parameters["featureName"]
                        ?: throw IllegalArgumentException("Feature name is required")
                    
                    val feature = service.getFeatureFlagByName(featureName)
                    if (feature != null) {
                        call.respondApi(feature, messageKey = MessageKeys.FEATURE_FLAG_RETRIEVED)
                    } else {
                        call.respondError(HttpStatusCode.NotFound, messageKey = MessageKeys.FEATURE_FLAG_NOT_FOUND, message = "Feature flag '$featureName' not found")
                    }
                } catch (e: IllegalArgumentException) {
                    call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                } catch (e: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.FEATURE_FLAGS_FAILED, message = "Failed to retrieve feature flag: ${e.message}")
                }
            }
            
            /**
             * POST /api/admin/features
             * Create a new feature flag (admin endpoint)
             */
            post {
                try {
                    val request = call.receive<CreateFeatureFlagRequest>()
                    val feature = service.createFeatureFlag(request)
                    call.respondApi(feature, statusCode = HttpStatusCode.Created, messageKey = MessageKeys.FEATURE_FLAG_CREATED)
                } catch (e: IllegalArgumentException) {
                    call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                } catch (e: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.FEATURE_FLAG_CREATE_FAILED, message = "Failed to create feature flag: ${e.message}")
                }
            }
            
            /**
             * PUT /api/admin/features/{featureName}
             * Update a feature flag (admin endpoint)
             */
            put("/{featureName}") {
                try {
                    val featureName = call.parameters["featureName"]
                        ?: throw IllegalArgumentException("Feature name is required")
                    val request = call.receive<UpdateFeatureFlagRequest>()
                    val feature = service.updateFeatureFlag(featureName, request)
                    call.respondApi(feature, messageKey = MessageKeys.FEATURE_FLAG_UPDATED)
                } catch (e: IllegalArgumentException) {
                    call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                } catch (e: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.FEATURE_FLAG_UPDATE_FAILED, message = "Failed to update feature flag: ${e.message}")
                }
            }
            
            /**
             * DELETE /api/admin/features/{featureName}
             * Delete a feature flag (admin endpoint)
             */
            delete("/{featureName}") {
                try {
                    val featureName = call.parameters["featureName"]
                        ?: throw IllegalArgumentException("Feature name is required")
                    val deleted = service.deleteFeatureFlag(featureName)
                    if (deleted) {
                        call.respondApi("", messageKey = MessageKeys.FEATURE_FLAG_DELETED)
                    } else {
                        call.respondError(HttpStatusCode.NotFound, messageKey = MessageKeys.FEATURE_FLAG_NOT_FOUND, message = "Feature flag '$featureName' not found")
                    }
                } catch (e: IllegalArgumentException) {
                    call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                } catch (e: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.FEATURE_FLAG_DELETE_FAILED, message = "Failed to delete feature flag: ${e.message}")
                }
            }
        }
    }
}

