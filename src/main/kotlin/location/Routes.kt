package com.apptime.code.location

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
                        call.respondApi(response, response.message, HttpStatusCode.OK)
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, "Failed to sync location: ${e.message}")
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
                        call.respondApi(response, "Last location retrieved successfully")
                    } catch (e: IllegalArgumentException) {
                        call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    } catch (e: SecurityException) {
                        call.respondError(HttpStatusCode.Forbidden, e.message ?: "Access denied")
                    } catch (e: Exception) {
                        call.respondError(HttpStatusCode.InternalServerError, "Failed to get location: ${e.message}")
                    }
                }
            }
        }
    }
}

