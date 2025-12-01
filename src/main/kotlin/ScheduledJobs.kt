package com.apptime.code

import com.apptime.code.leaderboard.LeaderboardRepository
import com.apptime.code.leaderboard.LeaderboardService
import io.ktor.server.application.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * Scheduled jobs for the application
 */
class ScheduledJobs(
    private val leaderboardService: LeaderboardService
) {
    private val logger = LoggerFactory.getLogger(ScheduledJobs::class.java)
    private var syncJob: Job? = null

    /**
     * Start the leaderboard sync job that runs every 10 minutes
     */
    fun startLeaderboardSyncJob(scope: CoroutineScope) {
        logger.info("Starting leaderboard sync job - will run every 10 minutes")
        
        syncJob = scope.launch {
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
     * Stop all scheduled jobs
     */
    fun stop() {
        logger.info("Stopping scheduled jobs...")
        syncJob?.cancel()
        syncJob = null
    }
}

/**
 * Configure and start scheduled jobs
 */
fun Application.configureScheduledJobs() {
    val leaderboardRepository = LeaderboardRepository()
    val leaderboardService = LeaderboardService(leaderboardRepository)
    val scheduledJobs = ScheduledJobs(leaderboardService)
    
    // Start the leaderboard sync job
    // Application extends CoroutineScope in Ktor, so we can use 'this' directly
    scheduledJobs.startLeaderboardSyncJob(this)
    
    // Store reference to stop jobs when application shuts down
    environment.monitor.subscribe(ApplicationStopped) {
        scheduledJobs.stop()
    }
    
    log.info("Scheduled jobs configured and started")
}

