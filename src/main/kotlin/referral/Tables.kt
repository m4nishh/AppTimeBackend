package com.apptime.code.referral

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * User Referral Codes table - stores unique referral codes for each user
 */
object UserReferralCodes : Table("user_referral_codes") {
    val id = long("id").autoIncrement()
    val userId = varchar("user_id", 255).uniqueIndex()
    val referralCode = varchar("referral_code", 50).uniqueIndex() // Unique code for the user
    val totalReferrals = integer("total_referrals").default(0) // Total successful referrals
    val totalCoinsEarned = long("total_coins_earned").default(0L) // Total coins earned from referrals
    val createdAt = timestamp("created_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
}

/**
 * Referrals table - tracks who referred whom
 */
object Referrals : Table("referrals") {
    val id = long("id").autoIncrement()
    val referrerId = varchar("referrer_id", 255).index() // User who sent the referral
    val referredUserId = varchar("referred_user_id", 255).uniqueIndex() // User who was referred (can only be referred once)
    val referralCode = varchar("referral_code", 50).index() // Code used for the referral
    val status = varchar("status", 50).default("PENDING").index() // PENDING, COMPLETED, REWARDED
    val referrerReward = long("referrer_reward").default(0L) // Coins awarded to referrer
    val referredReward = long("referred_reward").default(0L) // Coins awarded to referred user
    val completedAt = timestamp("completed_at").nullable() // When referral was completed (e.g., user completed signup/onboarding)
    val rewardedAt = timestamp("rewarded_at").nullable() // When rewards were given
    val createdAt = timestamp("created_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
}

