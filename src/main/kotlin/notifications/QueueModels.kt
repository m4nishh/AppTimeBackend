package com.apptime.code.notifications

import kotlinx.serialization.Serializable

/**
 * Base interface for notification queue messages
 */
@Serializable
sealed class NotificationMessage {
    abstract val messageId: String
    abstract val timestamp: Long
}

/**
 * Message for challenge reward notification to winner
 */
@Serializable
data class ChallengeRewardNotificationMessage(
    override val messageId: String,
    override val timestamp: Long,
    val userId: String,
    val challengeTitle: String,
    val rank: Int,
    val coins: Long,
    val challengeId: Long
) : NotificationMessage()

/**
 * Message for challenge winner notification to other participants
 */
@Serializable
data class ChallengeWinnerNotificationMessage(
    override val messageId: String,
    override val timestamp: Long,
    val winnerUserId: String,
    val winnerUsername: String,
    val challengeTitle: String,
    val coins: Long,
    val challengeId: Long,
    val otherUserIds: List<String>
) : NotificationMessage()

