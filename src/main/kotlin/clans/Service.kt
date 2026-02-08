package com.apptime.code.clans

import com.apptime.code.notifications.NotificationService
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import users.UserRepository
import kotlin.time.Duration.Companion.days

class ClanService(
    private val notificationService: NotificationService? = null,
    private val userRepository: UserRepository? = null,
    private val notificationScope: CoroutineScope? = null
) {
    val repository = ClanRepository()
    private val defaultScope = CoroutineScope(Dispatchers.IO)
    
    private fun getNotificationScope() = notificationScope ?: defaultScope
    
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
        
        val clan = repository.getClanById(clanId, creatorId)
            ?: throw IllegalStateException("Failed to create clan")
        
        // Send notification to creator
        notificationService?.let { service ->
            getNotificationScope().launch {
                try {
                    service.sendClanCreatedNotification(creatorId, clanId, request.name)
                } catch (e: Exception) {
                    // Log but don't fail the operation
                }
            }
        }
        
        return clan
    }
    
    /**
     * Update clan details (admin only)
     */
    fun updateClan(clanId: Long, userId: String, request: UpdateClanRequest): Clan {
        // Check if user is admin
        val (userClan, member) = repository.getUserClanInfo(userId)
//        if (userClan?.id != clanId) {
//            throw IllegalStateException("User is not a member of this clan")
//        }
        if (member?.role !in listOf("ADMIN", "MODERATOR")) {
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
        
        val updatedClan = repository.getClanById(clanId, userId)
            ?: throw IllegalStateException("Failed to update clan")
        
        // Send notifications to all members
        notificationService?.let { service ->
            getNotificationScope().launch {
                try {
                    val members = repository.getClanMembers(clanId)
                    val memberUserIds = members.map { it.userId }
                    val updatedByUsername = userRepository?.getUserById(userId)?.username
                    
                    service.sendClanUpdatedNotification(
                        clanId = clanId,
                        clanName = updatedClan.name,
                        memberUserIds = memberUserIds,
                        updatedByUsername = updatedByUsername
                    )
                } catch (e: Exception) {
                    // Log but don't fail the operation
                }
            }
        }
        
        return updatedClan
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
        
        // Get app usage analytics (default to daily)
        val appUsageAnalytics = try {
            getClanAppUsageAnalytics(clanId, "daily")
        } catch (e: Exception) {
            // If analytics fail, return null instead of breaking the API
            null
        }
        
        return ClanDetailsResponse(
            clan = clan,
            members = members,
            stats = stats,
            badges = badges,
            appUsageAnalytics = appUsageAnalytics
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
        
        // Query ClanStats table for daily, weekly, and monthly stats
        val dailyFocusHours = repository.getClanStatsByPeriod(clanId, "daily", todayStr)
        val weeklyFocusHours = repository.getClanStatsByPeriod(clanId, "weekly", weekStr)
        val monthlyFocusHours = repository.getClanStatsByPeriod(clanId, "monthly", monthStr)
        
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
            dailyFocusHours = dailyFocusHours,
            weeklyFocusHours = weeklyFocusHours,
            monthlyFocusHours = monthlyFocusHours,
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
        // If token is provided, decode it and use the invite code
        if (request.token != null) {
            val pair = com.apptime.code.common.TokenEncoder.decodeClanInvite(request.token)
                ?: throw IllegalArgumentException("Invalid invite token")
            val (clanIdFromToken, inviteCodeFromToken) = pair
            
            // Validate clan ID matches if provided
            if (request.clanId != null && request.clanId != clanIdFromToken) {
                throw IllegalArgumentException("Token does not match provided clan ID")
            }
            
            return repository.acceptInvite(inviteCodeFromToken, userId)
        }

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
            
        // Check if user is already a member
        val existingMembership = repository.getUserClanMembership(userId, clan.id)
        if (existingMembership != null) {
            return existingMembership
        }
        
        // Check clan type
        when (clan.clanType) {

            "PUBLIC", "PRIVATE" -> {
                // Anyone can join directly (PRIVATE clans are just hidden from search)
                val member = repository.joinClan(request.clanId, userId)
                
                // Send notifications to all members
                notificationService?.let { service ->
                    getNotificationScope().launch {
                        try {
                            val members = repository.getClanMembers(request.clanId)
                            val memberUserIds = members.map { it.userId }
                            val username = userRepository?.getUserById(userId)?.username
                            
                            service.sendClanMemberJoinedNotification(
                                clanId = request.clanId,
                                clanName = clan.name,
                                newMemberUsername = username,
                                newMemberUserId = userId,
                                memberUserIds = memberUserIds
                            )
                        } catch (e: Exception) {
                            // Log but don't fail the operation
                        }
                    }
                }
                
                return member
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
        val clan = repository.getClanById(clanId, userId)
            ?: throw IllegalStateException("Clan not found")
        
        repository.leaveClan(clanId, userId)
        
        // Send notifications to remaining members
        notificationService?.let { service ->
            getNotificationScope().launch {
                try {
                    val members = repository.getClanMembers(clanId)
                    val memberUserIds = members.map { it.userId }
                    val username = userRepository?.getUserById(userId)?.username
                    
                    service.sendClanMemberLeftNotification(
                        clanId = clanId,
                        clanName = clan.name,
                        leftMemberUsername = username,
                        leftMemberUserId = userId,
                        memberUserIds = memberUserIds
                    )
                } catch (e: Exception) {
                    // Log but don't fail the operation
                }
            }
        }
    }
    
    /**
     * Update member role (admin/moderator only)
     */
    fun updateMemberRole(clanId: Long, adminUserId: String, request: UpdateMemberRoleRequest) {
        // Check if user is admin
        val (userClan, member) = repository.getUserClanInfo(adminUserId)
//        if (userClan?.id != clanId) {
//            throw IllegalStateException("User is not a member of this clan")
//        }
        if (member?.role != "ADMIN") {
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
        
        // Send notification to the user whose role changed
        notificationService?.let { service ->
            getNotificationScope().launch {
                try {
                    val clan = repository.getClanById(clanId)
                    val adminUsername = userRepository?.getUserById(adminUserId)?.username
                    
                    if (clan != null) {
                        service.sendClanRoleChangedNotification(
                            userId = request.userId,
                            clanId = clanId,
                            clanName = clan.name,
                            newRole = request.role,
                            changedByUsername = adminUsername
                        )
                    }
                } catch (e: Exception) {
                    // Log but don't fail the operation
                }
            }
        }
    }
    
    /**
     * Remove member from clan (admin/moderator only)
     */
    fun removeMember(clanId: Long, adminUserId: String, request: RemoveMemberRequest) {
        // Check if user is admin or moderator
        val (userClan, member) = repository.getUserClanInfo(adminUserId)
//        if (userClan?.id != clanId) {
//            throw IllegalStateException("User is not a member of this clan")
//        }
        if (member?.role !in listOf("ADMIN", "MODERATOR")) {
            throw IllegalStateException("User does not have permission to remove members")
        }
        
        // Cannot remove yourself
        if (request.userId == adminUserId) {
            throw IllegalStateException("Cannot remove yourself. Use leave clan instead.")
        }
        
        // Cannot remove the creator
//        if (userClan?.creatorId == request.userId) {
//            throw IllegalStateException("Cannot remove the clan creator")
//        }
        
        val clan = repository.getClanById(clanId, adminUserId)
            ?: throw IllegalStateException("Clan not found")
        
        repository.removeMember(clanId, request.userId)
        
        // Send notification to removed member
        notificationService?.let { service ->
            getNotificationScope().launch {
                try {
                    val adminUsername = userRepository?.getUserById(adminUserId)?.username
                    
                    service.sendClanMemberRemovedNotification(
                        userId = request.userId,
                        clanId = clanId,
                        clanName = clan.name,
                        removedByUsername = adminUsername,
                        reason = request.reason
                    )
                } catch (e: Exception) {
                    // Log but don't fail the operation
                }
            }
        }
    }
    
    /**
     * Create clan invite
     */
    fun createInvite(clanId: Long, userId: String, request: CreateInviteRequest): ClanInvite {
        // Check if user is admin or moderator
        val (userClan, member) = repository.getUserClanInfo(userId)
//        if (userClan?.id != clanId) {
//            throw IllegalStateException("User is not a member of this clan")
//        }
        if (member?.role !in listOf("ADMIN", "MODERATOR")) {
            throw IllegalStateException("User does not have permission to create invites")
        }
        
        // Calculate expiration
        val expiresAt = if (request.expiresInDays != null) {
            Clock.System.now() + request.expiresInDays.days
        } else {
            null
        }
        
        val invite = repository.createInvite(
            clanId = clanId,
            inviterId = userId,
            inviteeUserId = request.inviteeUserId,
            maxUses = request.maxUses,
            expiresAt = expiresAt
        )
        
        // Send notification to invitee if specified
        if (request.inviteeUserId != null) {
            notificationService?.let { service ->
                getNotificationScope().launch {
                    try {
                        val clan = repository.getClanById(clanId)
                        val inviterUsername = userRepository?.getUserById(userId)?.username
                        
                        if (clan != null) {
                            service.sendClanInviteReceivedNotification(
                                userId = request.inviteeUserId,
                                clanId = clanId,
                                clanName = clan.name,
                                inviterUsername = inviterUsername,
                                inviteCode = invite.inviteCode
                            )
                        }
                    } catch (e: Exception) {
                        // Log but don't fail the operation
                    }
                }
            }
        }
        
        return invite
    }
    
    /**
     * Accept invite
     */
    fun acceptInvite(userId: String, inviteCode: String): ClanMember {
        val member = repository.acceptInvite(inviteCode, userId)
        
        // Send notifications to all members
        notificationService?.let { service ->
            getNotificationScope().launch {
                try {
                    val members = repository.getClanMembers(member.clanId)
                    val memberUserIds = members.map { it.userId }
                    val clan = repository.getClanById(member.clanId)
                    val username = userRepository?.getUserById(userId)?.username
                    
                    if (clan != null) {
                        service.sendClanMemberJoinedNotification(
                            clanId = member.clanId,
                            clanName = clan.name,
                            newMemberUsername = username,
                            newMemberUserId = userId,
                            memberUserIds = memberUserIds
                        )
                    }
                } catch (e: Exception) {
                    // Log but don't fail the operation
                }
            }
        }
        
        return member
    }
    
    /**
     * Get pending join requests for a clan (admin/moderator only)
     */
    fun getPendingJoinRequests(clanId: Long, userId: String): List<ClanJoinRequest> {
        // Check if user is admin or moderator
        val (clan, member) = repository.getUserClanInfo(userId)
//        if (clan?.id != clanId) {
//            throw IllegalStateException("User is not a member of this clan")
//        }
        if (member?.role !in listOf("ADMIN", "MODERATOR")) {
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
        // Check if user is admin or moderator
        val (userClan, member) = repository.getUserClanInfo(userId)
//        if (userClan?.id != clanId) {
//            throw IllegalStateException("User is not a member of this clan")
//        }
        if (member?.role !in listOf("ADMIN", "MODERATOR")) {
            throw IllegalStateException("User does not have permission to review join requests")
        }
        
        // Get join request details before reviewing (to get requester userId)
        val joinRequest = repository.getJoinRequestById(requestId)
            ?: throw IllegalStateException("Join request not found")
        
        val result = repository.reviewJoinRequest(requestId, userId, request.approved)
        
        // Send notification to requester
        notificationService?.let { service ->
            getNotificationScope().launch {
                try {
                    val requesterUserId = result?.userId ?: joinRequest.userId
                    val clan = repository.getClanById(clanId)
                    val reviewerUsername = userRepository?.getUserById(userId)?.username
                    
                    if (clan != null) {
                        if (request.approved) {
                            service.sendClanJoinRequestApprovedNotification(
                                userId = requesterUserId,
                                clanId = clanId,
                                clanName = clan.name,
                                approvedByUsername = reviewerUsername
                            )
                        } else {
                            service.sendClanJoinRequestRejectedNotification(
                                userId = requesterUserId,
                                clanId = clanId,
                                clanName = clan.name,
                                rejectedByUsername = reviewerUsername,
                                reason = request.reason
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Log but don't fail the operation
                }
            }
        }
        
        return result
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
        val (clan, _) = repository.getUserClanInfo(userId)
        if (clan == null) return
        
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
//                val badges = repository.getClanBadges(clan.id)
//                if (badges.none { it.badgeType == badgeType }) {
//                    repository.addClanBadge(
//                        clanId = clan.id,
//                        badgeType = badgeType,
//                        title = "${threshold / (3600 * 1000)} Hour Milestone",
//                        description = "Achieved ${threshold / (3600 * 1000)} total focus hours",
//                        iconUrl = null,
//                        metadata = """{"totalHours": ${threshold / (3600 * 1000)}}"""
//                    )
//                }
            }
        }
    }
    
    /**
     * Get or create a share link for a clan
     * Returns a shareable link with tracking code
     */
    fun getShareLink(clanId: Long, userId: String, baseUrl: String): ClanShareLinkResponse {
        // Check if user is a member of the clan
        val (userClan, member) = repository.getUserClanInfo(userId)
        if (userClan == null || member == null) {
            throw IllegalStateException("User is not a member of this clan")
        }
        
        // Validate clan exists
        val clan = repository.getClanById(clanId)
            ?: throw IllegalStateException("Clan not found")
        
        // Create or get share record
        val shareCode = repository.createOrGetClanShare(clanId, userId)
        
        // Encode clan ID and share code into a secure token
        val token = com.apptime.code.common.TokenEncoder.encodeClanShare(clanId, shareCode)
        val encodedToken = java.net.URLEncoder.encode(token, "UTF-8")
        
        // Use token instead of revealing clan ID and share code
        val shareLink = "$baseUrl/clan/$encodedToken"
        val deeplink = "apptime://screen/clan_detail/$encodedToken"
        

        return ClanShareLinkResponse(
            clanId = clanId,
            shareLink = shareLink,
            deeplink = deeplink,
            shareCode = shareCode
        )
    }

    /**
     * Get permanent invite link for a clan (Admin/Moderator only)
     */
    fun getPermanentInviteLink(clanId: Long, userId: String, baseUrl: String): ClanInviteLinkResponse {
        // Check if user is admin or moderator
        val (userClan, member) = repository.getUserClanInfo(userId)
//        if (userClan?.id != clanId) {
//            throw IllegalStateException("User is not a member of this clan")
//        }
        if (member?.role !in listOf("ADMIN", "MODERATOR")) {
            throw IllegalStateException("User does not have permission to create invite links")
        }
        

        // Get or create permanent invite
        val invite = repository.getOrCreateShareLink(clanId, userId)
        
        // Encode invite
        val token = com.apptime.code.common.TokenEncoder.encodeClanInvite(clanId, invite.inviteCode)
        val encodedToken = java.net.URLEncoder.encode(token, "UTF-8")
        

        // Construct links
        // We use query parameters for clanId and token
        val inviteLink = "$baseUrl/join-clan?clanId=$clanId&token=$encodedToken"
        val deeplink = "apptime://screen/clan_detail?clanId=$clanId&token=$encodedToken"
        
        return ClanInviteLinkResponse(
            inviteLink = inviteLink,
            inviteCode = invite.inviteCode,
            deeplink = deeplink
        )
    }
    
    /**
     * Get clan app usage analytics
     * Returns category-wise breakdown, top apps, and member activity
     */
    fun getClanAppUsageAnalytics(
        clanId: Long,
        period: String = "daily" // "daily", "weekly", "monthly"
    ): ClanAppUsageAnalyticsResponse {
        val clan = repository.getClanById(clanId)
            ?: throw IllegalStateException("Clan not found")
        
        // Get period date based on period type
        val today = LocalDate.now()
        val periodDate = when (period) {
            "daily" -> today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            "weekly" -> {
                val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())
                val week = today.get(weekFields.weekOfWeekBasedYear())
                val year = today.get(weekFields.weekBasedYear())
                "${year}-W${String.format("%02d", week)}"
            }
            "monthly" -> today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            else -> today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }
        
        val analytics = repository.getClanAppUsageAnalytics(clanId, period, periodDate)
        
        return ClanAppUsageAnalyticsResponse(
            period = analytics.period,
            periodDate = analytics.periodDate,
            totalScreenTime = analytics.totalScreenTime,
            categoryBreakdown = analytics.categoryBreakdown,
            topApps = analytics.topApps,
            memberActivity = analytics.memberActivity
        )
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

