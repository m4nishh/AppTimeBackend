package com.apptime.code.location

import com.apptime.code.common.MessageKeys
import com.apptime.code.common.respondApi
import com.apptime.code.common.respondError
import com.apptime.code.common.requireUserId
import users.UserRepository
import users.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

/**
 * Configure location-related routes
 */
fun Application.configureLocationRoutes() {
    val locationRepository = LocationRepository()
    val userRepository = UserRepository()
    val userService = UserService(userRepository)
    val service = LocationService(locationRepository, userRepository, userService)

    routing {
        route("/api/location") {
            /**
             * POST /api/location/sync
             * Sync location data from Android (requires authentication)
             * Fields: latitude, longitude, address, lastSyncTime
             */
            authenticate("auth-bearer") {
                post("/sync") {
                    try {
                        val userId = call.requireUserId()
                        val request = call.receive<LocationSyncRequest>()

                        val response = service.syncLocation(userId, request)
                        call.respondApi(response, message = response.message)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.LOCATION_SYNC_FAILED, message = "Failed to sync location: ${e.message}")
                    }
                }
            }

            /**
             * GET /api/location/user/{username}/last
             * Get last location data for a user by username
             * Requires authentication and valid TOTP verification session
             * Only accessible if User A has verified User B via TOTP
             */
            authenticate("auth-bearer") {
                get("/user/{username}/last") {
                    try {
                        val requestingUserId = call.requireUserId() // User A (authenticated)
                        val username = call.parameters["username"] // User B's username
                            ?: throw IllegalArgumentException("Username is required")

                        val response = service.getLocationByUsername(username, requestingUserId)
                        call.respondApi(response, messageKey = MessageKeys.LOCATION_RETRIEVED)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.INVALID_REQUEST, message = e.message)
                    } catch (e: SecurityException) {
                        call.respondError(HttpStatusCode.Forbidden, messageKey = MessageKeys.ACCESS_DENIED, message = e.message)
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, messageKey = MessageKeys.LOCATION_FAILED, message = "Failed to get location: ${e.message}")
                    }
                }
            }
        }
    }
}

