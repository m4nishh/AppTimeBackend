package com.apptime.code.rewards

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

/**
 * In-process message queue for awarding challenge rewards (coins) asynchronously.
 *
 * Why:
 * - Avoid doing heavy reward calculations inside scheduled jobs / request handlers
 * - Make awarding resilient and smooth under load
 *
 * Note:
 * - This is an in-memory queue (not durable). For multi-instance deployments, move to Redis/DB outbox.
 * - Awarding is idempotent because RewardService checks existing rewards/coins before inserting.
 */
object ChallengeRewardsQueueService {
    private val logger = Logger.getLogger(ChallengeRewardsQueueService::class.java.name)

    private val channel = Channel<Long>(Channel.UNLIMITED) // challengeId
    @Volatile private var started = false

    // Basic dedupe so we don't spam the same challengeId too much in a short period.
    // (Idempotency still exists in RewardService, this just reduces load.)
    private val inFlight = ConcurrentHashMap.newKeySet<Long>()

    private var totalEnqueued = 0L
    private var totalProcessed = 0L
    private var totalFailed = 0L

    @Serializable
    data class Stats(
        val totalEnqueued: Long,
        val totalProcessed: Long,
        val totalFailed: Long,
        val inFlightCount: Int
    )

    fun stats(): Stats = Stats(
        totalEnqueued = totalEnqueued,
        totalProcessed = totalProcessed,
        totalFailed = totalFailed,
        inFlightCount = inFlight.size
    )

    /**
     * Enqueue awarding for a challenge (safe to call multiple times).
     */
    fun enqueue(challengeId: Long) {
        if (!inFlight.add(challengeId)) {
            logger.info("⚠️ Challenge $challengeId already in flight, skipping duplicate enqueue")
            return
        }
        val result = channel.trySend(challengeId)
        if (result.isSuccess) {
            totalEnqueued++
            logger.info("✅ Enqueued challenge reward awarding: challengeId=$challengeId (inFlight=${inFlight.size}, totalEnqueued=$totalEnqueued)")
            println("✅ [ChallengeRewardsQueue] Enqueued challengeId=$challengeId")
        } else {
            logger.severe("❌ FAILED to enqueue challengeId=$challengeId - channel closed or full")
            println("❌ [ChallengeRewardsQueue] FAILED to enqueue challengeId=$challengeId")
            inFlight.remove(challengeId) // Clean up since we failed
        }
    }

    /**
     * Start background workers (safe to call multiple times).
     */
    fun startConsumer(
        scope: CoroutineScope,
        rewardService: RewardService,
        maxConcurrentWorkers: Int = 2,
        topNRanks: Int = 10
    ) {
        if (started) {
            logger.info("ChallengeRewardsQueueService already started.")
            return
        }
        started = true

        logger.info("Starting ChallengeRewardsQueueService with workers=$maxConcurrentWorkers ...")
        println("🚀 [ChallengeRewardsQueue] Starting with $maxConcurrentWorkers workers...")
        repeat(maxConcurrentWorkers) { workerId ->
            scope.launch(Dispatchers.IO) {
                try {
                    logger.info("ChallengeRewardsQueueService worker $workerId started")
                    println("✅ [ChallengeRewardsQueue] Worker $workerId started and waiting for messages...")
                    for (challengeId in channel) {
                        try {
                            logger.info("Worker $workerId awarding challenge rewards: challengeId=$challengeId")
                            println("🔄 [ChallengeRewardsQueue] Worker $workerId processing challengeId=$challengeId")
                            rewardService.awardChallengeRewards(challengeId, topNRanks = topNRanks)
                            totalProcessed++
                            logger.info("Worker $workerId successfully awarded rewards for challengeId=$challengeId")
                            println("✅ [ChallengeRewardsQueue] Worker $workerId successfully awarded rewards for challengeId=$challengeId")
                        } catch (e: Exception) {
                            totalFailed++
                            logger.severe("Worker $workerId failed awarding challengeId=$challengeId: ${e.message}")
                            logger.severe("Exception stack trace: ${e.stackTraceToString()}")
                            println("❌ [ChallengeRewardsQueue] Worker $workerId FAILED for challengeId=$challengeId: ${e.message}")
                            e.printStackTrace()
                            // Continue processing other challenges
                        } finally {
                            inFlight.remove(challengeId)
                        }
                    }
                } catch (e: Exception) {
                    logger.severe("ChallengeRewardsQueueService worker $workerId encountered fatal error: ${e.message}")
                    logger.severe("Fatal exception stack trace: ${e.stackTraceToString()}")
                    println("❌ [ChallengeRewardsQueue] Worker $workerId FATAL ERROR: ${e.message}")
                    e.printStackTrace()
                    // Worker will stop, but other workers continue
                }
            }
        }
        logger.info("ChallengeRewardsQueueService started successfully")
        println("✅ [ChallengeRewardsQueue] Started successfully with $maxConcurrentWorkers workers")
    }
}


