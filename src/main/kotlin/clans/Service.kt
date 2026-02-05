package com.apptime.code.clans

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlin.time.Duration.Companion.days

class ClanService {
    private val repository = ClanRepository()
    
    /**
     * Create a new clan
     */
    fun createClan(creatorId: String, request: CreateClanRequest): Clan {
        // Validate clan name
        if (request.name.isBlank() || request.name.length < 3) {
            throw IllegalArgumentException("Clan name must be at least 3 characters long")
        }
        
        if (request.name.length > 100) {
            throw IllegalArgumentException("Clan name must be at most 100 characters long")
        }
        
        // Validate max members
        if (request.maxMembers < 2 || request.maxMembers > 200) {
            throw IllegalArgumentException("Max members must be between 2 and 200")
        }
        
        // Validate clan type
        if (request.clanType !in listOf("PUBLIC", "PRIVATE", "INVITE_ONLY")) {
            throw IllegalArgumentException("Invalid clan type. Must be PUBLIC, PRIVATE, or INVITE_ONLY")
        }
        
        val clanId = repository.createClan(
            creatorId = creatorId,
            name = request.name,
            description = request.description,
            tagline = request.tagline,
            logoUrl = request.logoUrl,
            clanType = request.clanType,
            maxMembers = request.maxMembers,
            country = request.country,
            city = request.city,
            category = request.category
        )
        
        return repository.getClanById(clanId, creatorId)
            ?: throw IllegalStateException("Failed to create clan")
    }
    
    /**
     * Update clan details (admin only)
     */
    fun updateClan(clanId: Long, userId: String, request: UpdateClanRequest): Clan {
        // Check if user is admin or moderator of this specific clan
        val member = repository.getUserClanMembership(userId, clanId)
            ?: throw IllegalStateException("User is not a member of this clan")
        
        if (member.role !in listOf("ADMIN", "MODERATOR")) {
            throw IllegalStateException("User does not have permission to update clan")
        }
        
        // Validate max members if provided
        if (request.maxMembers != null && (request.maxMembers < 2 || request.maxMembers > 200)) {
            throw IllegalArgumentException("Max members must be between 2 and 200")
        }
        
        // Validate clan type if provided
        if (request.clanType != null && request.clanType !in listOf("PUBLIC", "PRIVATE", "INVITE_ONLY")) {
            throw IllegalArgumentException("Invalid clan type. Must be PUBLIC, PRIVATE, or INVITE_ONLY")
        }
        
        repository.updateClan(
            clanId = clanId,
            description = request.description,
            tagline = request.tagline,
            logoUrl = request.logoUrl,
            clanType = request.clanType,
            maxMembers = request.maxMembers,
            country = request.country,
            city = request.city,
            category = request.category
        )
        
        return repository.getClanById(clanId, userId)
            ?: throw IllegalStateException("Failed to update clan")
    }
    
    /**
     * Delete clan (creator/admin only)
     */
    fun deleteClan(clanId: Long, userId: String) {
        val clan = repository.getClanById(clanId, userId)
            ?: throw IllegalStateException("Clan not found")
        
        if (clan.creatorId != userId) {
            throw IllegalStateException("Only the clan creator can delete the clan")
        }
        
        repository.deleteClan(clanId)
    }
    
    /**
     * Get clan details
     */
    fun getClanDetails(clanId: Long, userId: String?): ClanDetailsResponse {
        val clan = repository.getClanById(clanId, userId)
            ?: throw IllegalStateException("Clan not found")
        
        val members = repository.getClanMembers(clanId)
        val badges = repository.getClanBadges(clanId)
        val stats = getClanStats(clanId)
        
        return ClanDetailsResponse(
            clan = clan,
            members = members,
            stats = stats,
            badges = badges
        )
    }
    
    /**
     * Get clan statistics
     */
    fun getClanStats(clanId: Long): ClanStatsResponse {
        val clan = repository.getClanById(clanId)
            ?: throw IllegalStateException("Clan not found")
        
        val members = repository.getClanMembers(clanId)
        
        // Get today's date
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        
        // Get this week's date
        val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())
        val week = today.get(weekFields.weekOfWeekBasedYear())
        val year = today.get(weekFields.weekBasedYear())
        val weekStr = "${year}-W${String.format("%02d", week)}"
        
        // Get this month
        val monthStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        
        // This would require querying ClanStats table
        // For now, returning basic stats
        val topContributors = members
            .sortedByDescending { it.contributedFocusHours }
            .take(5)
            .mapIndexed { index, member ->
                TopContributor(
                    userId = member.userId,
                    username = member.username,
                    contributedFocusHours = member.contributedFocusHours,
                    rank = index + 1
                )
            }
        
        return ClanStatsResponse(
            totalFocusHours = clan.totalFocusHours,
            dailyFocusHours = 0L, // Would need to query ClanStats
            weeklyFocusHours = 0L, // Would need to query ClanStats
            monthlyFocusHours = 0L, // Would need to query ClanStats
            topContributors = topContributors
        )
    }
    
    /**
     * List clans with filters
     */
    fun listClans(
        category: String?,
        country: String?,
        city: String?,
        searchQuery: String?,
        clanType: String?,
        page: Int,
        pageSize: Int,
        userId: String?
    ): ClanListResponse {
        val (clans, totalCount) = repository.listClans(
            category = category,
            country = country,
            city = city,
            searchQuery = searchQuery,
            clanType = clanType,
            page = page,
            pageSize = pageSize,
            requestingUserId = userId
        )
        
        return ClanListResponse(
            clans = clans,
            totalCount = totalCount,
            page = page,
            pageSize = pageSize
        )
    }
    
    /**
     * Join a clan
     */
    fun joinClan(userId: String, request: JoinClanRequest): ClanMember {
        // If invite code is provided, use it
        if (request.inviteCode != null) {
            return repository.acceptInvite(request.inviteCode, userId)
        }
        
        // Otherwise, use clan ID
        if (request.clanId == null) {
            throw IllegalArgumentException("Either clanId or inviteCode must be provided")
        }
        
        val clan = repository.getClanById(request.clanId)
            ?: throw IllegalStateException("Clan not found")
        
        // Check clan type
        when (clan.clanType) {
            "PUBLIC" -> {
                // Anyone can join
                return repository.joinClan(request.clanId, userId)
            }
            "PRIVATE" -> {
                // Requires approval - create join request
                repository.createJoinRequest(request.clanId, userId, request.message)
                throw IllegalStateException("Join request created. Waiting for approval from clan admins.")
            }
            "INVITE_ONLY" -> {
                throw IllegalStateException("This clan is invite-only. You need an invite code to join.")
            }
            else -> {
                throw IllegalStateException("Invalid clan type")
            }
        }
    }
    
    /**
     * Leave a clan
     */
    fun leaveClan(userId: String, clanId: Long) {
        repository.leaveClan(clanId, userId)
    }
    
    /**
     * Update member role (admin/moderator only)
     */
    fun updateMemberRole(clanId: Long, adminUserId: String, request: UpdateMemberRoleRequest) {
        // Check if user is admin of this specific clan
        val member = repository.getUserClanMembership(adminUserId, clanId)
            ?: throw IllegalStateException("User is not a member of this clan")
        
        if (member.role != "ADMIN") {
            throw IllegalStateException("User does not have permission to update member roles")
        }
        
        // Validate role
        if (request.role !in listOf("ADMIN", "MODERATOR", "MEMBER")) {
            throw IllegalArgumentException("Invalid role. Must be ADMIN, MODERATOR, or MEMBER")
        }
        
        // Cannot demote yourself if you're the only admin
        if (request.userId == adminUserId && request.role != "ADMIN") {
            val members = repository.getClanMembers(clanId)
            val adminCount = members.count { it.role == "ADMIN" }
            if (adminCount == 1) {
                throw IllegalStateException("Cannot demote yourself as the only admin")
            }
        }
        
        repository.updateMemberRole(clanId, request.userId, request.role)
    }
    
    /**
     * Remove member from clan (admin/moderator only)
     */
    fun removeMember(clanId: Long, adminUserId: String, request: RemoveMemberRequest) {
        // Check if user is admin or moderator of this specific clan
        val member = repository.getUserClanMembership(adminUserId, clanId)
            ?: throw IllegalStateException("User is not a member of this clan")
        
        if (member.role !in listOf("ADMIN", "MODERATOR")) {
            throw IllegalStateException("User does not have permission to remove members")
        }
        
        // Cannot remove yourself
        if (request.userId == adminUserId) {
            throw IllegalStateException("Cannot remove yourself. Use leave clan instead.")
        }
        
        // Cannot remove the creator
//        if (clan.creatorId == request.userId) {
//            throw IllegalStateException("Cannot remove the clan creator")
//        }
        
        repository.removeMember(clanId, request.userId)
    }
    
    /**
     * Create clan invite
     */
    fun createInvite(clanId: Long, userId: String, request: CreateInviteRequest): ClanInvite {
        // Check if user is admin or moderator of this specific clan
        val member = repository.getUserClanMembership(userId, clanId)
            ?: throw IllegalStateException("User is not a member of this clan")
        
        if (member.role !in listOf("ADMIN", "MODERATOR")) {
            throw IllegalStateException("User does not have permission to create invites")
        }
        
        // Calculate expiration
        val expiresAt = if (request.expiresInDays != null) {
            Clock.System.now() + request.expiresInDays.days
        } else {
            null
        }
        
        return repository.createInvite(
            clanId = clanId,
            inviterId = userId,
            inviteeUserId = request.inviteeUserId,
            maxUses = request.maxUses,
            expiresAt = expiresAt
        )
    }
    
    /**
     * Accept invite
     */
    fun acceptInvite(userId: String, inviteCode: String): ClanMember {
        return repository.acceptInvite(inviteCode, userId)
    }
    
    /**
     * Get pending join requests for a clan (admin/moderator only)
     */
    fun getPendingJoinRequests(clanId: Long, userId: String): List<ClanJoinRequest> {
        // Check if user is admin or moderator of this specific clan
        val member = repository.getUserClanMembership(userId, clanId)
            ?: throw IllegalStateException("User is not a member of this clan")
        
        if (member.role !in listOf("ADMIN", "MODERATOR")) {
            throw IllegalStateException("User does not have permission to view join requests")
        }
        
        return repository.getPendingJoinRequests(clanId)
    }
    
    /**
     * Review join request (admin/moderator only)
     */
    fun reviewJoinRequest(
        clanId: Long,
        requestId: Long,
        userId: String,
        request: ReviewJoinRequestRequest
    ): ClanMember? {
        // Check if user is admin or moderator of this specific clan
        val member = repository.getUserClanMembership(userId, clanId)
            ?: throw IllegalStateException("User is not a member of this clan")
        
        if (member.role !in listOf("ADMIN", "MODERATOR")) {
            throw IllegalStateException("User does not have permission to review join requests")
        }
        
        return repository.reviewJoinRequest(requestId, userId, request.approved)
    }
    
    /**
     * Get user's clan info
     */
    fun getUserClanInfo(userId: String): UserClanInfoResponse {
        val (clan, member) = repository.getUserClanInfo(userId)
        
        val contributionStats = if (member != null) {
            ContributionStats(
                totalContributed = member.contributedFocusHours,
                dailyContribution = 0L, // Would need to query ClanStats
                weeklyContribution = 0L, // Would need to query ClanStats
                monthlyContribution = 0L, // Would need to query ClanStats
                rankInClan = null // Would need to calculate from members
            )
        } else {
            null
        }
        
        return UserClanInfoResponse(
            clan = clan,
            memberInfo = member,
            contributionStats = contributionStats
        )
    }
    
    /**
     * Get clan leaderboard
     */
    fun getClanLeaderboard(
        period: String,
        periodDate: String?,
        limit: Int,
        userId: String?
    ): ClanLeaderboardResponse {
        // Validate period
        if (period !in listOf("daily", "weekly", "monthly")) {
            throw IllegalArgumentException("Invalid period. Must be daily, weekly, or monthly")
        }
        
        // Generate period date if not provided
        val finalPeriodDate = periodDate ?: when (period) {
            "daily" -> LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            "weekly" -> {
                val today = LocalDate.now()
                val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())
                val week = today.get(weekFields.weekOfWeekBasedYear())
                val year = today.get(weekFields.weekBasedYear())
                "${year}-W${String.format("%02d", week)}"
            }
            "monthly" -> LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
            else -> throw IllegalArgumentException("Invalid period")
        }
        
        // Get user's clan ID if userId is provided
//        val userClanId = if (userId != null) {
//            val (clan, _) = repository.getUserClanInfo(userId)
//            clan?.id
//        } else {
//            null
//        }
        
        val (entries, userClanRank) = repository.getClanLeaderboard(
            period = period,
            periodDate = finalPeriodDate,
            limit = limit,
            //userClanId = userClanId
        )
        
        return ClanLeaderboardResponse(
            period = period,
            periodDate = finalPeriodDate,
            entries = entries,
            userClanRank = userClanRank,
            totalClans = entries.size
        )
    }
    
    /**
     * Update clan stats with focus time
     * This should be called whenever a user completes a focus session
     */
    fun updateClanStatsWithFocusTime(userId: String, focusDuration: Long, date: LocalDate) {
        repository.updateClanStatsWithFocusTime(userId, focusDuration, date)
        
        // Check for badge achievements
        checkAndAwardBadges(userId)
    }
    
    /**
     * Check and award badges to clans based on achievements
     */
    private fun checkAndAwardBadges(userId: String) {
        // Check badges for all clans the user is a member of
        val userClans = repository.getUserClans(userId)
        
        for ((clan, _) in userClans) {
            // Check for total focus hours milestones
            val milestones = listOf(
                Pair(100L * 3600 * 1000, "MILESTONE_100H"),  // 100 hours
                Pair(500L * 3600 * 1000, "MILESTONE_500H"),  // 500 hours
                Pair(1000L * 3600 * 1000, "MILESTONE_1000H"), // 1000 hours
                Pair(5000L * 3600 * 1000, "MILESTONE_5000H")  // 5000 hours
            )
            
            for ((threshold, badgeType) in milestones) {
                if (clan.totalFocusHours >= threshold) {
                    // Check if badge already exists
//                    val badges = repository.getClanBadges(clan.id)
//                    if (badges.none { it.badgeType == badgeType }) {
//                        repository.addClanBadge(
//                            clanId = clan.id,
//                            badgeType = badgeType,
//                            title = "${threshold / (3600 * 1000)} Hour Milestone",
//                            description = "Achieved ${threshold / (3600 * 1000)} total focus hours",
//                            iconUrl = null,
//                            metadata = """{"totalHours": ${threshold / (3600 * 1000)}}"""
//                        )
//                    }
                }
            }
        }
    }
    
    /**
     * Award leaderboard badges to top clans
     * This should be called periodically (e.g., at the end of each day/week/month)
     */
    fun awardLeaderboardBadges(period: String, periodDate: String, topN: Int = 10) {
        val (entries, _) = repository.getClanLeaderboard(period, periodDate, topN, null)
        
        for (entry in entries) {
            val badgeType = when (period) {
                "daily" -> "TOP_${entry.rank}_DAILY"
                "weekly" -> "TOP_${entry.rank}_WEEKLY"
                "monthly" -> "TOP_${entry.rank}_MONTHLY"
                else -> continue
            }
            
            // Check if badge already exists for this period
            val badges = repository.getClanBadges(entry.clanId)
            val existingBadge = badges.find { 
                it.badgeType == badgeType && it.metadata?.contains(periodDate) == true 
            }
            
            if (existingBadge == null) {
                repository.addClanBadge(
                    clanId = entry.clanId,
                    badgeType = badgeType,
                    title = "Top ${entry.rank} - ${period.capitalize()}",
                    description = "Ranked #${entry.rank} in the $period leaderboard on $periodDate",
                    iconUrl = null,
                    metadata = """{"rank": ${entry.rank}, "period": "$period", "periodDate": "$periodDate"}"""
                )
            }
        }
    }
}

