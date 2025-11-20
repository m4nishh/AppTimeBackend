package com.apptime.code.notifications

import kotlinx.serialization.Serializable

@Serializable
data class NotificationData(
    val id: Long? = null,
    val title: String,
    val image: String? = null,
    val text: String,
    val deeplink: String? = null,
    val type: String? = null,
    val createdAt: String? = null,
    val isRead: Boolean = false
)

@Serializable
data class SendNotificationRequest(
    val userId: String,
    val message: String,
    val type: String? = null // "daily_limit", "break_reminder", etc.
)

@Serializable
data class NotificationHistoryResponse(
    val notifications: List<NotificationData>,
    val totalCount: Int,
    val unreadCount: Int
)

