package com.apptime.code.common

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import users.UserRepository

/**
 * Authentication configuration
 */
fun Application.configureAuthentication() {
    val userRepository = UserRepository()

    install(Authentication) {
        bearer("auth-bearer") {
            realm = "AppTimeBackend API"
            authenticate { tokenCredential ->
                // The token is the encrypted userId
                val userId = tokenCredential.token
                if (userId.isNotBlank() && userRepository.userExists(userId) && !userRepository.isUserBlocked(userId)) {
                    UserIdPrincipal(userId)
                } else {
                    null
                }
            }
        }
    }
}

/**
 * Extension to get authenticated userId from call
 */
val ApplicationCall.userId: String?
    get() = principal<UserIdPrincipal>()?.name

/**
 * Extension to require authentication and get userId
 * Throws exception if not authenticated
 */
suspend fun ApplicationCall.requireUserId(): String {
    return userId ?: run {
        respond(
            HttpStatusCode.Unauthorized, mapOf(
                "success" to false,
                "status" to 401,
                "message" to "Authentication required. Please provide a valid Bearer token.",
                "error" to mapOf(
                    "code" to "UNAUTHORIZED",
                    "message" to "Invalid or missing Bearer token"
                )
            )
        )
        throw AuthenticationException("User not authenticated")
    }
}

/**
 * Custom exception for authentication
 */
class AuthenticationException(message: String) : Exception(message)

