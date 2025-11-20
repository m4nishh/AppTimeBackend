package com.apptime.code.challenges

import com.apptime.code.common.dbTransaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class ChallengeRepository {
    
    /**
     * Get all active challenges (where isActive = true and endTime > now)
     */
    fun getActiveChallenges(userId: String? = null): List<ActiveChallenge> {
        return dbTransaction {
            val now = Clock.System.now()
            val joinedChallengeIds = if (userId != null) {
                ChallengeParticipants.slice(ChallengeParticipants.challengeId)
                    .select { ChallengeParticipants.userId eq userId }
                    .map { it[ChallengeParticipants.challengeId] }
                    .toSet()
            } else {
                emptySet()
            }
            Challenges.select {
                (Challenges.isActive eq true) and
                (Challenges.endTime greater now)
            }
            .orderBy(Challenges.startTime to SortOrder.ASC)
            .map { row ->
                val challengeId = row[Challenges.id]
                ActiveChallenge(
                    id = challengeId,
                    title = row[Challenges.title],
                    description = row[Challenges.description],
                    reward = row[Challenges.reward],
                    startTime = row[Challenges.startTime].toString(),
                    endTime = row[Challenges.endTime].toString(),
                    thumbnail = row[Challenges.thumbnail],
                    hasJoined = joinedChallengeIds.contains(challengeId)
                )
            }
        }
    }
    
    /**
     * Get challenge by ID
     */
    fun getChallengeById(challengeId: Long): Challenge? {
        return dbTransaction {
            Challenges.select { Challenges.id eq challengeId }
                .firstOrNull()
                ?.let { row ->
                    Challenge(
                        id = row[Challenges.id],
                        title = row[Challenges.title],
                        description = row[Challenges.description],
                        reward = row[Challenges.reward],
                        startTime = row[Challenges.startTime].toString(),
                        endTime = row[Challenges.endTime].toString(),
                        thumbnail = row[Challenges.thumbnail],
                        challengeType = row[Challenges.challengeType],
                        isActive = row[Challenges.isActive],
                        createdAt = row[Challenges.createdAt].toString()
                    )
                }
        }
    }
    
    /**
     * Check if user has already joined a challenge
     */
    fun hasUserJoinedChallenge(userId: String, challengeId: Long): Boolean {
        return dbTransaction {
            ChallengeParticipants.select {
                (ChallengeParticipants.challengeId eq challengeId) and
                (ChallengeParticipants.userId eq userId)
            }.count() > 0
        }
    }
    
    /**
     * Join a challenge
     */
    fun joinChallenge(userId: String, challengeId: Long): String {
        return dbTransaction {
            ChallengeParticipants.insert {
                it[ChallengeParticipants.challengeId] = challengeId
                it[ChallengeParticipants.userId] = userId
            }
            ChallengeParticipants.select {
                (ChallengeParticipants.challengeId eq challengeId) and
                (ChallengeParticipants.userId eq userId)
            }.first()[ChallengeParticipants.joinedAt].toString()
        }
    }
    
    /**
     * Get all challenges for a user (including past ones)
     */
    fun getUserChallenges(userId: String): List<UserChallenge> {
        return dbTransaction {
            val now = Clock.System.now()
            (ChallengeParticipants innerJoin Challenges)
                .select { ChallengeParticipants.userId eq userId }
                .orderBy(Challenges.endTime to SortOrder.DESC)
                .map { row ->
                    val endTime = row[Challenges.endTime]
                    UserChallenge(
                        id = row[Challenges.id],
                        title = row[Challenges.title],
                        description = row[Challenges.description],
                        reward = row[Challenges.reward],
                        startTime = row[Challenges.startTime].toString(),
                        endTime = endTime.toString(),
                        thumbnail = row[Challenges.thumbnail],
                        challengeType = row[Challenges.challengeType],
                        isActive = row[Challenges.isActive],
                        joinedAt = row[ChallengeParticipants.joinedAt].toString(),
                        isPast = endTime < now
                    )
                }
        }
    }
    
    /**
     * Get challenge detail with participant count
     */
    fun getChallengeDetail(challengeId: Long): ChallengeDetail? {
        return dbTransaction {
            val challenge = Challenges.select { Challenges.id eq challengeId }
                .firstOrNull() ?: return@dbTransaction null
            
            val participantCount = ChallengeParticipants.select {
                ChallengeParticipants.challengeId eq challengeId
            }.count().toInt()
            
            ChallengeDetail(
                id = challenge[Challenges.id],
                title = challenge[Challenges.title],
                description = challenge[Challenges.description],
                reward = challenge[Challenges.reward],
                startTime = challenge[Challenges.startTime].toString(),
                endTime = challenge[Challenges.endTime].toString(),
                thumbnail = challenge[Challenges.thumbnail],
                challengeType = challenge[Challenges.challengeType],
                isActive = challenge[Challenges.isActive],
                participantCount = participantCount,
                createdAt = challenge[Challenges.createdAt].toString()
            )
        }
    }
    
    /**
     * Submit challenge participant stats
     */
    fun submitChallengeStats(
        userId: String,
        challengeId: Long,
        appName: String,
        packageName: String,
        startSyncTime: Instant,
        endSyncTime: Instant,
        duration: Long
    ) {
        dbTransaction {
            ChallengeParticipantStats.insert {
                it[ChallengeParticipantStats.challengeId] = challengeId
                it[ChallengeParticipantStats.userId] = userId
                it[ChallengeParticipantStats.appName] = appName
                it[ChallengeParticipantStats.packageName] = packageName
                it[ChallengeParticipantStats.startSyncTime] = startSyncTime
                it[ChallengeParticipantStats.endSyncTime] = endSyncTime
                it[ChallengeParticipantStats.duration] = duration
            }
        }
    }
    
    /**
     * Get challenge rankings
     * For LESS_SCREENTIME: rank by total duration ascending (lower is better)
     * For MORE_SCREENTIME: rank by total duration descending (higher is better)
     */
    fun getChallengeRankings(
        challengeId: Long,
        challengeType: String,
        limit: Int = 10
    ): List<Pair<String, Long>> {
        return dbTransaction {
            val stats = ChallengeParticipantStats.select {
                ChallengeParticipantStats.challengeId eq challengeId
            }
            
            // Group by userId and sum durations
            val userTotals = stats.groupBy { it[ChallengeParticipantStats.userId] }
                .mapValues { (_, rows) ->
                    rows.sumOf { it[ChallengeParticipantStats.duration] }
                }
            
            // Sort based on challenge type
            val sorted = if (challengeType == "LESS_SCREENTIME") {
                userTotals.toList().sortedBy { it.second } // Ascending (lower is better)
            } else {
                userTotals.toList().sortedByDescending { it.second } // Descending (higher is better)
            }
            
            sorted.take(limit)
        }
    }
    
    /**
     * Get user's total duration for a challenge
     */
    fun getUserChallengeDuration(userId: String, challengeId: Long): Long {
        return dbTransaction {
            ChallengeParticipantStats.select {
                (ChallengeParticipantStats.challengeId eq challengeId) and
                (ChallengeParticipantStats.userId eq userId)
            }.sumOf { it[ChallengeParticipantStats.duration] }
        }
    }
    
    /**
     * Get user's rank in a challenge
     */
    fun getUserRank(userId: String, challengeId: Long, challengeType: String): Int? {
        return dbTransaction {
            val allUserTotals = ChallengeParticipantStats.select {
                ChallengeParticipantStats.challengeId eq challengeId
            }
            .groupBy { it[ChallengeParticipantStats.userId] }
            .mapValues { (_, rows) ->
                rows.sumOf { it[ChallengeParticipantStats.duration] }
            }
            
            val userDuration = allUserTotals[userId] ?: return@dbTransaction null
            if (userDuration == 0L) return@dbTransaction null
            
            val sorted = if (challengeType == "LESS_SCREENTIME") {
                allUserTotals.toList().sortedBy { it.second }
            } else {
                allUserTotals.toList().sortedByDescending { it.second }
            }
            
            sorted.indexOfFirst { it.first == userId }.takeIf { it >= 0 }?.plus(1)
        }
    }
    
    /**
     * Get distinct app count for a user in a challenge
     */
    fun getUserAppCount(userId: String, challengeId: Long): Int {
        return dbTransaction {
            ChallengeParticipantStats.select {
                (ChallengeParticipantStats.challengeId eq challengeId) and
                (ChallengeParticipantStats.userId eq userId)
            }
            .map { it[ChallengeParticipantStats.packageName] }
            .distinct()
            .count()
        }
    }
    
    /**
     * Get total participant count for a challenge
     */
    fun getParticipantCount(challengeId: Long): Int {
        return dbTransaction {
            ChallengeParticipants.select {
                ChallengeParticipants.challengeId eq challengeId
            }.count().toInt()
        }
    }
}

