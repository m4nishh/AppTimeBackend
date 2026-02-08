# Notification Deeplinks Summary

This document summarizes all the deeplinks used in the notification service, updated to match the AppTime deeplink standard.

## Updated Deeplink Format

All deeplinks now follow the standard format:
- **Parametrized routes**: `apptime://screen/route/{parameter}`
- **Simple routes**: Just the route name (e.g., `landing`, `rewards`, `profile`)

---

## 📱 Notification Types and Their Deeplinks

### Challenge Notifications

| Method | Notification Type | Deeplink Format | Example |
|--------|------------------|-----------------|---------|
| `sendChallengeJoinNotification` | `challenge_join` | `apptime://screen/challenge_detail/{challengeId}` | `apptime://screen/challenge_detail/123` |
| `sendChallengeCompletionNotification` | `challenge_complete` | `apptime://screen/challenge_detail/{challengeId}` | `apptime://screen/challenge_detail/123` |
| `sendChallengeRewardNotification` | `challenge_reward` | `apptime://screen/challenge_detail/{challengeId}` | `apptime://screen/challenge_detail/123` |
| `sendChallengeWinnerNotificationToOthers` | `challenge_winner` | `apptime://screen/challenge_detail/{challengeId}` | `apptime://screen/challenge_detail/123` |

### Reward & Coin Notifications

| Method | Notification Type | Deeplink Format | Example |
|--------|------------------|-----------------|---------|
| `sendRewardNotification` | `reward` | `rewards` (simple) OR `apptime://screen/reward_transaction/{rewardId}` | `rewards` or `apptime://screen/reward_transaction/456` |
| `sendCoinsAddedNotification` | `coins_added` | `coin_history` | `coin_history` |
| `sendCoinsSpentNotification` | `coins_spent` | `coin_history` | `coin_history` |
| `sendRewardCatalogClaimedNotification` | `reward_claimed` | `apptime://screen/reward_transaction/{transactionNumber}` | `apptime://screen/reward_transaction/TXN-123456` |
| `sendTransactionStatusNotification` | `transaction_status` | `apptime://screen/reward_transaction/{transactionNumber}` | `apptime://screen/reward_transaction/TXN-123456` |
| `sendLowBalanceNotification` | `low_balance` | `challenges` | `challenges` |
| `sendRewardBackInStockNotification` | `reward_back_in_stock` | `rewards` | `rewards` |

### Usage & Focus Notifications

| Method | Notification Type | Deeplink Format | Example |
|--------|------------------|-----------------|---------|
| `sendDailyLimitNotification` | `daily_limit` | `apptime://screen/app_usage_detail/{packageName}` OR `statistics` | `apptime://screen/app_usage_detail/com.instagram.android` or `statistics` |
| `sendAppUsageHighAlertNotification` | `app_usage_alert` | `apptime://screen/app_usage_detail/{packageName}` | `apptime://screen/app_usage_detail/com.whatsapp` |
| `sendFocusMilestoneNotification` | `focus_milestone` | `focus_mode` | `focus_mode` |
| `sendBreakReminderNotification` | `break_reminder` | `landing` | `landing` |

### Social & Engagement Notifications

| Method | Notification Type | Deeplink Format | Example |
|--------|------------------|-----------------|---------|
| `sendLeaderboardRankNotification` | `leaderboard_update` | `leaderboard` | `leaderboard` |
| `sendFriendActivityNotification` | `friend_activity` | `apptime://screen/record_detail/{username}` | `apptime://screen/record_detail/john_doe` |
| `sendProfileViewNotification` | `profile_view` | `profile` | `profile` |

### Content & Feature Notifications

| Method | Notification Type | Deeplink Format | Example |
|--------|------------------|-----------------|---------|
| `sendNewWallpaperNotification` | `new_wallpaper` | `wallpaper` | `wallpaper` |
| `sendWelcomeBonusNotification` | `welcome_bonus` | `landing` | `landing` |
| `sendStreakMilestoneNotification` | `streak_milestone` | `profile` | `profile` |

### System Notifications

| Method | Notification Type | Deeplink Format | Example |
|--------|------------------|-----------------|---------|
| `sendMaintenanceNotification` | `maintenance` | `landing` | `landing` |

---

## 🔍 Deeplink Categories

### Simple Routes (No Parameters)
These deeplinks use just the route name:
- `landing` → Landing/Home Screen
- `profile` → Profile Screen
- `statistics` → Statistics Screen
- `leaderboard` → Leaderboard Screen
- `challenges` → Challenges List Screen
- `rewards` → Rewards Screen
- `coin_history` → Coin History Screen
- `wallpaper` → Wallpaper Screen
- `focus_mode` → Focus Mode Screen

### Parametrized Routes (With IDs)
These deeplinks include dynamic parameters:
- `apptime://screen/challenge_detail/{challengeId}` → Specific Challenge
- `apptime://screen/reward_transaction/{transactionNumber}` → Specific Transaction
- `apptime://screen/app_usage_detail/{packageName}` → Specific App Usage
- `apptime://screen/record_detail/{username}` → User Profile/Record

---

## 📋 Usage Examples

### Example 1: Challenge Completion
```kotlin
notificationService.sendChallengeCompletionNotification(
    userId = "user123",
    challengeTitle = "7-Day Focus Challenge",
    rank = 1,
    challengeId = 456L
)
// Deeplink: apptime://screen/challenge_detail/456
```

### Example 2: High App Usage Alert
```kotlin
notificationService.sendAppUsageHighAlertNotification(
    userId = "user123",
    appName = "Instagram",
    packageName = "com.instagram.android",
    usageMinutes = 180,
    thresholdMinutes = 120
)
// Deeplink: apptime://screen/app_usage_detail/com.instagram.android
```

### Example 3: Reward Transaction Status
```kotlin
notificationService.sendTransactionStatusNotification(
    userId = "user123",
    transactionNumber = "TXN-789012",
    rewardTitle = "Premium Wallpaper Pack",
    status = "SHIPPED",
    trackingNumber = "TRACK123456"
)
// Deeplink: apptime://screen/reward_transaction/TXN-789012
```

### Example 4: Coins Added
```kotlin
notificationService.sendCoinsAddedNotification(
    userId = "user123",
    amount = 100L,
    source = "CHALLENGE_WIN",
    description = "7-Day Focus Challenge"
)
// Deeplink: coin_history
```

### Example 5: Friend Activity
```kotlin
notificationService.sendFriendActivityNotification(
    userId = "user123",
    friendUsername = "alice_jones",
    activityType = "CHALLENGE_WIN",
    activityDetails = "won the Weekend Challenge!"
)
// Deeplink: apptime://screen/record_detail/alice_jones
```

---

## 🔔 Firebase Cloud Messaging Format

When these notifications are sent, they follow this FCM payload format:

```json
{
  "notification": {
    "title": "Challenge Completed! 🏆",
    "body": "You finished in rank #1! Challenge: 7-Day Focus Challenge"
  },
  "data": {
    "deeplink": "apptime://screen/challenge_detail/456",
    "type": "challenge_complete",
    "notificationId": "123"
  }
}
```

---

## ✅ Key Changes Made

1. **Challenge deeplinks**: Changed from `app://challenge/{id}` to `apptime://screen/challenge_detail/{id}`
2. **Reward deeplinks**: Changed from `app://reward/{id}` to `apptime://screen/reward_transaction/{id}`
3. **Transaction deeplinks**: Changed from `app://rewards/transactions/{id}` to `apptime://screen/reward_transaction/{id}`
4. **App usage deeplinks**: Added support for `apptime://screen/app_usage_detail/{packageName}`
5. **Simple routes**: Changed from `app://focus`, `app://usage`, `app://home` to simple route names like `focus_mode`, `statistics`, `landing`
6. **Coin history**: Changed from `app://rewards/coins` to `coin_history`
7. **Challenges list**: Changed from `app://challenges` to `challenges`

---

## 🚀 New Notification Methods Added

The following new notification methods were added to enhance the app experience:

1. **`sendAppUsageHighAlertNotification`** - Warns users about high app usage
2. **`sendLeaderboardRankNotification`** - Notifies about leaderboard rank changes
3. **`sendNewWallpaperNotification`** - Announces new wallpaper collections
4. **`sendWelcomeBonusNotification`** - Welcomes new users with bonus coins
5. **`sendStreakMilestoneNotification`** - Celebrates login streak milestones
6. **`sendFriendActivityNotification`** - Shares friend achievements
7. **`sendProfileViewNotification`** - Notifies when profile is viewed
8. **`sendMaintenanceNotification`** - Announces scheduled maintenance

---

## 📱 How the App Handles These Deeplinks

According to your deeplink documentation, the app's `DeeplinkParser` will:

1. Parse the deeplink URL
2. Extract parameters from path or query string
3. Navigate to the appropriate screen
4. Clear back stack and add Landing screen (except for Landing/Permission)
5. Ensure back button returns to Landing screen

Example flow:
```
User taps notification
  → FCM extracts deeplink from data payload
  → DeeplinkParser parses: apptime://screen/challenge_detail/123
  → Navigation: Clear back stack → Add Landing → Add ChallengeDetail(123)
  → User sees challenge detail, back button returns to Landing
```

---

## 🔧 Testing Notifications with Deeplinks

Use the notification API to test:

```bash
# Test challenge completion notification
curl -X POST "http://localhost:8080/api/admin/notifications/send" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "title": "Challenge Completed! 🏆",
    "text": "You finished in rank #1! Challenge: 7-Day Focus Challenge",
    "type": "challenge_complete",
    "deeplink": "apptime://screen/challenge_detail/456"
  }'
```

---

## 📝 Notes for Developers

1. **Always use the standard format**: Stick to either `apptime://screen/route/{param}` or simple route names
2. **Package names**: For app usage deeplinks, ensure the package name is valid and exists on the device
3. **Transaction numbers**: Use the actual transaction number from the database
4. **Challenge IDs**: Must be valid, existing challenge IDs
5. **Usernames**: For record detail deeplinks, use the actual username (not userId)
6. **Image URLs**: Optional `image` parameter can include full URLs to notification images
7. **Type field**: Always set the `type` field for proper categorization and analytics

---

*Last Updated: January 28, 2026*

