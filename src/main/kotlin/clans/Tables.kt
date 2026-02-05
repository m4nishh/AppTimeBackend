package com.apptime.code.clans

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Clans table - stores clan/group information
 */
object Clans : LongIdTable("clans") {
    val name = varchar("name", 100).uniqueIndex()
    val description = text("description").nullable()
    val tagline = varchar("tagline", 200).nullable()
    val logoUrl = varchar("logo_url", 500).nullable()
    val clanType = varchar("clan_type", 50).default("PUBLIC")
    val maxMembers = integer("max_members").default(50)
    val currentMembers = integer("current_members").default(0)
    val totalFocusHours = long("total_focus_hours").default(0L)
    val creatorId = varchar("creator_id", 255).index()
    val isActive = bool("is_active").default(true)
    val country = varchar("country", 100).nullable()
    val city = varchar("city", 100).nullable()
    val category = varchar("category", 50).nullable()
    val createdAt = timestamp("created_at")
        .clientDefault { kotlinx.datetime.Clock.System.now() }
    val updatedAt = timestamp("updated_at")
        .clientDefault { kotlinx.datetime.Clock.System.now() }
    init {
        index(false, isActive)
        index(false, clanType)
        index(false, category)
        index(false, country, city)
    }
}


/**
 * Clan Members table - tracks clan membership
 */
object ClanMembers : LongIdTable("clan_members") {

    val clanId = long("clan_id")
        .references(Clans.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val userId = varchar("user_id", 255).index()
    val role = varchar("role", 50).default("MEMBER")
    val username = varchar("username", 255).nullable()
    val contributedFocusHours =
        long("contributed_focus_hours").default(0L)
    val joinedAt = timestamp("joined_at")
        .clientDefault { kotlinx.datetime.Clock.System.now() }
    val lastActiveAt = timestamp("last_active_at")
        .clientDefault { kotlinx.datetime.Clock.System.now() }
    val isActive = bool("is_active").default(true)
    init {
        // One user can only be active in one clan at a time
        uniqueIndex(userId, isActive)

        index(false, clanId, isActive)
    }
}


/**
 * Clan Stats table - stores aggregated clan statistics by period
 */
object ClanStats : Table("clan_stats") {
    val id = long("id").autoIncrement()
    val clanId = long("clan_id").references(Clans.id, onDelete = ReferenceOption.CASCADE).index()
    val period = varchar("period", 20) // "daily", "weekly", "monthly"
    val periodDate = varchar("period_date", 20) // YYYY-MM-DD for daily, YYYY-WW for weekly, YYYY-MM for monthly
    val totalFocusHours = long("total_focus_hours").default(0L) // Total focus hours in this period in milliseconds
    val activeMembersCount = integer("active_members_count").default(0) // Members who contributed in this period
    val rank = integer("rank").nullable() // Rank in leaderboard for this period
    val createdAt = timestamp("created_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        // Unique constraint on clanId + period + periodDate
        uniqueIndex(clanId, period, periodDate)
        index(isUnique = false, period, periodDate)
    }
}

/**
 * Clan Invites table - manages clan invitations
 */
object ClanInvites : LongIdTable("clan_invites") {

    val clanId = long("clan_id")
        .references(Clans.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val inviterId = varchar("inviter_id", 255).index()
    val inviteeUserId = varchar("invitee_user_id", 255)
        .nullable()
        .index()
    val inviteCode = varchar("invite_code", 50).uniqueIndex()
    val status = varchar("status", 50).default("PENDING")
    val maxUses = integer("max_uses").default(1)
    val currentUses = integer("current_uses").default(0)
    val expiresAt = timestamp("expires_at").nullable()
    val acceptedAt = timestamp("accepted_at").nullable()
    val createdAt = timestamp("created_at")
        .clientDefault { kotlinx.datetime.Clock.System.now() }
    init {
        index(false, clanId, status)
        index(false, inviteeUserId, status)
    }
}


/**
 * Clan Badges table - stores clan achievements and badges
 */
object ClanBadges : LongIdTable("clan_badges") {

    val clanId = long("clan_id")
        .references(Clans.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val badgeType = varchar("badge_type", 50)
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val iconUrl = varchar("icon_url", 500).nullable()
    val metadata = text("metadata").nullable()
    val earnedAt = timestamp("earned_at")
        .clientDefault { kotlinx.datetime.Clock.System.now() }

    init {
        index(false, clanId, badgeType)
        index(false, badgeType)
    }
}


/**
 * Clan Join Requests table - for PRIVATE and INVITE_ONLY clans
 */
object ClanJoinRequests : LongIdTable("clan_join_requests") {

    val clanId = long("clan_id")
        .references(Clans.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val userId = varchar("user_id", 255).index()
    val username = varchar("username", 255).nullable()
    val message = text("message").nullable()
    val status = varchar("status", 50).default("PENDING")
    val reviewedBy = varchar("reviewed_by", 255).nullable()
    val reviewedAt = timestamp("reviewed_at").nullable()
    val createdAt = timestamp("created_at")
        .clientDefault { kotlinx.datetime.Clock.System.now() }
    init {
        uniqueIndex(clanId, userId, status)
        index(false, userId, status)
    }
}


