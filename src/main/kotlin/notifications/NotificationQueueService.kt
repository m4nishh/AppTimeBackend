package com.apptime.code.notifications

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Logger

/**
 * Service for managing notification message queue
 * Uses in-memory queue with coroutines for async processing
 * Can be easily replaced with Redis Lists (LPUSH/RPOP) for distributed systems
 */
object NotificationQueueService {
    private val logger = Logger.getLogger(NotificationQueueService::class.java.name)
    
    // In-memory queue for notification messages
    private val queue = ConcurrentLinkedQueue<NotificationMessage>()
    
    // Channel for coroutine-based processing
    private val channel = Channel<NotificationMessage>(Channel.UNLIMITED)
    
    // Mutex for thread-safe operations
    private val mutex = Mutex()
    
    // Statistics
    private var totalEnqueued = 0L
    private var totalProcessed = 0L
    private var totalFailed = 0L
    @Volatile private var started = false
    
    /**
     * Enqueue a notification message
     */
    suspend fun enqueue(message: NotificationMessage) {
        mutex.withLock {
            queue.offer(message)
            channel.send(message)
            totalEnqueued++
            logger.info("✅ Notification message enqueued: ${message.messageId} (type=${message::class.simpleName}, Queue size: ${queue.size}, Total enqueued: $totalEnqueued)")
        }
    }
    
    /**
     * Enqueue challenge reward notification
     */
    suspend fun enqueueChallengeRewardNotification(
        userId: String,
        challengeTitle: String,
        rank: Int,
        coins: Long,
        challengeId: Long
    ) {
        val message = ChallengeRewardNotificationMessage(
            messageId = UUID.randomUUID().toString(),
            timestamp = Clock.System.now().toEpochMilliseconds(),
            userId = userId,
            challengeTitle = challengeTitle,
            rank = rank,
            coins = coins,
            challengeId = challengeId
        )
        enqueue(message)
    }
    
    /**
     * Enqueue challenge winner notification to other participants
     */
    suspend fun enqueueChallengeWinnerNotification(
        winnerUserId: String,
        winnerUsername: String,
        challengeTitle: String,
        coins: Long,
        challengeId: Long,
        otherUserIds: List<String>
    ) {
        val message = ChallengeWinnerNotificationMessage(
            messageId = UUID.randomUUID().toString(),
            timestamp = Clock.System.now().toEpochMilliseconds(),
            winnerUserId = winnerUserId,
            winnerUsername = winnerUsername,
            challengeTitle = challengeTitle,
            coins = coins,
            challengeId = challengeId,
            otherUserIds = otherUserIds
        )
        enqueue(message)
    }
    
    /**
     * Get queue size
     */
    fun getQueueSize(): Int {
        return queue.size
    }
    
    /**
     * Get statistics
     */
    fun getStatistics(): QueueStatistics {
        return QueueStatistics(
            queueSize = queue.size,
            totalEnqueued = totalEnqueued,
            totalProcessed = totalProcessed,
            totalFailed = totalFailed
        )
    }
    
    /**
     * Start consuming messages from the queue.
     *
     * Important:
     * - This is safe to call multiple times; only the first call will start workers.
     * - Workers run on Dispatchers.IO because sending notifications may do network IO.
     */
    fun startConsumer(
        notificationService: NotificationService,
        scope: CoroutineScope,
        maxConcurrentWorkers: Int = 5
    ) {
        if (started) {
            logger.info("Notification queue consumer already started (workers=$maxConcurrentWorkers).")
            return
        }
        started = true

        logger.info("Starting notification queue consumer with $maxConcurrentWorkers workers...")

        repeat(maxConcurrentWorkers) { workerId ->
            scope.launch(Dispatchers.IO) {
                processMessages(notificationService, workerId)
            }
        }

        logger.info("Notification queue consumer started successfully")
    }
    
    // Note: we intentionally do not expose stop() yet, because worker lifecycles are owned by the
    // passed-in CoroutineScope. When that scope is cancelled (application shutdown), workers stop.
    
    /**
     * Process messages from the channel
     */
    private suspend fun processMessages(
        notificationService: NotificationService,
        workerId: Int
    ) {
        logger.info("Notification queue worker $workerId started")
        
        try {
            for (message in channel) {
                try {
                    when (message) {
                        is ChallengeRewardNotificationMessage -> {
                            logger.info("Worker $workerId processing challenge reward notification: ${message.messageId} for user ${message.userId}")
                            try {
                                notificationService.sendChallengeRewardNotification(
                                    userId = message.userId,
                                    challengeTitle = message.challengeTitle,
                                    rank = message.rank,
                                    coins = message.coins,
                                    challengeId = message.challengeId
                                )
                                logger.info("Worker $workerId successfully sent challenge reward notification to user ${message.userId}")
                            } catch (e: Exception) {
                                logger.severe("Worker $workerId failed to send challenge reward notification to user ${message.userId}: ${e.message}")
                                logger.severe("Exception: ${e.stackTraceToString()}")
                                throw e // Re-throw to be caught by outer catch block
                            }
                            mutex.withLock {
                                queue.remove(message)
                                totalProcessed++
                            }
                            logger.info("Worker $workerId successfully processed challenge reward notification: ${message.messageId}")
                        }
                        
                        is ChallengeWinnerNotificationMessage -> {
                            logger.info("Worker $workerId processing challenge winner notification: ${message.messageId} for ${message.otherUserIds.size} users")
                            try {
                                notificationService.sendChallengeWinnerNotificationToOthers(
                                    winnerUserId = message.winnerUserId,
                                    winnerUsername = message.winnerUsername,
                                    challengeTitle = message.challengeTitle,
                                    coins = message.coins,
                                    challengeId = message.challengeId,
                                    otherUserIds = message.otherUserIds
                                )
                                logger.info("Worker $workerId successfully sent challenge winner notifications to ${message.otherUserIds.size} users")
                            } catch (e: Exception) {
                                logger.severe("Worker $workerId failed to send challenge winner notifications: ${e.message}")
                                logger.severe("Exception: ${e.stackTraceToString()}")
                                throw e // Re-throw to be caught by outer catch block
                            }
                            mutex.withLock {
                                queue.remove(message)
                                totalProcessed++
                            }
                            logger.info("Worker $workerId successfully processed challenge winner notification: ${message.messageId}")
                        }
                    }
                } catch (e: Exception) {
                    logger.severe("Worker $workerId failed to process notification ${message.messageId}: ${e.message}")
                    logger.severe("Exception details: ${e.stackTraceToString()}")
                    mutex.withLock {
                        totalFailed++
                        // Optionally, you could implement retry logic here
                        // For now, we just log and continue
                    }
                }
            }
        } catch (e: Exception) {
            logger.severe("Worker $workerId encountered fatal error: ${e.message}")
        }
    }
    
    /**
     * Clear the queue (for testing/debugging)
     */
    suspend fun clearQueue() {
        mutex.withLock {
            queue.clear()
            logger.info("Notification queue cleared")
        }
    }
}

/**
 * Queue statistics
 */
@Serializable
data class QueueStatistics(
    val queueSize: Int,
    val totalEnqueued: Long,
    val totalProcessed: Long,
    val totalFailed: Long
)

