package com.apptime.code.common

import common.ApiResponse
import common.ResponseHelper
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Extension function to respond with ApiResponse
 * Supports translation via message key or direct message
 * 
 * Usage:
 * - call.respondApi(data, "message") - direct message
 * - call.respondApi(data, "message", HttpStatusCode.Created) - direct message with status
 * - call.respondApi(data, messageKey = MessageKeys.SUCCESS) - translated message
 * - call.respondApi(data, messageKey = MessageKeys.SUCCESS, statusCode = HttpStatusCode.Created) - translated with status
 */
suspend inline fun <reified T> ApplicationCall.respondApi(
    data: T? = null,
    message: String? = null,
    statusCode: HttpStatusCode = HttpStatusCode.OK,
    messageKey: String? = null
) {
    val translatedMessage = when {
        messageKey != null -> TranslationService.getMessage(messageKey, appLanguage, message)
        message != null -> message
        else -> null
    }
    
    @Suppress("UNCHECKED_CAST")
    val response: ApiResponse<T> = ResponseHelper.success(data as T, translatedMessage)
    respond(statusCode, response)
}

suspend fun ApplicationCall.respondError(
    statusCode: HttpStatusCode,
    message: String? = null,
    messageKey: String? = null,
    code: String? = null
) {
    val translatedMessage = when {
        messageKey != null -> TranslationService.getMessage(messageKey, appLanguage, message ?: "An error occurred")
        message != null -> message
        else -> "An error occurred"
    }
    
    val response: ApiResponse<Unit> = ResponseHelper.error(statusCode.value, translatedMessage, code)
    respond(statusCode, response)
}

/**
 * Extension to get X-App-Language header from request
 */
val ApplicationCall.appLanguage: String?
    get() = request.headers["X-App-Language"]

/**
 * Extension to get X-App-Version header from request
 */
val ApplicationCall.appVersion: String?
    get() = request.headers["X-App-Version"]

/**
 * Extension to require X-App-Language header
 * Returns the language or null if not present
 */
fun ApplicationCall.requireAppLanguage(): String? {
    return appLanguage
}

/**
 * Extension to require X-App-Version header
 * Returns the version or null if not present
 */
fun ApplicationCall.requireAppVersion(): String? {
    return appVersion
}

/**
 * Execute database transaction with error handling
 */
inline fun <T> dbTransaction(crossinline block: () -> T): T {
    return transaction {
        block()
    }
}

