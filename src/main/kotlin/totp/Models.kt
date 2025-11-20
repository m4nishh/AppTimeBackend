package com.apptime.code.totp

import kotlinx.serialization.Serializable

@Serializable
data class TOTPVerifyRequest(
    val secret: String,
    val code: String,
    val tolerance: Int = 1
)

@Serializable
data class TOTPVerifyResponse(
    val valid: Boolean,
    val message: String? = null
)

@Serializable
data class TOTPGenerateResponse(
    val secret: String,
    val qrCodeUrl: String? = null
)

