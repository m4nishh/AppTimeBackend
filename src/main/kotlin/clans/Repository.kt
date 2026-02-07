package com.apptime.code.clans

import com.apptime.code.common.dbTransaction
import com.apptime.code.users.Users
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*
import kotlin.random.Random
import usage.AppUsageEvents

class ClanRepository {
    
    /**
     * Create a new clan
     */
    fun createClan(
        creatorId: String,
        name: String,
        description: String?,
        tagline: String?,
        logoUrl: String?,
        clanType: String,
        maxMembers: Int,
        country: String?,
        city: String?,
        category: String?
    ): Long {
        return dbTransaction {
            // Check if clan name already exists
            val existingClan = Clans.select { Clans.name eq name }.firstOrNull()
            if (existingClan != null) {
                throw IllegalStateException("Clan name already exists")
            }
            
            // Create clan
            val clanId = Clans.insertAndGetId {
                it[Clans.name] = name
                it[Clans.description] = description
                it[Clans.tagline] = tagline
                it[Clans.logoUrl] = logoUrl
                it[Clans.clanType] = clanType
                it[Clans.maxMembers] = maxMembers
                it[currentMembers] = 1 // Creator is the first member
                it[Clans.creatorId] = creatorId
                it[Clans.country] = country
                it[Clans.city] = city
                it[Clans.category] = category
            }.value
            
            // Add creator as admin member
            val username = Users.select { Users.userId eq creatorId }
                .firstOrNull()
                ?.get(Users.username)
            
            ClanMembers.insert {
                it[ClanMembers.clanId] = clanId
                it[ClanMembers.userId] = creatorId
                it[ClanMembers.username] = username
                it[ClanMembers.role] = "ADMIN"
            }
            
            clanId
        }
    }
    
    /**
     * Get clan by ID
     */
    fun getClanById(clanId: Long, requestingUserId: String? = null): Clan? {
        return dbTransaction {
            val clan = Clans.select { Clans.id eq clanId }.firstOrNull() ?: return@dbTransaction null
            
            var userRole: String? = null
            var isMember = false
            
            if (requestingUserId != null) {
                val membership = ClanMembers.select {
                    (ClanMembers.clanId eq clanId) and
                    (ClanMembers.userId eq requestingUserId) and
                    (ClanMembers.isActive eq true)
                }.firstOrNull()
                
                if (membership != null) {
                    userRole = membership[ClanMembers.role]
                    isMember = true
                }
            }
            
            Clan(
                id = clan[Clans.id].value,
                name = clan[Clans.name],
                description = clan[Clans.description],
                tagline = clan[Clans.tagline],
                logoUrl = clan[Clans.logoUrl],
                clanType = clan[Clans.clanType],
                maxMembers = clan[Clans.maxMembers],
                currentMembers = clan[Clans.currentMembers],
                totalFocusHours = clan[Clans.totalFocusHours],
                creatorId = clan[Clans.creatorId],
                isActive = clan[Clans.isActive],
                country = clan[Clans.country],
                city = clan[Clans.city],
                category = clan[Clans.category],
                createdAt = clan[Clans.createdAt].toString(),
                updatedAt = clan[Clans.updatedAt].toString(),
                userRole = userRole,
                isMember = isMember
            )
        }
    }
    
    /**
     * Update clan details
     */
    fun updateClan(
        clanId: Long,
        description: String?,
        tagline: String?,
        logoUrl: String?,
        clanType: String?,
        maxMembers: Int?,
        country: String?,
        city: String?,
        category: String?
    ) {
        dbTransaction {
            Clans.update({ Clans.id eq clanId }) {
                if (description != null) it[Clans.description] = description
                if (tagline != null) it[Clans.tagline] = tagline
                if (logoUrl != null) it[Clans.logoUrl] = logoUrl
                if (clanType != null) it[Clans.clanType] = clanType
                if (maxMembers != null) it[Clans.maxMembers] = maxMembers
                if (country != null) it[Clans.country] = country
                if (city != null) it[Clans.city] = city
                if (category != null) it[Clans.category] = category
                it[Clans.updatedAt] = Clock.System.now()
            }
        }
    }
    
    /**
     * Delete clan (only by admin/creator)
     */
    fun deleteClan(clanId: Long) {
        dbTransaction {
            Clans.deleteWhere { Clans.id eq clanId }
        }
    }
    
    /**
     * List clans with filters
     */
    fun listClans(
        category: String? = null,
        country: String? = null,
        city: String? = null,
        searchQuery: String? = null,
        clanType: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
        requestingUserId: String? = null
    ): Pair<List<Clan>, Int> {
        return dbTransaction {
            var query = Clans.select { Clans.isActive eq true }
            
            // Privacy: Only show PUBLIC clans to non-members
            // PRIVATE and INVITE_ONLY clans are only visible to members or users with pending join requests
            if (requestingUserId == null) {
                // Not logged in - only show PUBLIC clans
                query = query.andWhere { Clans.clanType eq "PUBLIC" }
            } else {
                // Logged in - show PUBLIC clans + PRIVATE/INVITE_ONLY clans where user is a member or has pending request
                val userClanIds = ClanMembers.select {
                    (ClanMembers.userId eq requestingUserId) and (ClanMembers.isActive eq true)
                }.map { it[ClanMembers.clanId] }.toSet()
                
                val pendingRequestClanIds = ClanJoinRequests.select {
                    (ClanJoinRequests.userId eq requestingUserId) and (ClanJoinRequests.status eq "PENDING")
                }.map { it[ClanJoinRequests.clanId] }.toSet()
                
                val visibleClanIds = userClanIds + pendingRequestClanIds
                
                // Show PUBLIC clans OR clans where user is a member/has pending request
                query = query.andWhere {
                    (Clans.clanType eq "PUBLIC") or 
                    ((Clans.clanType inList listOf("PRIVATE", "INVITE_ONLY")) and (Clans.id inList visibleClanIds))
                }
            }
            
            if (category != null) {
                query = query.andWhere { Clans.category eq category }
            }
            if (country != null) {
                query = query.andWhere { Clans.country eq country }
            }
            if (city != null) {
                query = query.andWhere { Clans.city eq city }
            }
            if (clanType != null) {
                query = query.andWhere { Clans.clanType eq clanType }
            }
            if (searchQuery != null) {
                query = query.andWhere { 
                    (Clans.name like "%$searchQuery%") or 
                    (Clans.description like "%$searchQuery%")
                }
            }
            
            val totalCount = query.count().toInt()
            val offset = (page - 1) * pageSize
            
            val clans = query
                .orderBy(Clans.totalFocusHours to SortOrder.DESC)
                .limit(pageSize, offset.toLong())
                .map { row ->
                    var userRole: String? = null
                    var isMember = false
                    
                    if (requestingUserId != null) {
                        val membership = ClanMembers.select {
                            (ClanMembers.clanId eq row[Clans.id].value) and
                            (ClanMembers.userId eq requestingUserId) and
                            (ClanMembers.isActive eq true)
                        }.firstOrNull()
                        
                        if (membership != null) {
                            userRole = membership[ClanMembers.role]
                            isMember = true
                        }
                    }
                    
                    Clan(
                        id = row[Clans.id].value,
                        name = row[Clans.name],
                        description = row[Clans.description],
                        tagline = row[Clans.tagline],
                        logoUrl = row[Clans.logoUrl],
                        clanType = row[Clans.clanType],
                        maxMembers = row[Clans.maxMembers],
                        currentMembers = row[Clans.currentMembers],
                        totalFocusHours = row[Clans.totalFocusHours],
                        creatorId = row[Clans.creatorId],
                        isActive = row[Clans.isActive],
                        country = row[Clans.country],
                        city = row[Clans.city],
                        category = row[Clans.category],
                        createdAt = row[Clans.createdAt].toString(),
                        updatedAt = row[Clans.updatedAt].toString(),
                        userRole = userRole,
                        isMember = isMember
                    )
                }
            
            Pair(clans, totalCount)
        }
    }
    
    /**
     * Join a clan
     */
    fun joinClan(clanId: Long, userId: String): ClanMember {
        return dbTransaction {
            // Check if user is already a member of this specific clan
            val existingMembership = ClanMembers.select {
                (ClanMembers.clanId eq clanId) and
                (ClanMembers.userId eq userId) and
                (ClanMembers.isActive eq true)
            }.firstOrNull()
            
            if (existingMembership != null) {
                throw IllegalStateException("User is already a member of this clan")
            }
            
            // Check if clan exists and has space
            val clan = Clans.select { Clans.id eq clanId }.firstOrNull()
                ?: throw IllegalStateException("Clan not found")
            
            if (clan[Clans.currentMembers] >= clan[Clans.maxMembers]) {
                throw IllegalStateException("Clan is full")
            }
            
            // Get username
            val username = Users.select { Users.userId eq userId }
                .firstOrNull()
                ?.get(Users.username)
            
            // Add member
            val memberId = ClanMembers.insertAndGetId {
                it[ClanMembers.clanId] = clanId
                it[ClanMembers.userId] = userId
                it[ClanMembers.username] = username
                it[role] = "MEMBER"
            }.value
            
            // Update clan member count
            Clans.update({ Clans.id eq clanId }) {
                it[Clans.currentMembers] = clan[Clans.currentMembers] + 1
            }
            
            val member = ClanMembers.select { ClanMembers.id eq memberId }.first()
            ClanMember(
                clanId = member[ClanMembers.clanId],
                userId = member[ClanMembers.userId],
                username = member[ClanMembers.username],
                role = member[ClanMembers.role],
                contributedFocusHours = member[ClanMembers.contributedFocusHours],
                joinedAt = member[ClanMembers.joinedAt].toString(),
                lastActiveAt = member[ClanMembers.lastActiveAt].toString(),
                isActive = member[ClanMembers.isActive]
            )
        }
    }
    
    /**
     * Leave a clan
     */
    fun leaveClan(clanId: Long, userId: String) {
        dbTransaction {
            // Check if user is the creator/last admin
            val member = ClanMembers.select {
                (ClanMembers.clanId eq clanId) and
                (ClanMembers.userId eq userId) and
                (ClanMembers.isActive eq true)
            }.firstOrNull() ?: throw IllegalStateException("User is not a member of this clan")
            
            val clan = Clans.select { Clans.id eq clanId }.firstOrNull()
                ?: throw IllegalStateException("Clan not found")
            
            // If user is creator and there are other members, check if there are other admins
            if (clan[Clans.creatorId] == userId && clan[Clans.currentMembers] > 1) {
                val otherAdmins = ClanMembers.select {
                    (ClanMembers.clanId eq clanId) and
                    (ClanMembers.userId neq userId) and
                    (ClanMembers.role eq "ADMIN") and
                    (ClanMembers.isActive eq true)
                }.count()
                
                if (otherAdmins == 0L) {
                    throw IllegalStateException("Cannot leave clan as creator without assigning another admin")
                }
            }
            
            // Remove member
            ClanMembers.update({
                (ClanMembers.clanId eq clanId) and (ClanMembers.userId eq userId)
            }) {
                it[ClanMembers.isActive] = false
            }
            
            // Update clan member count
            Clans.update({ Clans.id eq clanId }) {
                it[Clans.currentMembers] = clan[Clans.currentMembers] - 1
            }
            
            // If clan is now empty, deactivate it
            if (clan[Clans.currentMembers] - 1 == 0) {
                Clans.update({ Clans.id eq clanId }) {
                    it[Clans.isActive] = false
                }
            }
        }
    }
    
    /**
     * Get clan members
     */
    fun getClanMembers(clanId: Long): List<ClanMember> {
        return dbTransaction {
            ClanMembers.select {
                (ClanMembers.clanId eq clanId) and (ClanMembers.isActive eq true)
            }
            .orderBy(ClanMembers.contributedFocusHours to SortOrder.DESC)
            .map { row ->
                ClanMember(
                    clanId = row[ClanMembers.clanId],
                    userId = row[ClanMembers.userId],
                    username = row[ClanMembers.username],
                    role = row[ClanMembers.role],
                    contributedFocusHours = row[ClanMembers.contributedFocusHours],
                    joinedAt = row[ClanMembers.joinedAt].toString(),
                    lastActiveAt = row[ClanMembers.lastActiveAt].toString(),
                    isActive = row[ClanMembers.isActive]
                )
            }
        }
    }
    
    /**
     * Update member role
     */
    fun updateMemberRole(clanId: Long, userId: String, newRole: String) {
        dbTransaction {
            ClanMembers.update({
                (ClanMembers.clanId eq clanId) and (ClanMembers.userId eq userId)
            }) {
                it[ClanMembers.role] = newRole
            }
        }
    }
    
    /**
     * Remove member from clan
     */
    fun removeMember(clanId: Long, userId: String) {
        dbTransaction {
            val clan = Clans.select { Clans.id eq clanId }.firstOrNull()
                ?: throw IllegalStateException("Clan not found")
            
            ClanMembers.update({
                (ClanMembers.clanId eq clanId) and (ClanMembers.userId eq userId)
            }) {
                it[ClanMembers.isActive] = false
            }
            
            // Update clan member count
            Clans.update({ Clans.id eq clanId }) {
                it[Clans.currentMembers] = clan[Clans.currentMembers] - 1
            }
        }
    }
    
    /**
     * Update clan stats with focus time
     */
    fun updateClanStatsWithFocusTime(
        userId: String,
        focusDuration: Long,
        date: LocalDate
    ) {
        dbTransaction {
            // Get all user's clan memberships
            val memberships = ClanMembers.select {
                (ClanMembers.userId eq userId) and (ClanMembers.isActive eq true)
            }
            
            // Update stats for all clans the user is a member of
            for (membership in memberships) {
                val clanId = membership[ClanMembers.clanId]
                
                // Update member's contributed hours
                ClanMembers.update({
                    (ClanMembers.clanId eq clanId) and (ClanMembers.userId eq userId)
                }) {
                    it[ClanMembers.contributedFocusHours] = membership[ClanMembers.contributedFocusHours] + focusDuration
                    it[ClanMembers.lastActiveAt] = Clock.System.now()
                }
                
                // Update clan's total focus hours
                val clan = Clans.select { Clans.id eq clanId }.first()
                Clans.update({ Clans.id eq clanId }) {
                    it[Clans.totalFocusHours] = clan[Clans.totalFocusHours] + focusDuration
                    it[Clans.updatedAt] = Clock.System.now()
                }
                
                // Update daily stats
                updateClanDailyStats(clanId, date, focusDuration)
                
                // Update weekly and monthly stats
                updateClanWeeklyStats(clanId, date)
                updateClanMonthlyStats(clanId, date)
            }
        }
    }
    
    private fun updateClanDailyStats(clanId: Long, date: LocalDate, focusDuration: Long) {
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        
        val existing = ClanStats.select {
            (ClanStats.clanId eq clanId) and
            (ClanStats.period eq "daily") and
            (ClanStats.periodDate eq dateStr)
        }.firstOrNull()
        
        if (existing != null) {
            ClanStats.update({
                (ClanStats.clanId eq clanId) and
                (ClanStats.period eq "daily") and
                (ClanStats.periodDate eq dateStr)
            }) {
                it[ClanStats.totalFocusHours] = existing[ClanStats.totalFocusHours] + focusDuration
                it[ClanStats.updatedAt] = Clock.System.now()
            }
        } else {
            // Count active members for this day
            val activeMembersCount = ClanMembers.select {
                (ClanMembers.clanId eq clanId) and (ClanMembers.isActive eq true)
            }.count().toInt()
            
            ClanStats.insert {
                it[ClanStats.clanId] = clanId
                it[ClanStats.period] = "daily"
                it[ClanStats.periodDate] = dateStr
                it[ClanStats.totalFocusHours] = focusDuration
                it[ClanStats.activeMembersCount] = activeMembersCount
            }
        }
    }
    
    private fun updateClanWeeklyStats(clanId: Long, date: LocalDate) {
        val weekDate = getWeekDate(date)
        val (weekStart, weekEnd) = parseWeekDate(weekDate)
        
        val datesInWeek = mutableListOf<String>()
        var currentDate = weekStart
        while (!currentDate.isAfter(weekEnd)) {
            datesInWeek.add(currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
            currentDate = currentDate.plusDays(1)
        }
        
        // Sum all daily stats for this week
        val weeklyTotal = ClanStats.select {
            (ClanStats.clanId eq clanId) and
            (ClanStats.period eq "daily") and
            (ClanStats.periodDate inList datesInWeek)
        }.sumOf { it[ClanStats.totalFocusHours] }
        
        val activeMembersCount = ClanMembers.select {
            (ClanMembers.clanId eq clanId) and (ClanMembers.isActive eq true)
        }.count().toInt()
        
        // Update or create weekly stats
        val existing = ClanStats.select {
            (ClanStats.clanId eq clanId) and
            (ClanStats.period eq "weekly") and
            (ClanStats.periodDate eq weekDate)
        }.firstOrNull()
        
        if (existing != null) {
            ClanStats.update({
                (ClanStats.clanId eq clanId) and
                (ClanStats.period eq "weekly") and
                (ClanStats.periodDate eq weekDate)
            }) {
                it[ClanStats.totalFocusHours] = weeklyTotal
                it[ClanStats.activeMembersCount] = activeMembersCount
                it[ClanStats.updatedAt] = Clock.System.now()
            }
        } else {
            ClanStats.insert {
                it[ClanStats.clanId] = clanId
                it[ClanStats.period] = "weekly"
                it[ClanStats.periodDate] = weekDate
                it[ClanStats.totalFocusHours] = weeklyTotal
                it[ClanStats.activeMembersCount] = activeMembersCount
            }
        }
    }
    
    private fun updateClanMonthlyStats(clanId: Long, date: LocalDate) {
        val monthDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val (monthStart, monthEnd) = parseMonthDate(monthDate)
        
        val datesInMonth = mutableListOf<String>()
        var currentDate = monthStart
        while (!currentDate.isAfter(monthEnd)) {
            datesInMonth.add(currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
            currentDate = currentDate.plusDays(1)
        }
        
        // Sum all daily stats for this month
        val monthlyTotal = ClanStats.select {
            (ClanStats.clanId eq clanId) and
            (ClanStats.period eq "daily") and
            (ClanStats.periodDate inList datesInMonth)
        }.sumOf { it[ClanStats.totalFocusHours] }
        
        val activeMembersCount = ClanMembers.select {
            (ClanMembers.clanId eq clanId) and (ClanMembers.isActive eq true)
        }.count().toInt()
        
        // Update or create monthly stats
        val existing = ClanStats.select {
            (ClanStats.clanId eq clanId) and
            (ClanStats.period eq "monthly") and
            (ClanStats.periodDate eq monthDate)
        }.firstOrNull()
        
        if (existing != null) {
            ClanStats.update({
                (ClanStats.clanId eq clanId) and
                (ClanStats.period eq "monthly") and
                (ClanStats.periodDate eq monthDate)
            }) {
                it[ClanStats.totalFocusHours] = monthlyTotal
                it[ClanStats.activeMembersCount] = activeMembersCount
                it[ClanStats.updatedAt] = Clock.System.now()
            }
        } else {
            ClanStats.insert {
                it[ClanStats.clanId] = clanId
                it[ClanStats.period] = "monthly"
                it[ClanStats.periodDate] = monthDate
                it[ClanStats.totalFocusHours] = monthlyTotal
                it[ClanStats.activeMembersCount] = activeMembersCount
            }
        }
    }
    
    /**
     * Get clan stats for a specific period
     */
    fun getClanStatsByPeriod(clanId: Long, period: String, periodDate: String): Long {
        return dbTransaction {
            val stats = ClanStats.select {
                (ClanStats.clanId eq clanId) and
                (ClanStats.period eq period) and
                (ClanStats.periodDate eq periodDate)
            }.firstOrNull()
            
            stats?.get(ClanStats.totalFocusHours) ?: 0L
        }
    }
    
    private fun getWeekDate(date: LocalDate): String {
        val weekFields = WeekFields.of(Locale.getDefault())
        val week = date.get(weekFields.weekOfWeekBasedYear())
        val year = date.get(weekFields.weekBasedYear())
        return "${year}-W${String.format("%02d", week)}"
    }
    
    private fun parseWeekDate(weekDate: String): Pair<LocalDate, LocalDate> {
        val parts = weekDate.split("-W")
        val year = parts[0].toInt()
        val week = parts[1].toInt()
        
        val weekFields = WeekFields.of(Locale.getDefault())
        var date = LocalDate.of(year, 1, 1)
        var currentWeek = date.get(weekFields.weekOfWeekBasedYear())
        var currentYear = date.get(weekFields.weekBasedYear())
        
        while (currentYear < year || (currentYear == year && currentWeek < week)) {
            date = date.plusWeeks(1)
            currentWeek = date.get(weekFields.weekOfWeekBasedYear())
            currentYear = date.get(weekFields.weekBasedYear())
        }
        
        val weekStart = date.with(weekFields.dayOfWeek(), 1)
        val weekEnd = weekStart.plusDays(6)
        
        return Pair(weekStart, weekEnd)
    }
    
    private fun parseMonthDate(monthDate: String): Pair<LocalDate, LocalDate> {
        val parts = monthDate.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        
        val monthStart = LocalDate.of(year, month, 1)
        val monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth())
        
        return Pair(monthStart, monthEnd)
    }
    
    /**
     * Get clan leaderboard
     */
    fun getClanLeaderboard(
        period: String,
        periodDate: String,
        limit: Int = 20,
        userClanId: Long? = null
    ): Pair<List<ClanLeaderboardEntry>, Int?> {
        return dbTransaction {
            val entries = (Clans innerJoin ClanStats)
                .select {
                    (ClanStats.period eq period) and
                    (ClanStats.periodDate eq periodDate) and
                    (Clans.isActive eq true)
                }
                .orderBy(ClanStats.totalFocusHours to SortOrder.DESC)
                .limit(limit)
                .mapIndexed { index, row ->
                    ClanLeaderboardEntry(
                        rank = index + 1,
                        clanId = row[Clans.id].value,
                        clanName = row[Clans.name],
                        clanLogoUrl = row[Clans.logoUrl],
                        totalFocusHours = row[ClanStats.totalFocusHours],
                        activeMembersCount = row[ClanStats.activeMembersCount],
                        currentMembers = row[Clans.currentMembers],
                        category = row[Clans.category],
                        city = row[Clans.city],
                        country = row[Clans.country]
                    )
                }
            
            // Find user's clan rank if provided
            val userClanRank = if (userClanId != null) {
                val allRankedClans = (Clans innerJoin ClanStats)
                    .select {
                        (ClanStats.period eq period) and
                        (ClanStats.periodDate eq periodDate) and
                        (Clans.isActive eq true)
                    }
                    .orderBy(ClanStats.totalFocusHours to SortOrder.DESC)
                    .mapIndexed { index, row ->
                        Pair(row[Clans.id].value, index + 1)
                    }
                    .toMap()

                allRankedClans[userClanId]
            } else {
                null
            }
            
            Pair(entries, userClanRank)
        }
    }
    
    /**
     * Get user's clan info (returns first clan for backward compatibility)
     */
    fun getUserClanInfo(userId: String): Pair<Clan?, ClanMember?> {
        return dbTransaction {
            val membership = ClanMembers.select {
                (ClanMembers.userId eq userId) and (ClanMembers.isActive eq true)
            }.firstOrNull()
            
            if (membership == null) {
                return@dbTransaction Pair(null, null)
            }
            
            val clanId = membership[ClanMembers.clanId]
            val clan = getClanById(clanId, userId)
            
            val member = ClanMember(
                clanId = membership[ClanMembers.clanId],
                userId = membership[ClanMembers.userId],
                username = membership[ClanMembers.username],
                role = membership[ClanMembers.role],
                contributedFocusHours = membership[ClanMembers.contributedFocusHours],
                joinedAt = membership[ClanMembers.joinedAt].toString(),
                lastActiveAt = membership[ClanMembers.lastActiveAt].toString(),
                isActive = membership[ClanMembers.isActive]
            )
            
            Pair(clan, member)
        }
    }
    
    /**
     * Get all clans a user is a member of
     */
    fun getUserClans(userId: String): List<Pair<Clan, ClanMember>> {
        return dbTransaction {
            val memberships = ClanMembers.select {
                (ClanMembers.userId eq userId) and (ClanMembers.isActive eq true)
            }.map { row ->
                val clanId = row[ClanMembers.clanId]
                val clan = getClanById(clanId, userId) ?: return@dbTransaction emptyList()
                
                val member = ClanMember(
                    clanId = row[ClanMembers.clanId],
                    userId = row[ClanMembers.userId],
                    username = row[ClanMembers.username],
                    role = row[ClanMembers.role],
                    contributedFocusHours = row[ClanMembers.contributedFocusHours],
                    joinedAt = row[ClanMembers.joinedAt].toString(),
                    lastActiveAt = row[ClanMembers.lastActiveAt].toString(),
                    isActive = row[ClanMembers.isActive]
                )
                
                Pair(clan, member)
            }
            
            memberships
        }
    }
    
    /**
     * Get user's membership in a specific clan
     */
    fun getUserClanMembership(userId: String, clanId: Long): ClanMember? {
        return dbTransaction {
            val membership = ClanMembers.select {
                (ClanMembers.clanId eq clanId) and
                (ClanMembers.userId eq userId) and
                (ClanMembers.isActive eq true)
            }.firstOrNull()
            
            if (membership == null) {
                return@dbTransaction null
            }
            
            ClanMember(
                clanId = membership[ClanMembers.clanId],
                userId = membership[ClanMembers.userId],
                username = membership[ClanMembers.username],
                role = membership[ClanMembers.role],
                contributedFocusHours = membership[ClanMembers.contributedFocusHours],
                joinedAt = membership[ClanMembers.joinedAt].toString(),
                lastActiveAt = membership[ClanMembers.lastActiveAt].toString(),
                isActive = membership[ClanMembers.isActive]
            )
        }
    }
    
    /**
     * Create clan invite
     */
    fun createInvite(
        clanId: Long,
        inviterId: String,
        inviteeUserId: String?,
        maxUses: Int,
        expiresAt: Instant?
    ): ClanInvite {
        return dbTransaction {
            val inviteCode = generateInviteCode()
            
            val inviteId = ClanInvites.insertAndGetId {
                it[ClanInvites.clanId] = clanId
                it[ClanInvites.inviterId] = inviterId
                it[ClanInvites.inviteeUserId] = inviteeUserId
                it[ClanInvites.inviteCode] = inviteCode
                it[ClanInvites.maxUses] = maxUses
                it[ClanInvites.expiresAt] = expiresAt
            }.value
            
            val invite = ClanInvites.select { ClanInvites.id eq inviteId }.first()
            val clan = Clans.select { Clans.id eq clanId }.firstOrNull()
            val inviter = Users.select { Users.userId eq inviterId }.firstOrNull()
            
            ClanInvite(
                clanId = invite[ClanInvites.clanId],
                clanName = clan?.get(Clans.name),
                inviterId = invite[ClanInvites.inviterId],
                inviterUsername = inviter?.get(Users.username),
                inviteeUserId = invite[ClanInvites.inviteeUserId],
                inviteCode = invite[ClanInvites.inviteCode],
                status = invite[ClanInvites.status],
                maxUses = invite[ClanInvites.maxUses],
                currentUses = invite[ClanInvites.currentUses],
                expiresAt = invite[ClanInvites.expiresAt]?.toString(),
                acceptedAt = invite[ClanInvites.acceptedAt]?.toString(),
                createdAt = invite[ClanInvites.createdAt].toString()
            )
        }
    }
    
    /**
     * Accept invite and join clan
     */
    fun acceptInvite(inviteCode: String, userId: String): ClanMember {
        return dbTransaction {
            val invite = ClanInvites.select { ClanInvites.inviteCode eq inviteCode }
                .firstOrNull() ?: throw IllegalStateException("Invalid invite code")
            
            // Validate invite
            if (invite[ClanInvites.status] != "PENDING") {
                throw IllegalStateException("Invite is no longer valid")
            }
            
            if (invite[ClanInvites.expiresAt] != null && invite[ClanInvites.expiresAt]!! < Clock.System.now()) {
                ClanInvites.update({ ClanInvites.inviteCode eq inviteCode }) {
                    it[ClanInvites.status] = "EXPIRED"
                }
                throw IllegalStateException("Invite has expired")
            }
            
            if (invite[ClanInvites.maxUses] != -1 && invite[ClanInvites.currentUses] >= invite[ClanInvites.maxUses]) {
                throw IllegalStateException("Invite has reached maximum uses")
            }
            
            // If invite is for specific user, validate
            if (invite[ClanInvites.inviteeUserId] != null && invite[ClanInvites.inviteeUserId] != userId) {
                throw IllegalStateException("This invite is for a different user")
            }
            
            val clanId = invite[ClanInvites.clanId]
            
            // Join clan
            val member = joinClan(clanId, userId)
            
            // Update invite
            ClanInvites.update({ ClanInvites.inviteCode eq inviteCode }) {
                it[ClanInvites.currentUses] = invite[ClanInvites.currentUses] + 1
                it[ClanInvites.acceptedAt] = Clock.System.now()
                if (invite[ClanInvites.maxUses] != -1 && invite[ClanInvites.currentUses] + 1 >= invite[ClanInvites.maxUses]) {
                    it[ClanInvites.status] = "ACCEPTED"
                }
            }
            
            member
        }
    }
    
    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars.random() }.joinToString("")
    }
    
    private fun generateShareCode(): String {
        val random = java.security.SecureRandom()
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Exclude confusing characters
        return (1..8).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
    
    /**
     * Create or get existing clan share record
     * Returns the share code for tracking
     */
    fun createOrGetClanShare(clanId: Long, sharerUserId: String): String {
        return dbTransaction {
            // Check if user already shared this clan
            val existingShare = ClanShares.select {
                (ClanShares.clanId eq clanId) and
                (ClanShares.sharerUserId eq sharerUserId)
            }.firstOrNull()
            
            if (existingShare != null) {
                return@dbTransaction existingShare[ClanShares.shareCode]
            }
            
            // Generate unique share code
            var shareCode = generateShareCode()
            var attempts = 0
            while (ClanShares.select { ClanShares.shareCode eq shareCode }.count() > 0 && attempts < 10) {
                shareCode = generateShareCode()
                attempts++
            }
            
            if (attempts >= 10) {
                throw IllegalStateException("Failed to generate unique share code")
            }
            
            // Create new share record
            ClanShares.insert {
                it[ClanShares.clanId] = clanId
                it[ClanShares.sharerUserId] = sharerUserId
                it[ClanShares.shareCode] = shareCode
                it[ClanShares.clickCount] = 0
                it[ClanShares.joinCount] = 0
            }
            
            shareCode
        }
    }
    
    /**
     * Get share record by share code
     */
    fun getClanShareByCode(shareCode: String): ClanShare? {
        return dbTransaction {
            ClanShares.select { ClanShares.shareCode eq shareCode }
                .firstOrNull()
                ?.let { row ->
                    ClanShare(
                        id = row[ClanShares.id],
                        clanId = row[ClanShares.clanId],
                        sharerUserId = row[ClanShares.sharerUserId],
                        shareCode = row[ClanShares.shareCode],
                        clickCount = row[ClanShares.clickCount],
                        joinCount = row[ClanShares.joinCount],
                        createdAt = row[ClanShares.createdAt].toString(),
                        updatedAt = row[ClanShares.updatedAt].toString()
                    )
                }
        }
    }
    
    /**
     * Track a clan share event (click, join, app_open, etc.)
     */
    fun trackClanShareEvent(
        shareCode: String,
        eventType: String,
        joinerUserId: String? = null,
        deviceId: String? = null,
        userAgent: String? = null,
        ipAddress: String? = null
    ) {
        dbTransaction {
            val share = ClanShares.select { ClanShares.shareCode eq shareCode }.firstOrNull()
                ?: throw IllegalArgumentException("Invalid share code")
            
            val shareId = share[ClanShares.id]
            
            // Insert event
            ClanShareEvents.insert {
                it[ClanShareEvents.shareId] = shareId
                it[ClanShareEvents.eventType] = eventType
                it[ClanShareEvents.joinerUserId] = joinerUserId
                it[ClanShareEvents.deviceId] = deviceId
                it[ClanShareEvents.userAgent] = userAgent
                it[ClanShareEvents.ipAddress] = ipAddress
            }
            
            // Update counters
            when (eventType) {
                "CLICK" -> {
                    ClanShares.update({ ClanShares.id eq shareId }) {
                        it[ClanShares.clickCount] = share[ClanShares.clickCount] + 1
                        it[ClanShares.updatedAt] = Clock.System.now()
                    }
                }
                "JOIN", "APP_OPEN" -> {
                    ClanShares.update({ ClanShares.id eq shareId }) {
                        it[ClanShares.joinCount] = share[ClanShares.joinCount] + 1
                        it[ClanShares.updatedAt] = Clock.System.now()
                    }
                }
                else -> {}
            }
        }
    }
    
    /**
     * Get share statistics for a user
     */
    fun getUserClanShareStats(userId: String): ClanShareStats {
        return dbTransaction {
            val shares = ClanShares.select { ClanShares.sharerUserId eq userId }
            
            val totalShares = shares.count()
            val totalClicks = shares.sumOf { it[ClanShares.clickCount] }
            val totalJoins = shares.sumOf { it[ClanShares.joinCount] }
            
            ClanShareStats(
                totalShares = totalShares,
                totalClicks = totalClicks,
                totalJoins = totalJoins
            )
        }
    }
    
    /**
     * Data class for clan share
     */
    data class ClanShare(
        val id: Long,
        val clanId: Long,
        val sharerUserId: String,
        val shareCode: String,
        val clickCount: Int,
        val joinCount: Int,
        val createdAt: String,
        val updatedAt: String
    )
    
    /**
     * Data class for clan share stats
     */
    data class ClanShareStats(
        val totalShares: Long,
        val totalClicks: Int,
        val totalJoins: Int
    )
    
    /**
     * Get or create a permanent share link for a clan
     * Returns an invite code that never expires and has unlimited uses
     */
    fun getOrCreateShareLink(clanId: Long, inviterId: String): ClanInvite {
        return dbTransaction {
            // Check if there's already a permanent share link (unlimited uses, no expiration, no specific invitee)
            val existingShareLink = ClanInvites.select {
                (ClanInvites.clanId eq clanId) and
                (ClanInvites.maxUses eq -1) and
                (ClanInvites.expiresAt.isNull()) and
                (ClanInvites.inviteeUserId.isNull()) and
                (ClanInvites.status eq "PENDING")
            }.firstOrNull()
            
            if (existingShareLink != null) {
                // Return existing share link
                val clan = Clans.select { Clans.id eq clanId }.firstOrNull()
                val inviter = Users.select { Users.userId eq inviterId }.firstOrNull()
                
                ClanInvite(
                    clanId = existingShareLink[ClanInvites.clanId],
                    clanName = clan?.get(Clans.name),
                    inviterId = existingShareLink[ClanInvites.inviterId],
                    inviterUsername = inviter?.get(Users.username),
                    inviteeUserId = existingShareLink[ClanInvites.inviteeUserId],
                    inviteCode = existingShareLink[ClanInvites.inviteCode],
                    status = existingShareLink[ClanInvites.status],
                    maxUses = existingShareLink[ClanInvites.maxUses],
                    currentUses = existingShareLink[ClanInvites.currentUses],
                    expiresAt = existingShareLink[ClanInvites.expiresAt]?.toString(),
                    acceptedAt = existingShareLink[ClanInvites.acceptedAt]?.toString(),
                    createdAt = existingShareLink[ClanInvites.createdAt].toString()
                )
            } else {
                // Create new permanent share link
                val inviteCode = generateInviteCode()
                
                val inviteId = ClanInvites.insertAndGetId {
                    it[ClanInvites.clanId] = clanId
                    it[ClanInvites.inviterId] = inviterId
                    it[ClanInvites.inviteeUserId] = null
                    it[ClanInvites.inviteCode] = inviteCode
                    it[ClanInvites.maxUses] = -1 // Unlimited
                    it[ClanInvites.expiresAt] = null // Never expires
                }.value
                
                val invite = ClanInvites.select { ClanInvites.id eq inviteId }.first()
                val clan = Clans.select { Clans.id eq clanId }.firstOrNull()
                val inviter = Users.select { Users.userId eq inviterId }.firstOrNull()
                
                ClanInvite(
                    clanId = invite[ClanInvites.clanId],
                    clanName = clan?.get(Clans.name),
                    inviterId = invite[ClanInvites.inviterId],
                    inviterUsername = inviter?.get(Users.username),
                    inviteeUserId = invite[ClanInvites.inviteeUserId],
                    inviteCode = invite[ClanInvites.inviteCode],
                    status = invite[ClanInvites.status],
                    maxUses = invite[ClanInvites.maxUses],
                    currentUses = invite[ClanInvites.currentUses],
                    expiresAt = invite[ClanInvites.expiresAt]?.toString(),
                    acceptedAt = invite[ClanInvites.acceptedAt]?.toString(),
                    createdAt = invite[ClanInvites.createdAt].toString()
                )
            }
        }
    }
    
    /**
     * Create join request for private clans
     */
    fun createJoinRequest(clanId: Long, userId: String, message: String?): ClanJoinRequest {
        return dbTransaction {
            val username = Users.select { Users.userId eq userId }
                .firstOrNull()
                ?.get(Users.username)
            
            val requestId = ClanJoinRequests.insertAndGetId {
                it[ClanJoinRequests.clanId] = clanId
                it[ClanJoinRequests.userId] = userId
                it[ClanJoinRequests.username] = username
                it[ClanJoinRequests.message] = message
            }.value
            
            val request = ClanJoinRequests.select { ClanJoinRequests.id eq requestId }.first()
            ClanJoinRequest(
                clanId = request[ClanJoinRequests.clanId],
                userId = request[ClanJoinRequests.userId],
                username = request[ClanJoinRequests.username],
                message = request[ClanJoinRequests.message],
                status = request[ClanJoinRequests.status],
                reviewedBy = request[ClanJoinRequests.reviewedBy],
                reviewedAt = request[ClanJoinRequests.reviewedAt]?.toString(),
                createdAt = request[ClanJoinRequests.createdAt].toString()
            )
        }
    }
    
    /**
     * Get join request by ID
     */
    fun getJoinRequestById(requestId: Long): ClanJoinRequest? {
        return dbTransaction {
            val request = ClanJoinRequests.select { ClanJoinRequests.id eq requestId }
                .firstOrNull() ?: return@dbTransaction null
            
            ClanJoinRequest(
                clanId = request[ClanJoinRequests.clanId],
                userId = request[ClanJoinRequests.userId],
                username = request[ClanJoinRequests.username],
                message = request[ClanJoinRequests.message],
                status = request[ClanJoinRequests.status],
                reviewedBy = request[ClanJoinRequests.reviewedBy],
                reviewedAt = request[ClanJoinRequests.reviewedAt]?.toString(),
                createdAt = request[ClanJoinRequests.createdAt].toString()
            )
        }
    }
    
    /**
     * Review join request
     */
    fun reviewJoinRequest(requestId: Long, reviewerId: String, approved: Boolean): ClanMember? {
        return dbTransaction {
            val request = ClanJoinRequests.select { ClanJoinRequests.id eq requestId }
                .firstOrNull() ?: throw IllegalStateException("Join request not found")
            
            if (request[ClanJoinRequests.status] != "PENDING") {
                throw IllegalStateException("Join request has already been reviewed")
            }
            
            ClanJoinRequests.update({ ClanJoinRequests.id eq requestId }) {
                it[ClanJoinRequests.status] = if (approved) "APPROVED" else "REJECTED"
                it[ClanJoinRequests.reviewedBy] = reviewerId
                it[ClanJoinRequests.reviewedAt] = Clock.System.now()
            }
            
            if (approved) {
                joinClan(request[ClanJoinRequests.clanId], request[ClanJoinRequests.userId])
            } else {
                null
            }
        }
    }
    
    /**
     * Get pending join requests for a clan
     */
    fun getPendingJoinRequests(clanId: Long): List<ClanJoinRequest> {
        return dbTransaction {
            ClanJoinRequests.select {
                (ClanJoinRequests.clanId eq clanId) and
                (ClanJoinRequests.status eq "PENDING")
            }
            .orderBy(ClanJoinRequests.createdAt to SortOrder.DESC)
            .map { row ->
                ClanJoinRequest(
                    clanId = row[ClanJoinRequests.clanId],
                    userId = row[ClanJoinRequests.userId],
                    username = row[ClanJoinRequests.username],
                    message = row[ClanJoinRequests.message],
                    status = row[ClanJoinRequests.status],
                    reviewedBy = row[ClanJoinRequests.reviewedBy],
                    reviewedAt = row[ClanJoinRequests.reviewedAt]?.toString(),
                    createdAt = row[ClanJoinRequests.createdAt].toString()
                )
            }
        }
    }
    
    /**
     * Add badge to clan
     */
    fun addClanBadge(
        clanId: Long,
        badgeType: String,
        title: String,
        description: String?,
        iconUrl: String?,
        metadata: String?
    ): ClanBadge {
        return dbTransaction {
            val badgeId = ClanBadges.insertAndGetId {
                it[ClanBadges.clanId] = clanId
                it[ClanBadges.badgeType] = badgeType
                it[ClanBadges.title] = title
                it[ClanBadges.description] = description
                it[ClanBadges.iconUrl] = iconUrl
                it[ClanBadges.metadata] = metadata
            }.value
            
            val badge = ClanBadges.select { ClanBadges.id eq badgeId }.first()
            ClanBadge(
                clanId = badge[ClanBadges.clanId],
                badgeType = badge[ClanBadges.badgeType],
                title = badge[ClanBadges.title],
                description = badge[ClanBadges.description],
                iconUrl = badge[ClanBadges.iconUrl],
                metadata = badge[ClanBadges.metadata],
                earnedAt = badge[ClanBadges.earnedAt].toString()
            )
        }
    }
    
    /**
     * Get clan badges
     */
    fun getClanBadges(clanId: Long): List<ClanBadge> {
        return dbTransaction {
            ClanBadges.select { ClanBadges.clanId eq clanId }
                .orderBy(ClanBadges.earnedAt to SortOrder.DESC)
                .map { row ->
                    ClanBadge(
                        clanId = row[ClanBadges.clanId],
                        badgeType = row[ClanBadges.badgeType],
                        title = row[ClanBadges.title],
                        description = row[ClanBadges.description],
                        iconUrl = row[ClanBadges.iconUrl],
                        metadata = row[ClanBadges.metadata],
                        earnedAt = row[ClanBadges.earnedAt].toString()
                    )
                }
        }
    }
    
    /**
     * Get app usage analytics for clan members
     * Aggregates app usage by category, top apps, and member activity
     */
    fun getClanAppUsageAnalytics(
        clanId: Long,
        period: String, // "daily", "weekly", "monthly"
        periodDate: String // date string based on period
    ): ClanAppUsageAnalytics {
        return dbTransaction {
            // Get all active clan members
            val members = ClanMembers.select {
                (ClanMembers.clanId eq clanId) and (ClanMembers.isActive eq true)
            }.map { it[ClanMembers.userId] }
            
            if (members.isEmpty()) {
                return@dbTransaction ClanAppUsageAnalytics(
                    period = period,
                    periodDate = periodDate,
                    totalScreenTime = 0L,
                    categoryBreakdown = emptyList(),
                    topApps = emptyList(),
                    memberActivity = emptyList()
                )
            }
            
            // Calculate date range based on period
            val (startDate, endDate) = when (period) {
                "daily" -> {
                    val date = LocalDate.parse(periodDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    Pair(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                }
                "weekly" -> {
                    val weekFields = WeekFields.of(Locale.getDefault())
                    val parts = periodDate.split("-W")
                    val year = parts[0].toInt()
                    val week = parts[1].toInt()
                    val date = LocalDate.of(year, 1, 1)
                    val weekStart = date.with(weekFields.weekOfWeekBasedYear(), week.toLong())
                        .with(weekFields.dayOfWeek(), 1)
                    val weekEnd = weekStart.plusDays(6).atTime(23, 59, 59)
                    Pair(weekStart.atStartOfDay(), weekEnd)
                }
                "monthly" -> {
                    val parts = periodDate.split("-")
                    val year = parts[0].toInt()
                    val month = parts[1].toInt()
                    val monthStart = LocalDate.of(year, month, 1)
                    val monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth())
                    Pair(monthStart.atStartOfDay(), monthEnd.atTime(23, 59, 59))
                }
                else -> {
                    val date = LocalDate.now()
                    Pair(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                }
            }

            val startInstant = startDate
            val endInstant = endDate


            // Get all app usage events for clan members in the period
            val events = AppUsageEvents.select {
                (AppUsageEvents.userId inList members) and
                (AppUsageEvents.eventTimestamp greaterEq startInstant) and
                (AppUsageEvents.eventTimestamp lessEq endInstant) and
                (AppUsageEvents.duration.isNotNull())
            }.toList()
            
            // Aggregate by package/app
            val appUsageMap = mutableMapOf<String, AppUsageData>()
            val userAppMap = mutableMapOf<String, MutableSet<String>>() // package -> set of userIds
            
            for (event in events) {
                val packageName = event[AppUsageEvents.packageName]
                val appName = event[AppUsageEvents.appName]
                val duration = event[AppUsageEvents.duration] ?: 0L
                val userId = event[AppUsageEvents.userId]
                
                val key = packageName
                if (!appUsageMap.containsKey(key)) {
                    appUsageMap[key] = AppUsageData(
                        packageName = packageName,
                        appName = appName,
                        totalTime = 0L,
                        userCount = 0
                    )
                    userAppMap[key] = mutableSetOf()
                }
                
                appUsageMap[key] = appUsageMap[key]!!.copy(
                    totalTime = appUsageMap[key]!!.totalTime + duration
                )
                userAppMap[key]!!.add(userId)
            }
            
            // Update user counts
            appUsageMap.forEach { (key, data) ->
                appUsageMap[key] = data.copy(userCount = userAppMap[key]!!.size)
            }
            
            // Aggregate by category (simple mapping based on package name patterns)
            val categoryUsageMap = mutableMapOf<String?, CategoryUsageData>()
            val categoryUserMap = mutableMapOf<String?, MutableSet<String>>()
            
            for ((packageName, appData) in appUsageMap) {
                val category = inferCategory(packageName, appData.appName)
                
                if (!categoryUsageMap.containsKey(category)) {
                    categoryUsageMap[category] = CategoryUsageData(
                        category = category,
                        totalTime = 0L,
                        memberCount = 0
                    )
                    categoryUserMap[category] = mutableSetOf()
                }
                
                categoryUsageMap[category] = categoryUsageMap[category]!!.copy(
                    totalTime = categoryUsageMap[category]!!.totalTime + appData.totalTime
                )
                
                // Add all users who used apps in this category
                userAppMap[packageName]?.forEach { userId ->
                    categoryUserMap[category]!!.add(userId)
                }
            }
            
            // Update member counts
            categoryUsageMap.forEach { (category, data) ->
                categoryUsageMap[category] = data.copy(memberCount = categoryUserMap[category]!!.size)
            }
            
            val totalScreenTime = categoryUsageMap.values.sumOf { it.totalTime }
            
            // Calculate percentages
            val categoryBreakdown = categoryUsageMap.values.map { data ->
                val percentage = if (totalScreenTime > 0) {
                    (data.totalTime.toDouble() / totalScreenTime.toDouble()) * 100.0
                } else {
                    0.0
                }
                CategoryUsage(
                    category = data.category,
                    totalTime = data.totalTime,
                    percentage = percentage,
                    memberCount = data.memberCount
                )
            }.sortedByDescending { it.totalTime }
            
            // Top apps
            val topApps = appUsageMap.values
                .map { data ->
                    val avgTime = if (data.userCount > 0) {
                        data.totalTime / data.userCount
                    } else {
                        0L
                    }
                    TopAppUsage(
                        packageName = data.packageName,
                        appName = data.appName,
                        totalTime = data.totalTime,
                        userCount = data.userCount,
                        averageTime = avgTime
                    )
                }
                .sortedByDescending { it.totalTime }
                .take(20)
            
            // Member activity
            val memberActivityMap = mutableMapOf<String, MemberActivityData>()
            val memberAppMap = mutableMapOf<String, MutableSet<String>>()
            val memberCategoryMap = mutableMapOf<String, MutableSet<String?>>()
            
            for (event in events) {
                val userId = event[AppUsageEvents.userId]
                val packageName = event[AppUsageEvents.packageName]
                val duration = event[AppUsageEvents.duration] ?: 0L
                val appName = event[AppUsageEvents.appName]
                val category = inferCategory(packageName, appName)
                
                if (!memberActivityMap.containsKey(userId)) {
                    memberActivityMap[userId] = MemberActivityData(
                        userId = userId,
                        totalScreenTime = 0L,
                        appCount = 0,
                        categoryCount = 0
                    )
                    memberAppMap[userId] = mutableSetOf()
                    memberCategoryMap[userId] = mutableSetOf()
                }
                
                memberActivityMap[userId] = memberActivityMap[userId]!!.copy(
                    totalScreenTime = memberActivityMap[userId]!!.totalScreenTime + duration
                )
                memberAppMap[userId]!!.add(packageName)
                memberCategoryMap[userId]!!.add(category)
            }
            
            // Update counts
            memberActivityMap.forEach { (userId, data) ->
                memberActivityMap[userId] = data.copy(
                    appCount = memberAppMap[userId]!!.size,
                    categoryCount = memberCategoryMap[userId]!!.size
                )
            }
            
            // Get usernames
            val userIds = memberActivityMap.keys.toList()
            val usernameMap = if (userIds.isNotEmpty()) {
                Users.select { Users.userId inList userIds }
                    .associate { it[Users.userId] to it[Users.username] }
            } else {
                emptyMap()
            }
            
            val memberActivity = memberActivityMap.values
                .sortedByDescending { it.totalScreenTime }
                .mapIndexed { index, data ->
                    MemberActivityStats(
                        userId = data.userId,
                        username = usernameMap[data.userId],
                        totalScreenTime = data.totalScreenTime,
                        appCount = data.appCount,
                        categoryCount = data.categoryCount,
                        rank = index + 1
                    )
                }
            
            ClanAppUsageAnalytics(
                period = period,
                periodDate = periodDate,
                totalScreenTime = totalScreenTime,
                categoryBreakdown = categoryBreakdown,
                topApps = topApps,
                memberActivity = memberActivity
            )
        }
    }
    
    /**
     * Infer app category from package name and app name
     * Simple pattern matching - can be enhanced with a proper category database
     */
    private fun inferCategory(packageName: String?, appName: String?): String? {
        val name = (appName ?: packageName ?: "").lowercase()
        val packageLower = packageName?.lowercase() ?: ""
        
        return when {
            name.contains("social") || name.contains("instagram") || name.contains("facebook") || 
            name.contains("twitter") || name.contains("whatsapp") || name.contains("telegram") ||
            name.contains("snapchat") || name.contains("linkedin") || packageLower.contains("com.facebook") ||
            packageLower.contains("com.instagram") || packageLower.contains("com.twitter") ||
            packageLower.contains("com.whatsapp") -> "Social Media"
            
            name.contains("game") || name.contains("play") || packageLower.contains("com.game") ||
            packageLower.contains("com.play") -> "Games"
            
            name.contains("video") || name.contains("youtube") || name.contains("netflix") ||
            name.contains("prime") || name.contains("hotstar") || packageLower.contains("com.youtube") ||
            packageLower.contains("com.netflix") -> "Video & Entertainment"
            
            name.contains("music") || name.contains("spotify") || name.contains("gaana") ||
            packageLower.contains("com.spotify") -> "Music & Audio"
            
            name.contains("book") || name.contains("read") || name.contains("kindle") ||
            packageLower.contains("com.amazon.kindle") -> "Books & Reading"
            
            name.contains("news") || name.contains("times") || name.contains("hindu") ||
            packageLower.contains("com.news") -> "News & Magazines"
            
            name.contains("shop") || name.contains("amazon") || name.contains("flipkart") ||
            name.contains("myntra") || packageLower.contains("com.amazon") ||
            packageLower.contains("com.flipkart") -> "Shopping"
            
            name.contains("food") || name.contains("zomato") || name.contains("swiggy") ||
            name.contains("uber") || packageLower.contains("com.zomato") ||
            packageLower.contains("com.swiggy") -> "Food & Delivery"
            
            name.contains("bank") || name.contains("pay") || name.contains("wallet") ||
            name.contains("upi") || packageLower.contains("com.bank") ||
            packageLower.contains("com.pay") -> "Finance & Banking"
            
            name.contains("health") || name.contains("fitness") || name.contains("workout") ||
            packageLower.contains("com.health") -> "Health & Fitness"
            
            name.contains("education") || name.contains("learn") || name.contains("course") ||
            packageLower.contains("com.education") -> "Education & Learning"
            
            name.contains("travel") || name.contains("booking") || name.contains("makemytrip") ||
            packageLower.contains("com.travel") -> "Travel & Booking"
            
            name.contains("productivity") || name.contains("note") || name.contains("todo") ||
            name.contains("calendar") || packageLower.contains("com.productivity") -> "Productivity"
            
            name.contains("browser") || name.contains("chrome") || name.contains("firefox") ||
            name.contains("safari") || packageLower.contains("com.chrome") ||
            packageLower.contains("com.browser") -> "Browsers"
            
            name.contains("camera") || name.contains("photo") || name.contains("gallery") ||
            packageLower.contains("com.camera") -> "Camera & Photos"
            
            name.contains("message") || name.contains("sms") || name.contains("messenger") ||
            packageLower.contains("com.message") -> "Messaging"
            
            else -> "Other"
        }
    }
    
    /**
     * Data classes for internal use
     */
    private data class AppUsageData(
        val packageName: String,
        val appName: String?,
        val totalTime: Long,
        val userCount: Int
    )
    
    private data class CategoryUsageData(
        val category: String?,
        val totalTime: Long,
        val memberCount: Int
    )
    
    private data class MemberActivityData(
        val userId: String,
        val totalScreenTime: Long,
        val appCount: Int,
        val categoryCount: Int
    )
    
    /**
     * Data class for clan app usage analytics (internal)
     */
    data class ClanAppUsageAnalytics(
        val period: String,
        val periodDate: String,
        val totalScreenTime: Long,
        val categoryBreakdown: List<CategoryUsage>,
        val topApps: List<TopAppUsage>,
        val memberActivity: List<MemberActivityStats>
    )
}

