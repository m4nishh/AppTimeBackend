# Welcome Bonus Feature

## Overview

New users receive **10 coins** as a welcome bonus when they first register on the platform.

---

## Implementation Details

### Trigger
- **When:** User registers for the first time via `POST /api/users/register`
- **Who:** Only **new users** (not returning users)
- **Amount:** 10 coins

### How It Works

```
User Registration Flow:
1. User sends registration request with device info
2. System checks if device ID already exists
3. User account is created/updated
4. IF NEW USER:
   ├─ Add 10 coins to user account
   ├─ Source: ADMIN_GRANT
   ├─ Description: "Welcome bonus! Thanks for joining our platform."
   └─ Send welcome notification 💰
5. Return registration response
```

### Code Location

**UserService.kt** (`registerDevice` method):
- Checks if user exists by device ID
- Adds 10 coins for new users only
- Sends welcome notification
- Gracefully handles errors (bonus failure doesn't block registration)

**UserRepository.kt**:
- Added `userExistsByDeviceId()` method to detect new users

---

## Notification

### Welcome Notification Details
- **Title:** "Coins Earned! 💰"
- **Text:** "You earned 10 coins for admin grant: Welcome bonus! Thanks for joining our platform."
- **Type:** `coins_added`
- **Deeplink:** `app://rewards/coins`

### When Sent
- Automatically sent when new user registers
- Queued asynchronously (doesn't slow down registration)
- Stored in database for in-app viewing
- Sent as push notification (if Firebase token available)

---

## Database Records

### Coins Table Entry
```json
{
  "userId": "user_generated_id",
  "amount": 10,
  "source": "ADMIN_GRANT",
  "description": "Welcome bonus! Thanks for joining our platform.",
  "metadata": "{\"type\": \"welcome_bonus\"}",
  "expiresAt": null,
  "createdAt": "2026-01-27T..."
}
```

### Notification Table Entry
```json
{
  "userId": "user_generated_id",
  "title": "Coins Earned! 💰",
  "text": "You earned 10 coins for admin grant: Welcome bonus! Thanks for joining our platform.",
  "type": "coins_added",
  "deeplink": "app://rewards/coins",
  "isRead": false,
  "createdAt": "2026-01-27T..."
}
```

---

## Testing

### Test New User Registration

```bash
# Register a new user (use unique device ID)
curl -X POST "http://localhost:8080/api/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceInfo": {
      "deviceId": "test_device_001",
      "manufacturer": "Samsung",
      "model": "Galaxy S21",
      "brand": "Samsung",
      "product": "Galaxy",
      "device": "SM-G991B",
      "hardware": "exynos",
      "androidVersion": "12",
      "sdkVersion": "31"
    },
    "firebaseToken": "test_firebase_token"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Device registered successfully",
  "data": {
    "userId": "generated_user_id",
    "username": "user_xxxxxxxx",
    "createdAt": "2026-01-27T...",
    "totpSecret": "...",
    "totpEnabled": true
  }
}
```

**Server Logs:**
```
🎉 [Welcome Bonus] New user registered: generated_user_id
💰 [Welcome Bonus] Added 10 coins to user generated_user_id, coin ID: 1
✅ [Welcome Bonus] Enqueued welcome notification for user generated_user_id
```

### Verify Coins Were Added

```bash
# Get coin balance (requires authentication)
curl -X GET "http://localhost:8080/api/rewards/coins" \
  -H "Authorization: Bearer USER_TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "userId": "generated_user_id",
    "totalCoins": 10,
    "coinHistory": [
      {
        "id": 1,
        "userId": "generated_user_id",
        "amount": 10,
        "source": "ADMIN_GRANT",
        "description": "Welcome bonus! Thanks for joining our platform.",
        "metadata": "{\"type\": \"welcome_bonus\"}",
        "createdAt": "2026-01-27T..."
      }
    ]
  }
}
```

### Verify Notification Was Sent

```bash
# Get notifications (requires authentication)
curl -X GET "http://localhost:8080/api/notifications" \
  -H "Authorization: Bearer USER_TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "notifications": [
      {
        "id": 1,
        "title": "Coins Earned! 💰",
        "text": "You earned 10 coins for admin grant: Welcome bonus! Thanks for joining our platform.",
        "type": "coins_added",
        "deeplink": "app://rewards/coins",
        "isRead": false,
        "createdAt": "2026-01-27T..."
      }
    ],
    "totalCount": 1,
    "unreadCount": 1
  }
}
```

### Test Existing User (Should NOT Get Bonus)

```bash
# Register again with same device ID
curl -X POST "http://localhost:8080/api/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceInfo": {
      "deviceId": "test_device_001",
      ...
    }
  }'
```

**Server Logs:**
```
👤 [Registration] Existing user logged in: generated_user_id
```

**No bonus coins added (user already received welcome bonus)**

---

## Error Handling

### Graceful Degradation
If the welcome bonus fails (e.g., database error, notification error):
- ✅ **Registration still succeeds**
- ✅ User account is created
- ❌ Welcome bonus is not added
- 📝 Error is logged in server logs

**Example Error Log:**
```
❌ [Welcome Bonus] Failed to add welcome bonus for user generated_user_id: Database connection timeout
```

### Why This Approach?
- User registration is critical and should never fail due to bonus issues
- Bonus can be manually added later if needed
- Better user experience (account created immediately)

---

## Configuration

### Change Bonus Amount

To change the welcome bonus amount, edit `UserService.kt`:

```kotlin
// Current: 10 coins
val coinId = rewardRepository.addCoins(
    userId = response.userId,
    amount = 10L,  // <-- Change this value
    source = CoinSource.ADMIN_GRANT,
    ...
)
```

Also update the notification:
```kotlin
NotificationQueueService.enqueueCoinsAddedNotification(
    userId = response.userId,
    amount = 10L,  // <-- Change this value too
    ...
)
```

### Change Bonus Description

Edit the `description` parameter:
```kotlin
description = "Welcome bonus! Thanks for joining our platform."
// Change to your custom message
```

### Disable Welcome Bonus

To disable the welcome bonus temporarily:
```kotlin
// Comment out the entire try-catch block in UserService.registerDevice()
/*
try {
    // Welcome bonus code here
    ...
} catch (e: Exception) {
    ...
}
*/
```

---

## Admin Operations

### Manually Add Welcome Bonus

If a user didn't receive the welcome bonus due to an error:

```bash
curl -X POST "http://localhost:8080/api/rewards/coins/add" \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user_id",
    "amount": 10,
    "source": "ADMIN_GRANT",
    "description": "Welcome bonus (manual)"
  }'
```

### Query Users Who Received Bonus

```sql
-- PostgreSQL query
SELECT 
    c.user_id,
    u.username,
    c.amount,
    c.description,
    c.created_at
FROM coins c
JOIN users u ON c.user_id = u.user_id
WHERE c.source = 'ADMIN_GRANT'
  AND c.description LIKE '%Welcome bonus%'
ORDER BY c.created_at DESC;
```

### Count Total Welcome Bonuses Given

```sql
SELECT COUNT(*) as total_welcome_bonuses
FROM coins
WHERE source = 'ADMIN_GRANT'
  AND description LIKE '%Welcome bonus%';
```

---

## Business Metrics

### Track Welcome Bonus Impact

**Key Metrics to Monitor:**
1. **Welcome Bonus Redemption Rate**
   - How many users spend their welcome coins?
   - Average time to first purchase

2. **User Retention**
   - Do users with welcome bonus have better retention?
   - Correlation between welcome bonus and active users

3. **Conversion Rate**
   - Do welcome bonus users convert to paying users?
   - Lifetime value comparison

### Analytics Queries

```sql
-- Users who received welcome bonus
SELECT COUNT(DISTINCT user_id) as users_with_welcome_bonus
FROM coins
WHERE source = 'ADMIN_GRANT'
  AND description LIKE '%Welcome bonus%';

-- Users who spent their welcome bonus
SELECT COUNT(DISTINCT c1.user_id) as users_who_spent
FROM coins c1
WHERE c1.user_id IN (
    SELECT user_id FROM coins 
    WHERE source = 'ADMIN_GRANT' 
    AND description LIKE '%Welcome bonus%'
)
AND c1.amount < 0;  -- Negative amount = spent

-- Average time to first purchase after welcome bonus
SELECT AVG(EXTRACT(EPOCH FROM (c2.created_at - c1.created_at))/3600) as avg_hours_to_purchase
FROM coins c1
JOIN coins c2 ON c1.user_id = c2.user_id
WHERE c1.source = 'ADMIN_GRANT'
  AND c1.description LIKE '%Welcome bonus%'
  AND c2.source = 'REDEMPTION'
  AND c2.created_at > c1.created_at;
```

---

## Best Practices

### ✅ Do's
- ✅ Keep welcome bonus modest (10-50 coins)
- ✅ Send notification immediately
- ✅ Log all welcome bonus operations
- ✅ Monitor bonus redemption rates
- ✅ Use welcome bonus to encourage first action

### ❌ Don'ts
- ❌ Don't give bonus to existing users
- ❌ Don't make bonus too large (reduces value perception)
- ❌ Don't block registration if bonus fails
- ❌ Don't forget to notify users about the bonus
- ❌ Don't allow multiple bonuses per user

---

## Future Enhancements

### 1. Referral Bonus
When user refers a friend:
- Referrer gets 50 coins
- New user gets 10 coins (welcome bonus)
- Both get notifications

### 2. Time-Limited Welcome Offer
First purchase within 7 days:
- Get 2x coins
- Special welcome discount

### 3. Progressive Onboarding Rewards
- Day 1: 10 coins (welcome)
- Day 3: 20 coins (active user)
- Day 7: 50 coins (loyal user)
- Day 30: 100 coins (veteran)

### 4. Welcome Bundle
Instead of just coins:
- 10 coins
- 1 free reward item
- Exclusive "Newcomer" badge
- Access to welcome challenges

### 5. A/B Testing
Test different welcome bonus amounts:
- Group A: 5 coins
- Group B: 10 coins (current)
- Group C: 20 coins
- Measure engagement and retention

---

## FAQ

### Q: Do existing users get the welcome bonus?
**A:** No, only new users (first-time registration) receive the welcome bonus.

### Q: What happens if the bonus fails?
**A:** Registration still succeeds, bonus can be added manually by admin.

### Q: Can users get multiple welcome bonuses?
**A:** No, the system checks by device ID to prevent duplicate bonuses.

### Q: When does the welcome bonus expire?
**A:** Never. Welcome bonus coins don't expire by default.

### Q: Can I change the welcome bonus amount?
**A:** Yes, edit the amount in `UserService.kt` (search for "10L").

### Q: Does the bonus notification always work?
**A:** It's queued asynchronously. If delivery fails, it's logged but doesn't block registration.

### Q: Can I see who received welcome bonuses?
**A:** Yes, query the `coins` table where `source = 'ADMIN_GRANT'` and description contains "Welcome bonus".

---

## Summary

✅ **Implemented:** Welcome bonus of 10 coins for new users
✅ **Automatic:** No manual intervention needed
✅ **Notified:** Users receive notification about bonus
✅ **Safe:** Registration succeeds even if bonus fails
✅ **Trackable:** All bonuses logged and queryable
✅ **Tested:** Works with existing notification system

**Impact:**
- 🎯 Better user onboarding experience
- 🎯 Immediate value for new users
- 🎯 Encourages first interaction with reward system
- 🎯 Increases user engagement

---

**Feature Status:** ✅ **PRODUCTION READY**

**Implementation Date:** January 27, 2026
**Version:** 1.0

