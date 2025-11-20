package com.apptime.code.consents

import kotlinx.serialization.Serializable

@Serializable
data class ConsentTemplate(
    val id: Int,
    val name: String,
    val description: String? = null,
    val isMandatory: Boolean
)

@Serializable
data class ConsentSubmissionItem(
    val id: Int,
    val value: String // "accepted" or "rejected"
)

@Serializable
data class ConsentSubmissionRequest(
    val consents: List<ConsentSubmissionItem>
)

@Serializable
data class ConsentSubmissionResponseItem(
    val id: Int,
    val consentId: Int? = null,
    val consentName: String? = null,
    val value: String? = null,
    val submittedAt: String? = null
)

@Serializable
data class ConsentResponse(
    val username: String? = null,
    val hasConsent: Boolean? = null,
    val dataSharing: Boolean? = null,
    val analytics: Boolean? = null,
    val marketing: Boolean? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

