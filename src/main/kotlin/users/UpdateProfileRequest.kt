package users

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val username: String? = null,
    val firebaseToken: String? = null // Optional Firebase Cloud Messaging (FCM) token
)

