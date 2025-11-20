package common

import kotlinx.serialization.Serializable

/**
 * Standard API response wrapper
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean = true,
    val status: Int = 200,
    val data: T? = null,
    val message: String? = null,
    val timestamp: String = java.time.Instant.now().toString(),
    val error: ApiError? = null
)

/**
 * API error model
 */
@Serializable
data class ApiError(
    val code: String? = null,
    val message: String? = null,
    val details: Map<String, String>? = null
)

/**
 * Helper functions for creating responses
 */
object ResponseHelper {
    fun <T> success(data: T, message: String? = null): ApiResponse<T> {
        return ApiResponse(success = true, status = 200, data = data, message = message)
    }
    
    fun <T> error(status: Int, message: String, code: String? = null): ApiResponse<T> {
        return ApiResponse(
            success = false,
            status = status,
            data = null,
            message = message,
            error = ApiError(code = code, message = message)
        )
    }
}

