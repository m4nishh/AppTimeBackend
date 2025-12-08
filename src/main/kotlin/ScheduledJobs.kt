package com.apptime.code

import com.apptime.code.challenges.ChallengeRepository
import com.apptime.code.challenges.ChallengeService
import com.apptime.code.leaderboard.LeaderboardRepository
import com.apptime.code.leaderboard.LeaderboardService
import io.ktor.server.application.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * Scheduled jobs for the application
 */
class ScheduledJobs(
    private val leaderboardService: LeaderboardService,
    private val challengeService: ChallengeService
) {
    private val logger = LoggerFactory.getLogger(ScheduledJobs::class.java)
    private var leaderboardSyncJob: Job? = null
    private var challengeStatsSyncJob: Job? = null

    /**
     * Start the leaderboard sync job that runs every 10 minutes
     */
    fun startLeaderboardSyncJob(scope: CoroutineScope) {
        logger.info("Starting leaderboard sync job - will run every 10 minutes")
        
        leaderboardSyncJob = scope.launch {
            while (isActive) {
                try {
                    logger.info("Running scheduled leaderboard sync...")
                    val result = leaderboardService.syncFromAppUsageEvents(null)
                    logger.info("Leaderboard sync completed: ${result.message}. " +
                            "Events processed: ${result.eventsProcessed}, " +
                            "Stats updated: ${result.statsUpdated}")
                } catch (e: Exception) {
                    logger.error("Error during scheduled leaderboard sync: ${e.message}", e)
                }
                
                // Wait 10 minutes (600,000 milliseconds) before next run
                delay(10 * 60 * 1000L)
            }
        }
    }
    
    /**
     * Start the challenge stats sync job that runs every 15 minutes
     */
    fun startChallengeStatsSyncJob(scope: CoroutineScope) {
        logger.info("Starting challenge stats sync job - will run every 15 minutes")
        
        challengeStatsSyncJob = scope.launch {
            while (isActive) {
                try {
                    logger.info("Running scheduled challenge stats sync...")
                    val result = challengeService.syncChallengeStatsFromAppUsageEvents(null)
                    logger.info("Challenge stats sync completed: ${result.message}. " +
                            "Events processed: ${result.eventsProcessed}, " +
                            "Challenges processed: ${result.challengesProcessed}, " +
                            "Stats created: ${result.statsCreated}, " +
                            "Users updated: ${result.usersUpdated}")
                } catch (e: Exception) {
                    logger.error("Error during scheduled challenge stats sync: ${e.message}", e)
                }
                
                // Wait 15 minutes (900,000 milliseconds) before next run
                delay(15 * 60 * 1000L)
            }
        }
    }

    /**
     * Stop all scheduled jobs
     */
    fun stop() {
        logger.info("Stopping scheduled jobs...")
        leaderboardSyncJob?.cancel()
        leaderboardSyncJob = null
        challengeStatsSyncJob?.cancel()
        challengeStatsSyncJob = null
    }
}

/**
 * Configure and start scheduled jobs
 */
fun Application.configureScheduledJobs() {
    val leaderboardRepository = LeaderboardRepository()
    val leaderboardService = LeaderboardService(leaderboardRepository)
    val challengeRepository = ChallengeRepository()
    val challengeService = ChallengeService(challengeRepository)
    val scheduledJobs = ScheduledJobs(leaderboardService, challengeService)
    
    // Start the leaderboard sync job
    // Application extends CoroutineScope in Ktor, so we can use 'this' directly
    scheduledJobs.startLeaderboardSyncJob(this)
    
    // Start the challenge stats sync job
    scheduledJobs.startChallengeStatsSyncJob(this)
    
    // Store reference to stop jobs when application shuts down
    environment.monitor.subscribe(ApplicationStopped) {
        scheduledJobs.stop()
    }
    
    log.info("Scheduled jobs configured and started")
}

