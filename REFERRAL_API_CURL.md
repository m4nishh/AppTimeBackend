# Referral API - cURL Examples

Quick reference for testing the Referral API endpoints.

## Setup

```bash
# Set your base URL
BASE_URL="http://localhost:8080"

# Set your auth tokens
USER_A_TOKEN="your_user_a_token_here"
USER_B_TOKEN="your_user_b_token_here"
ADMIN_TOKEN="your_admin_token_here"
```

## User Endpoints

### 1. Get My Referral Code

Get or create your unique referral code.

```bash
curl -X GET "$BASE_URL/api/referrals/my-code" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "userId": "user123",
    "referralCode": "ABC123XYZ",
    "totalReferrals": 0,
    "totalCoinsEarned": 0,
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  },
  "message": "Your referral code: ABC123XYZ"
}
```

### 2. Apply Referral Code

Apply a referral code during signup/onboarding.

```bash
curl -X POST "$BASE_URL/api/referrals/apply" \
  -H "Authorization: Bearer $USER_B_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "referralCode": "ABC123XYZ"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "success": true,
    "message": "Referral code applied successfully! You'll receive 200 coins.",
    "referrerId": "user123",
    "bonusCoins": 200
  }
}
```

### 3. Complete Referral

Complete a referral and award coins to both users.

```bash
curl -X POST "$BASE_URL/api/referrals/complete" \
  -H "Authorization: Bearer $USER_B_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "referredUserId": "user456"
  }'
```

**Expected Response:**
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

### 4. Get My Referral Info

Get detailed information about your referrals.

```bash
curl -X GET "$BASE_URL/api/referrals/my-info" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response:**
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

### 5. Get Referral Leaderboard

View top referrers and your rank.

```bash
# Get top 20 referrers (default)
curl -X GET "$BASE_URL/api/referrals/leaderboard" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json"

# Get top 10 referrers (custom limit, max 20)
curl -X GET "$BASE_URL/api/referrals/leaderboard?limit=10" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response:**
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
      },
      {
        "userId": "user456",
        "username": "second_place",
        "totalReferrals": 40,
        "totalCoinsEarned": 20000,
        "rank": 2
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

## Admin Endpoints

### 1. Get All Referrals

View all referrals in the system (admin only).

```bash
# Get all referrals
curl -X GET "$BASE_URL/api/referrals/admin/all" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json"

# Filter by status
curl -X GET "$BASE_URL/api/referrals/admin/all?status=PENDING" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json"

# With pagination
curl -X GET "$BASE_URL/api/referrals/admin/all?limit=50&offset=0" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json"

# Filter by status with pagination
curl -X GET "$BASE_URL/api/referrals/admin/all?status=COMPLETED&limit=20&offset=0" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response:**
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

### 2. Manually Complete Referral

Manually complete a referral (admin only).

```bash
# Complete referral with ID 1
curl -X POST "$BASE_URL/api/referrals/admin/complete/1" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json"

# Complete referral with ID 42
curl -X POST "$BASE_URL/api/referrals/admin/complete/42" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response:**
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

## Complete Workflow Example

### Scenario: User A refers User B

```bash
# Step 1: User A gets their referral code
echo "=== Step 1: User A gets referral code ==="
curl -X GET "$BASE_URL/api/referrals/my-code" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json"

# Note the referral code from response (e.g., "ABC123XYZ")

# Step 2: User B signs up and applies the code
echo "=== Step 2: User B applies referral code ==="
curl -X POST "$BASE_URL/api/referrals/apply" \
  -H "Authorization: Bearer $USER_B_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "referralCode": "ABC123XYZ"
  }'

# Step 3: User B completes required action (e.g., first challenge)
# System automatically calls complete, or you can call it manually:
echo "=== Step 3: Complete referral ==="
curl -X POST "$BASE_URL/api/referrals/complete" \
  -H "Authorization: Bearer $USER_B_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "referredUserId": "user_b_id"
  }'

# Step 4: User A checks their referral stats
echo "=== Step 4: User A checks referral info ==="
curl -X GET "$BASE_URL/api/referrals/my-info" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json"

# Step 5: Check coins were awarded
echo "=== Step 5: User A checks coins ==="
curl -X GET "$BASE_URL/api/rewards/coins" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json"

echo "=== Step 6: User B checks coins ==="
curl -X GET "$BASE_URL/api/rewards/coins" \
  -H "Authorization: Bearer $USER_B_TOKEN" \
  -H "Content-Type: application/json"
```

## Error Examples

### Error: Invalid Referral Code

```bash
curl -X POST "$BASE_URL/api/referrals/apply" \
  -H "Authorization: Bearer $USER_B_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "referralCode": "INVALID123"
  }'
```

**Response:**
```json
{
  "success": false,
  "error": "Invalid referral code. Please check the code and try again."
}
```

### Error: Already Used Referral Code

```bash
curl -X POST "$BASE_URL/api/referrals/apply" \
  -H "Authorization: Bearer $USER_B_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "referralCode": "ABC123XYZ"
  }'
```

**Response:**
```json
{
  "success": false,
  "error": "You have already used a referral code. Each user can only be referred once."
}
```

### Error: Self-Referral

```bash
curl -X POST "$BASE_URL/api/referrals/apply" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "referralCode": "ABC123XYZ"
  }'
```

**Response:**
```json
{
  "success": false,
  "error": "You cannot use your own referral code."
}
```

## Testing Tips

1. **Create test users**: Create multiple test accounts to simulate referrals
2. **Check database**: Query the `referrals` and `user_referral_codes` tables directly
3. **Monitor logs**: Watch server logs for notification queue processing
4. **Verify coins**: Check the `coins` table to ensure rewards were added
5. **Test edge cases**: Try invalid codes, duplicate applications, self-referrals

## Database Queries (PostgreSQL)

```sql
-- Check user's referral code
SELECT * FROM user_referral_codes WHERE user_id = 'user123';

-- Check all referrals for a user
SELECT * FROM referrals WHERE referrer_id = 'user123';

-- Check if user was referred
SELECT * FROM referrals WHERE referred_user_id = 'user456';

-- Get referral statistics
SELECT 
  status,
  COUNT(*) as count
FROM referrals
GROUP BY status;

-- Get top referrers
SELECT 
  user_id,
  referral_code,
  total_referrals,
  total_coins_earned
FROM user_referral_codes
ORDER BY total_referrals DESC
LIMIT 10;

-- Check coins awarded for referrals
SELECT * FROM coins 
WHERE "coinSource" = 'REFERRAL' 
ORDER BY created_at DESC;
```

---

**Note**: Replace `$BASE_URL`, `$USER_A_TOKEN`, `$USER_B_TOKEN`, and `$ADMIN_TOKEN` with actual values.

