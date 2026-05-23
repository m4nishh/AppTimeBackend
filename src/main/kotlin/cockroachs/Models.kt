package com.apptime.code.cockroachs

import kotlinx.serialization.Serializable

@Serializable
data class EncryptedCockroachRequest(
    val encryptedData: String
)

@Serializable
data class CockroachRequest(
    val city: String,
    val email: String,
    val exact_lat: Double,
    val exact_lng: Double,
    val handle: String? = null,
    val joinedAt: Long,
    val name: String,
    val phone: String,
    val pincode: String? = null
)

@Serializable
data class CockroachResponse(
    val id: Long,
    val city: String,
    val email: String,
    val exact_lat: Double,
    val exact_lng: Double,
    val handle: String? = null,
    val joinedAt: Long,
    val name: String,
    val phone: String,
    val pincode: String? = null
)
