package com.apptime.code.clans

import kotlinx.serialization.Serializable

/**
 * Clan type enum
 */
@Serializable
enum class ClanType {
    PUBLIC,       // Anyone can join
    PRIVATE,      // Requires approval
    INVITE_ONLY   // Can only join with invite code
}

/**
 * Member role enum
 */
@Serializable
enum class MemberRole {
    ADMIN,        // Full control - can manage members, settings, delete clan
    MODERATOR,    // Can approve members, manage invites
    MEMBER        // Regular member
}

/**
 * Clan status enum
 */
@Serializable
enum class InviteStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED
}

/**
 * Clan data model
 */
@Serializable
data class Clan(
    val name: String,
    val description: String? = null,
    val tagline: String? = null,
    val logoUrl: String? = null,
    val clanType: String,
    val maxMembers: Int = 50,
    val currentMembers: Int = 0,
    val totalFocusHours: Long = 0L, // in milliseconds
    val creatorId: String,
    val isActive: Boolean = true,
    val country: String? = null,
    val city: String? = null,
    val category: String? = null,
    val createdAt: String, // ISO 8601
    val updatedAt: String, // ISO 8601
    // Additional computed fields
    val rank: Int? = null,
    val userRole: String? = null, // Role of requesting user if they're a member
    val isMember: Boolean = false
)

/**
 * Request to create a clan
 */
@Serializable
data class CreateClanRequest(
    val name: String,
    val description: String? = null,
    val tagline: String? = null,
    val logoUrl: String? = null,
    val clanType: String = "PUBLIC", // PUBLIC, PRIVATE, INVITE_ONLY
    val maxMembers: Int = 50,
    val country: String? = null,
    val city: String? = null,
    val category: String? = null
)

/**
 * Request to update clan details
 */
@Serializable
data class UpdateClanRequest(
    val description: String? = null,
    val tagline: String? = null,
    val logoUrl: String? = null,
    val clanType: String? = null,
    val maxMembers: Int? = null,
    val country: String? = null,
    val city: String? = null,
    val category: String? = null
)

/**
 * Clan member data model
 */
@Serializable
data class ClanMember(
    val clanId: Long,
    val userId: String,
    val username: String? = null,
    val role: String,
    val contributedFocusHours: Long = 0L, // in milliseconds
    val joinedAt: String, // ISO 8601
    val lastActiveAt: String, // ISO 8601
    val isActive: Boolean = true
)

/**
 * Clan statistics data model
 */
@Serializable
data class ClanStat(
    val id: Long,
    val clanId: Long,
    val period: String,
    val periodDate: String,
    val totalFocusHours: Long = 0L, // in milliseconds
    val activeMembersCount: Int = 0,
    val rank: Int? = null,
    val createdAt: String, // ISO 8601
    val updatedAt: String // ISO 8601
)

/**
 * Clan invite data model
 */
@Serializable
data class ClanInvite(
    val clanId: Long,
    val clanName: String? = null,
    val inviterId: String,
    val inviterUsername: String? = null,
    val inviteeUserId: String? = null,
    val inviteCode: String,
    val status: String,
    val maxUses: Int = 1,
    val currentUses: Int = 0,
    val expiresAt: String? = null,
    val acceptedAt: String? = null,
    val createdAt: String // ISO 8601
)

/**
 * Request to create an invite
 */
@Serializable
data class CreateInviteRequest(
    val clanId: Long,
    val inviteeUserId: String? = null, // Optional - if null, creates a general invite code
    val maxUses: Int = 1, // 1 for single use, -1 for unlimited
    val expiresInDays: Int? = null // null = never expires
)

/**
 * Request to join a clan
 */
@Serializable
data class JoinClanRequest(
    val clanId: Long? = null, // Either clanId or inviteCode must be provided
    val inviteCode: String? = null,
    val message: String? = null // Optional message for PRIVATE clans
)

/**
 * Request to accept an invite
 */
@Serializable
data class AcceptInviteRequest(
    val inviteCode: String
)

/**
 * Clan badge data model
 */
@Serializable
data class ClanBadge(
    val clanId: Long,
    val badgeType: String,
    val title: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val metadata: String? = null,
    val earnedAt: String // ISO 8601
)

/**
 * Clan leaderboard entry
 */
@Serializable
data class ClanLeaderboardEntry(
    val rank: Int,
    val clanId: Long,
    val clanName: String,
    val clanLogoUrl: String? = null,
    val totalFocusHours: Long, // in milliseconds
    val activeMembersCount: Int,
    val currentMembers: Int,
    val category: String? = null,
    val city: String? = null,
    val country: String? = null
)

/**
 * Clan leaderboard response
 */
@Serializable
data class ClanLeaderboardResponse(
    val period: String, // daily, weekly, monthly
    val periodDate: String,
    val entries: List<ClanLeaderboardEntry>,
    val userClanRank: Int? = null, // Rank of user's clan if they're in one
    val totalClans: Int
)

/**
 * Clan details response (includes members and stats)
 */
@Serializable
data class ClanDetailsResponse(
    val clan: Clan,
    val members: List<ClanMember>,
    val stats: ClanStatsResponse,
    val badges: List<ClanBadge>
)

/**
 * Clan stats response
 */
@Serializable
data class ClanStatsResponse(
    val totalFocusHours: Long, // lifetime total in milliseconds
    val dailyFocusHours: Long = 0L,
    val weeklyFocusHours: Long = 0L,
    val monthlyFocusHours: Long = 0L,
    val topContributors: List<TopContributor> = emptyList()
)

/**
 * Top contributor data
 */
@Serializable
data class TopContributor(
    val userId: String,
    val username: String? = null,
    val contributedFocusHours: Long, // in milliseconds
    val rank: Int
)

/**
 * Request to update member role
 */
@Serializable
data class UpdateMemberRoleRequest(
    val userId: String,
    val role: String // ADMIN, MODERATOR, MEMBER
)

/**
 * Request to remove member
 */
@Serializable
data class RemoveMemberRequest(
    val userId: String,
    val reason: String? = null
)

/**
 * Clan search/list response
 */
@Serializable
data class ClanListResponse(
    val clans: List<Clan>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int
)

/**
 * Clan join request data model
 */
@Serializable
data class ClanJoinRequest(
    val clanId: Long,
    val userId: String,
    val username: String? = null,
    val message: String? = null,
    val status: String,
    val reviewedBy: String? = null,
    val reviewedAt: String? = null,
    val createdAt: String // ISO 8601
)

/**
 * Request to review join request
 */
@Serializable
data class ReviewJoinRequestRequest(
    val approved: Boolean,
    val reason: String? = null
)

/**
 * User's clan info response
 */
@Serializable
data class UserClanInfoResponse(
    val clan: Clan?,
    val memberInfo: ClanMember?,
    val contributionStats: ContributionStats?
)

/**
 * Contribution stats for a member
 */
@Serializable
data class ContributionStats(
    val totalContributed: Long, // lifetime in milliseconds
    val dailyContribution: Long = 0L,
    val weeklyContribution: Long = 0L,
    val monthlyContribution: Long = 0L,
    val rankInClan: Int? = null
)

/**
 * Clan activity log entry (for future use)
 */
@Serializable
data class ClanActivity(
    val activityType: String, // MEMBER_JOINED, MEMBER_LEFT, MILESTONE_REACHED, RANK_CHANGED
    val description: String,
    val userId: String? = null,
    val metadata: String? = null,
    val timestamp: String // ISO 8601
)

