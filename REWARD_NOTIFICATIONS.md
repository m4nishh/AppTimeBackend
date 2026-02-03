# Reward & Coins Notification System

## Overview

This document describes the comprehensive notification system for reward catalog and coins-related activities in the AppTime backend.

## Features Implemented

### 1. **Coins Added Notification** 💰
Notifies users when coins are added to their account from various sources.

**Triggered When:**
- User earns coins from daily login
- User earns coins from achievements
- User earns coins from referrals
- User receives admin-granted coins
- User purchases coins
- Note: Challenge win coins have their own dedicated notification

**Notification Details:**
- Title: "Coins Earned! 💰"
- Text: "You earned [amount] coins for [source]: [description]"
- Type: `coins_added`
- Deeplink: `app://rewards/coins`

**Example Usage:**
```kotlin
NotificationQueueService.enqueueCoinsAddedNotification(
    userId = "user123",
    amount = 100,
    source = "DAILY_LOGIN",
    description = "Logged in for 7 consecutive days"
)
```

---

### 2. **Reward Catalog Claimed Notification** 🎁
Notifies users when they successfully claim a reward from the catalog.

**Triggered When:**
- User successfully claims a reward item from the catalog
- Order is placed and coins are deducted

**Notification Details:**
- Title: "Reward Claimed! 🎁"
- Text: "You've successfully claimed '[rewardTitle]' for [coins] coins! Order #[transactionNumber]. You have [remaining] coins remaining."
- Type: `reward_claimed`
- Deeplink: `app://rewards/transactions/[transactionNumber]`

**Automatic Integration:**
This notification is automatically sent when `RewardService.claimRewardCatalog()` is called.

---

### 3. **Transaction Status Update Notification** 📦
Notifies users about order status changes throughout the fulfillment process.

**Triggered When:**
- Order status changes to: PENDING, PROCESSING, SHIPPED, DELIVERED, or CANCELLED

**Status-Specific Notifications:**

#### PENDING
- Title: "Order Placed! 📦"
- Text: "Your order #[transactionNumber] for '[rewardTitle]' has been placed and is awaiting processing."

#### PROCESSING
- Title: "Order Processing 🔄"
- Text: "Your order #[transactionNumber] for '[rewardTitle]' is being prepared."

#### SHIPPED
- Title: "Order Shipped! 🚚"
- Text: "Your order #[transactionNumber] for '[rewardTitle]' has been shipped! Tracking number: [trackingNumber]"

#### DELIVERED
- Title: "Order Delivered! ✅"
- Text: "Your order #[transactionNumber] for '[rewardTitle]' has been delivered. Enjoy your reward!"

#### CANCELLED
- Title: "Order Cancelled ❌"
- Text: "Your order #[transactionNumber] for '[rewardTitle]' has been cancelled. Your coins have been refunded."

**Type:** `transaction_status`
**Deeplink:** `app://rewards/transactions/[transactionNumber]`

**Automatic Integration:**
This notification is automatically sent when `RewardService.updateTransactionStatus()` is called by an admin.

---

### 4. **Low Balance Warning Notification** ⚠️
Warns users when their coin balance falls below a threshold.

**Triggered When:**
- User's remaining coin balance ≤ 100 coins after a purchase

**Notification Details:**
- Title: "Low Coin Balance ⚠️"
- Text: "You only have [balance] coins remaining. Complete challenges to earn more coins!"
- Type: `low_balance`
- Deeplink: `app://challenges`

**Configuration:**
The threshold can be adjusted in `RewardService.claimRewardCatalog()`:
```kotlin
val LOW_BALANCE_THRESHOLD = 100L // Change this value as needed
```

**Automatic Integration:**
This notification is automatically sent after a successful reward claim if the remaining balance is low.

---

### 5. **Reward Back in Stock Notification** 🎉
Notifies users when a previously out-of-stock reward becomes available again.

**Triggered When:**
- Admin updates a catalog item that was out of stock to have stock again
- Manual call to notify interested users

**Notification Details:**
- Title: "Reward Back in Stock! 🎉"
- Text: "'[rewardTitle]' is back in stock for [coins] coins! Claim it before it's gone again."
- Type: `reward_back_in_stock`
- Deeplink: `app://rewards/catalog/[catalogId]`

**Manual Usage:**
```kotlin
rewardService.notifyRewardBackInStock(
    catalogId = 123,
    interestedUserIds = listOf("user1", "user2", "user3")
)
```

**Note:** In the current implementation, you need to maintain a list of "interested users" (e.g., wishlist feature) to use this notification effectively.

---

## Architecture

### Queue-Based System

All reward and coins notifications use an asynchronous queue-based system for reliable delivery:

1. **Enqueue**: Notifications are added to an in-memory queue
2. **Process**: Background workers process notifications asynchronously
3. **Store**: Notifications are saved to the database
4. **Push**: Push notifications are sent via Firebase (if token available)

### Components

#### 1. **QueueModels.kt**
Defines notification message types:
- `CoinsAddedNotificationMessage`
- `RewardCatalogClaimedNotificationMessage`
- `TransactionStatusNotificationMessage`
- `LowBalanceNotificationMessage`
- `RewardBackInStockNotificationMessage`

#### 2. **NotificationQueueService.kt**
Manages the notification queue:
- `enqueueCoinsAddedNotification()`
- `enqueueRewardCatalogClaimedNotification()`
- `enqueueTransactionStatusNotification()`
- `enqueueLowBalanceNotification()`
- `enqueueRewardBackInStockNotification()`

#### 3. **NotificationService.kt**
Handles notification creation and delivery:
- `sendCoinsAddedNotification()`
- `sendRewardCatalogClaimedNotification()`
- `sendTransactionStatusNotification()`
- `sendLowBalanceNotification()`
- `sendRewardBackInStockNotification()`

#### 4. **RewardService.kt**
Business logic with automatic notification integration:
- `addCoins()` - Sends coins added notification
- `claimRewardCatalog()` - Sends catalog claimed and low balance notifications
- `updateTransactionStatus()` - Sends transaction status notification
- `notifyRewardBackInStock()` - Helper method for back-in-stock notifications

---

## API Endpoints

### Get Notification Statistics

**Endpoint:** `GET /api/notifications/queue/stats`

**Response:**
```json
{
  "queueSize": 5,
  "totalEnqueued": 150,
  "totalProcessed": 145,
  "totalFailed": 0,
  "isStarted": true,
  "messagesByType": {
    "CoinsAddedNotificationMessage": 2,
    "RewardCatalogClaimedNotificationMessage": 1,
    "TransactionStatusNotificationMessage": 2
  }
}
```

---

## Testing

### Test Coins Added Notification

```bash
curl -X POST "http://localhost:8080/api/rewards/coins/add" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "your_user_id",
    "amount": 500,
    "source": "DAILY_LOGIN",
    "description": "7 day login streak bonus"
  }'
```

### Test Reward Catalog Claim

```bash
curl -X POST "http://localhost:8080/api/rewards/catalog/claim" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rewardCatalogId": 1,
    "recipientName": "John Doe",
    "recipientEmail": "john@example.com",
    "recipientPhone": "+1234567890"
  }'
```

### Test Transaction Status Update

First, get transaction ID from your transactions:
```bash
curl -X GET "http://localhost:8080/api/rewards/transactions" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

Then update status (admin only):
```bash
# Note: This would require admin authentication
curl -X PUT "http://localhost:8080/api/rewards/transactions/{transactionId}/status" \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "SHIPPED",
    "trackingNumber": "TRACK123456",
    "adminNotes": "Shipped via FedEx"
  }'
```

---

## Database Schema

All notifications are stored in the `notifications` table:

```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    text TEXT NOT NULL,
    type VARCHAR(50),
    image TEXT,
    deeplink TEXT,
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
);
```

---

## Best Practices

### 1. **Error Handling**
All notification operations are wrapped in try-catch blocks to prevent blocking the main business logic if notification delivery fails.

### 2. **Asynchronous Processing**
Notifications are enqueued and processed asynchronously, so they don't slow down API responses.

### 3. **Graceful Degradation**
If Firebase push notification fails (e.g., no token), the notification is still saved in the database for in-app viewing.

### 4. **Queue Monitoring**
Monitor queue statistics regularly:
```bash
curl -X GET "http://localhost:8080/api/notifications/queue/stats" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 5. **Low Balance Threshold**
Adjust the low balance threshold based on your reward pricing:
- Current threshold: 100 coins
- Location: `RewardService.claimRewardCatalog()`

---

## Future Enhancements

### 1. **Wishlist Feature**
Implement a wishlist table to track which users are interested in specific catalog items:
```sql
CREATE TABLE reward_wishlist (
    user_id VARCHAR(255),
    reward_catalog_id BIGINT,
    created_at TIMESTAMP,
    PRIMARY KEY (user_id, reward_catalog_id)
);
```

Then use this data to send back-in-stock notifications.

### 2. **Notification Preferences**
Allow users to configure which notification types they want to receive:
```sql
CREATE TABLE notification_preferences (
    user_id VARCHAR(255) PRIMARY KEY,
    coins_added BOOLEAN DEFAULT true,
    reward_claimed BOOLEAN DEFAULT true,
    transaction_status BOOLEAN DEFAULT true,
    low_balance BOOLEAN DEFAULT true,
    back_in_stock BOOLEAN DEFAULT true
);
```

### 3. **Rich Notifications**
Add images to notifications:
- Coins added: Coin icon
- Reward claimed: Product image
- Transaction status: Status-specific icons

### 4. **Scheduled Notifications**
Implement time-based notifications:
- Weekly coin balance summaries
- Expiring coins warnings
- Abandoned cart reminders

### 5. **Multi-language Support**
Integrate with the existing translation system to send notifications in the user's preferred language.

---

## Troubleshooting

### Notifications Not Sending

1. **Check if queue consumer is started:**
```bash
curl -X GET "http://localhost:8080/api/notifications/queue/stats" \
  -H "Authorization: Bearer YOUR_TOKEN"
```
Look for `"isStarted": true`

2. **Check queue size:**
If `queueSize` is growing, the consumer may be stuck. Check server logs.

3. **Check Firebase token:**
Ensure users have registered their Firebase tokens:
```bash
curl -X GET "http://localhost:8080/api/users/profile" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Notification Delays

- Current implementation uses in-memory queue with 5 concurrent workers
- For production at scale, consider migrating to Redis for distributed queue processing

### Database Notifications Not Appearing

Check that notifications are being created:
```bash
curl -X GET "http://localhost:8080/api/notifications" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## Summary

The reward and coins notification system provides comprehensive real-time updates to users about:
- ✅ Coins earned from various sources
- ✅ Successful reward claims
- ✅ Order status updates (pending → processing → shipped → delivered)
- ✅ Low balance warnings
- ✅ Rewards back in stock

All notifications are:
- 📱 Sent as push notifications (if Firebase token available)
- 💾 Stored in database for in-app viewing
- ⚡ Processed asynchronously via queue system
- 🔄 Automatically integrated into reward service operations

The system is production-ready, scalable, and provides excellent user engagement through timely notifications about reward and coin activities.

