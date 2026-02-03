# Referral Service Documentation

## Overview

The Referral Service is a comprehensive system that allows users to invite friends to the app and earn rewards. Both the referrer (person who invites) and the referred user (person who joins) receive coin rewards.

## Features

- **Unique Referral Codes**: Each user gets a unique, easy-to-share referral code
- **Automatic Rewards**: Both referrer and referred user receive coins when referral is completed
- **Referral Tracking**: Track all referrals with status (PENDING, COMPLETED, REWARDED)
- **Referral Leaderboard**: See top referrers and compete for the most referrals
- **Notifications**: Push notifications for referral success and welcome bonuses
- **Admin Dashboard**: Admin can view all referrals and manually complete them if needed

## Database Schema

### Tables

#### 1. `user_referral_codes`
Stores unique referral codes for each user.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| user_id | VARCHAR(255) | User ID (unique) |
| referral_code | VARCHAR(50) | Unique referral code |
| total_referrals | INTEGER | Total successful referrals |
| total_coins_earned | BIGINT | Total coins earned from referrals |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

#### 2. `referrals`
Tracks who referred whom.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| referrer_id | VARCHAR(255) | User who sent the referral |
| referred_user_id | VARCHAR(255) | User who was referred (unique) |
| referral_code | VARCHAR(50) | Code used for the referral |
| status | VARCHAR(50) | PENDING, COMPLETED, REWARDED |
| referrer_reward | BIGINT | Coins awarded to referrer |
| referred_reward | BIGINT | Coins awarded to referred user |
| completed_at | TIMESTAMP | When referral was completed |
| rewarded_at | TIMESTAMP | When rewards were given |
| created_at | TIMESTAMP | Creation timestamp |

## API Endpoints

### User Endpoints

#### 1. Get My Referral Code
```
GET /api/referrals/my-code
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "userId": "user123",
    "referralCode": "ABC123XYZ",
    "totalReferrals": 5,
    "totalCoinsEarned": 2500,
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-20T14:45:00Z"
  },
  "message": "Your referral code: ABC123XYZ"
}
```

#### 2. Apply Referral Code
```
POST /api/referrals/apply
Authorization: Bearer <token>
Content-Type: application/json

{
  "referralCode": "ABC123XYZ"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "success": true,
    "message": "Referral code applied successfully! You'll receive 200 coins.",
    "referrerId": "user456",
    "bonusCoins": 200
  }
}
```

**Error Cases:**
- `400 Bad Request`: Invalid referral code, already used a code, or trying to use own code
- `500 Internal Server Error`: Server error

#### 3. Complete Referral
```
POST /api/referrals/complete
Authorization: Bearer <token>
Content-Type: application/json

{
  "referredUserId": "user789"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "success": true,
    "message": "Referral completed successfully!",
    "referrerReward": 500,
    "referredReward": 200
  }
}
```

**Note:** This endpoint is typically called automatically by the system when the referred user completes a required action (e.g., finishes onboarding, completes first challenge).

#### 4. Get My Referral Info
```
GET /api/referrals/my-info
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "userId": "user123",
    "referralCode": "ABC123XYZ",
    "totalReferrals": 5,
    "totalCoinsEarned": 2500,
    "pendingReferrals": 2,
    "completedReferrals": 3,
    "referrals": [
      {
        "referredUserId": "user456",
        "referredUsername": "john_doe",
        "status": "REWARDED",
        "coinsEarned": 500,
        "createdAt": "2024-01-15T10:30:00Z",
        "completedAt": "2024-01-15T11:00:00Z",
        "rewardedAt": "2024-01-15T11:00:00Z"
      }
    ]
  }
}
```

#### 5. Get Referral Leaderboard
```
GET /api/referrals/leaderboard?limit=20
Authorization: Bearer <token>
```

**Query Parameters:**
- `limit` (optional): Number of top referrers to return (default: 20, max: 20)

**Response:**
```json
{
  "success": true,
  "data": {
    "leaderboard": [
      {
        "userId": "user123",
        "username": "top_referrer",
        "totalReferrals": 50,
        "totalCoinsEarned": 25000,
        "rank": 1
      }
    ],
    "myRank": 5,
    "myStats": {
      "totalReferrals": 10,
      "pendingReferrals": 2,
      "completedReferrals": 8,
      "totalCoinsEarned": 5000,
      "referralCode": "ABC123XYZ"
    }
  }
}
```

### Admin Endpoints

#### 1. Get All Referrals
```
GET /api/referrals/admin/all?status=PENDING&limit=50&offset=0
Authorization: Bearer <token>
```

**Query Parameters:**
- `status` (optional): Filter by status (PENDING, COMPLETED, REWARDED)
- `limit` (optional): Limit results
- `offset` (optional): Offset for pagination

**Response:**
```json
{
  "success": true,
  "data": {
    "referrals": [
      {
        "id": 1,
        "referrerId": "user123",
        "referrerUsername": "john_doe",
        "referredUserId": "user456",
        "referredUsername": "jane_smith",
        "referralCode": "ABC123XYZ",
        "status": "REWARDED",
        "referrerReward": 500,
        "referredReward": 200,
        "completedAt": "2024-01-15T11:00:00Z",
        "rewardedAt": "2024-01-15T11:00:00Z",
        "createdAt": "2024-01-15T10:30:00Z"
      }
    ],
    "total": 100,
    "pending": 20,
    "completed": 30,
    "rewarded": 50
  }
}
```

#### 2. Manually Complete Referral
```
POST /api/referrals/admin/complete/{referralId}
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "success": true,
    "message": "Referral completed successfully!",
    "referrerReward": 500,
    "referredReward": 200
  }
}
```

## Reward Configuration

The reward amounts are configured in `ReferralService.kt`:

```kotlin
companion object {
    const val REFERRER_REWARD_COINS = 500L  // Coins given to referrer
    const val REFERRED_REWARD_COINS = 200L  // Coins given to new user
}
```

To change reward amounts, modify these constants and restart the server.

## Referral Code Format

Referral codes are automatically generated with the following format:
- **Prefix**: First 3 characters of user ID (uppercase)
- **Random Part**: 6 random characters from `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`
- **Example**: `ABC123XYZ`

The code excludes confusing characters (I, O, 0, 1) to prevent user errors.

## Workflow

### 1. User Gets Referral Code
```
User A opens app → Navigates to referrals → Gets code "ABC123XYZ"
```

### 2. User Shares Code
```
User A shares "ABC123XYZ" with User B
```

### 3. New User Applies Code
```
User B signs up → Enters "ABC123XYZ" during onboarding → Code is applied
Status: PENDING
```

### 4. System Completes Referral
```
User B completes required action (e.g., first challenge) →
System calls completeReferral() →
- User A receives 500 coins (referrer reward)
- User B receives 200 coins (welcome bonus)
- Status: COMPLETED → REWARDED
- Both users receive push notifications
```

## Notifications

### 1. Referral Success (to Referrer)
```
Title: "Referral Success! 🎉"
Text: "john_doe joined using your referral code! You earned 500 coins."
Deeplink: "referrals"
```

### 2. Welcome Bonus (to Referred User)
```
Title: "Welcome Bonus! 🎁"
Text: "Welcome to AppTime! You've received 200 coins as a welcome bonus."
Deeplink: "rewards"
```

## Integration Points

### 1. During User Signup/Onboarding
```kotlin
// After user registers
val response = referralService.applyReferralCode(
    newUserId = userId,
    referralCode = userEnteredCode
)
```

### 2. When User Completes Required Action
```kotlin
// After user completes first challenge or onboarding
val response = referralService.completeReferral(
    referredUserId = userId
)
```

### 3. In User Profile/Settings
```kotlin
// Show user's referral code and stats
val info = referralService.getMyReferralInfo(userId)
```

## Testing

### Test Scenario 1: Apply Referral Code
```bash
# 1. Get User A's referral code
curl -X GET "http://localhost:8080/api/referrals/my-code" \
  -H "Authorization: Bearer <userA_token>"

# 2. User B applies the code
curl -X POST "http://localhost:8080/api/referrals/apply" \
  -H "Authorization: Bearer <userB_token>" \
  -H "Content-Type: application/json" \
  -d '{"referralCode": "ABC123XYZ"}'

# 3. Complete the referral
curl -X POST "http://localhost:8080/api/referrals/complete" \
  -H "Authorization: Bearer <system_token>" \
  -H "Content-Type: application/json" \
  -d '{"referredUserId": "userB_id"}'

# 4. Check User A's referral info
curl -X GET "http://localhost:8080/api/referrals/my-info" \
  -H "Authorization: Bearer <userA_token>"
```

### Test Scenario 2: View Leaderboard
```bash
curl -X GET "http://localhost:8080/api/referrals/leaderboard?limit=10" \
  -H "Authorization: Bearer <token>"
```

## Error Handling

### Common Errors

1. **User already referred**
```json
{
  "success": false,
  "error": "You have already used a referral code. Each user can only be referred once."
}
```

2. **Invalid referral code**
```json
{
  "success": false,
  "error": "Invalid referral code. Please check the code and try again."
}
```

3. **Self-referral attempt**
```json
{
  "success": false,
  "error": "You cannot use your own referral code."
}
```

4. **Referral already completed**
```json
{
  "success": false,
  "error": "Referral has already been completed or rewarded."
}
```

## Best Practices

1. **Call `completeReferral()` at the right time**: Choose a meaningful action (e.g., completing onboarding, first challenge) to prevent abuse.

2. **Display referral code prominently**: Make it easy for users to find and share their code.

3. **Show referral stats**: Display total referrals, coins earned, and leaderboard rank to encourage sharing.

4. **Send notifications**: Keep users engaged with timely notifications about referral success.

5. **Monitor for abuse**: Check for suspicious patterns (e.g., same device, rapid signups).

## Future Enhancements

1. **Tiered Rewards**: Increase rewards for users who refer more people
2. **Time-Limited Bonuses**: Special bonus periods with higher rewards
3. **Referral Milestones**: Badges/achievements for reaching referral goals
4. **Social Sharing**: Direct integration with WhatsApp, SMS, etc.
5. **Referral Analytics**: Track conversion rates, most effective channels
6. **Custom Codes**: Allow users to create custom referral codes
7. **Referral Expiry**: Set expiration dates for referral codes
8. **Multi-Level Referrals**: Reward referrers when their referrals refer others

## Troubleshooting

### Issue: Referral code not working
- Check if code exists in database
- Verify user hasn't already used a code
- Ensure code is entered correctly (case-insensitive)

### Issue: Rewards not awarded
- Check referral status in database
- Verify `completeReferral()` was called
- Check notification queue for errors
- Verify coin transactions in `coins` table

### Issue: Duplicate referrals
- Check `referred_user_id` uniqueness constraint
- Verify application logic prevents multiple applications

## Support

For issues or questions:
1. Check server logs for error messages
2. Query database directly to verify data
3. Use admin endpoints to manually complete referrals if needed
4. Contact development team for assistance

---

**Last Updated**: January 2026
**Version**: 1.0

