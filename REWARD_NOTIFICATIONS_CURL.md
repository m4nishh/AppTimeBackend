# Reward Notifications - CURL Testing Guide

Quick reference for testing reward and coins notification features.

## Prerequisites

```bash
# Set your authorization token
export TOKEN="your_bearer_token_here"
export BASE_URL="http://localhost:8080"
```

---

## 1. Coins Added Notification

### Manual Add Coins (Triggers notification)
```bash
curl -X POST "$BASE_URL/api/rewards/coins/add" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "your_user_id",
    "amount": 500,
    "source": "DAILY_LOGIN",
    "description": "7 day login streak bonus"
  }'
```

**Sources you can test:**
- `DAILY_LOGIN`
- `STREAK_MILESTONE`
- `REFERRAL`
- `ACHIEVEMENT`
- `ADMIN_GRANT`
- `PURCHASE`

**Expected Notification:**
- Title: "Coins Earned! 💰"
- Text: "You earned 500 coins for daily login: 7 day login streak bonus"

---

## 2. Reward Catalog Claimed Notification

### View Available Rewards
```bash
curl -X GET "$BASE_URL/api/rewards/catalog" \
  -H "Authorization: Bearer $TOKEN"
```

### Check Your Coin Balance
```bash
curl -X GET "$BASE_URL/api/rewards/coins" \
  -H "Authorization: Bearer $TOKEN"
```

### Claim a Digital Reward
```bash
curl -X POST "$BASE_URL/api/rewards/catalog/claim" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rewardCatalogId": 1,
    "recipientName": "John Doe",
    "recipientEmail": "john@example.com",
    "recipientPhone": "+1234567890"
  }'
```

### Claim a Physical Reward
```bash
curl -X POST "$BASE_URL/api/rewards/catalog/claim" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rewardCatalogId": 2,
    "recipientName": "John Doe",
    "recipientEmail": "john@example.com",
    "recipientPhone": "+1234567890",
    "shippingAddress": "123 Main Street, Apt 4B",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "USA"
  }'
```

**Expected Notifications:**
1. **Reward Claimed:**
   - Title: "Reward Claimed! 🎁"
   - Text: "You've successfully claimed '[Reward Name]' for [X] coins! Order #[TXN-XXX]. You have [Y] coins remaining."

2. **Low Balance (if balance ≤ 100):**
   - Title: "Low Coin Balance ⚠️"
   - Text: "You only have [Y] coins remaining. Complete challenges to earn more coins!"

---

## 3. Transaction Status Update Notification

### Get Your Transactions
```bash
curl -X GET "$BASE_URL/api/rewards/transactions" \
  -H "Authorization: Bearer $TOKEN"
```

### View Specific Transaction
```bash
curl -X GET "$BASE_URL/api/rewards/transactions/1" \
  -H "Authorization: Bearer $TOKEN"
```

### Update Transaction Status (Admin Only)
**Note:** This would require admin authentication. These are examples for testing.

#### Mark as Processing
```bash
curl -X PUT "$BASE_URL/api/admin/transactions/1/status" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "PROCESSING",
    "adminNotes": "Order is being prepared"
  }'
```

#### Mark as Shipped
```bash
curl -X PUT "$BASE_URL/api/admin/transactions/1/status" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "SHIPPED",
    "trackingNumber": "TRACK123456789",
    "adminNotes": "Shipped via FedEx - Expected delivery in 3-5 business days"
  }'
```

#### Mark as Delivered
```bash
curl -X PUT "$BASE_URL/api/admin/transactions/1/status" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "DELIVERED",
    "adminNotes": "Package delivered successfully"
  }'
```

#### Cancel Order
```bash
curl -X PUT "$BASE_URL/api/admin/transactions/1/status" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "CANCELLED",
    "adminNotes": "Out of stock - coins refunded"
  }'
```

**Expected Notifications:**
- **PENDING:** "Order Placed! 📦"
- **PROCESSING:** "Order Processing 🔄"
- **SHIPPED:** "Order Shipped! 🚚" (includes tracking number)
- **DELIVERED:** "Order Delivered! ✅"
- **CANCELLED:** "Order Cancelled ❌"

---

## 4. Low Balance Warning

This notification is **automatically triggered** after a reward claim if balance ≤ 100 coins.

To test:
1. Ensure your balance is above 100 but close (e.g., 150 coins)
2. Claim a reward that costs 50+ coins
3. Your remaining balance will be ≤ 100
4. Low balance notification will be sent automatically

---

## 5. Reward Back in Stock Notification

This requires admin access to update catalog items.

### Check Current Catalog Item
```bash
curl -X GET "$BASE_URL/api/rewards/catalog/1" \
  -H "Authorization: Bearer $TOKEN"
```

### Update Stock (Admin Only)
```bash
curl -X PUT "$BASE_URL/api/admin/rewards/catalog/1" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Premium Headphones",
    "description": "High-quality wireless headphones",
    "category": "Electronics",
    "rewardType": "PHYSICAL",
    "coinPrice": 5000,
    "stockQuantity": 10,
    "isActive": true
  }'
```

### Manual Notification (Admin/System)
If you have a list of interested users:
```bash
# This would be called programmatically, not via REST API
# Example in Kotlin:
rewardService.notifyRewardBackInStock(
    catalogId = 1,
    interestedUserIds = listOf("user1", "user2", "user3")
)
```

**Expected Notification:**
- Title: "Reward Back in Stock! 🎉"
- Text: "'Premium Headphones' is back in stock for 5000 coins! Claim it before it's gone again."

---

## Monitoring & Debugging

### Check Notification Queue Stats
```bash
curl -X GET "$BASE_URL/api/notifications/queue/stats" \
  -H "Authorization: Bearer $TOKEN"
```

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

### Get All Notifications
```bash
curl -X GET "$BASE_URL/api/notifications" \
  -H "Authorization: Bearer $TOKEN"
```

### Get Unread Notifications Only
```bash
curl -X GET "$BASE_URL/api/notifications?isRead=false" \
  -H "Authorization: Bearer $TOKEN"
```

### Get Unread Count
```bash
curl -X GET "$BASE_URL/api/notifications/unread-count" \
  -H "Authorization: Bearer $TOKEN"
```

### Mark Notification as Read
```bash
curl -X POST "$BASE_URL/api/notifications/read" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "notificationId": 123
  }'
```

### Mark All as Read
```bash
curl -X POST "$BASE_URL/api/notifications/read-all" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Test Flow: Complete User Journey

### 1. Start with checking your coins
```bash
curl -X GET "$BASE_URL/api/rewards/coins" \
  -H "Authorization: Bearer $TOKEN"
```

### 2. Earn some coins (get notification)
```bash
curl -X POST "$BASE_URL/api/rewards/coins/add" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "your_user_id",
    "amount": 1000,
    "source": "ACHIEVEMENT",
    "description": "Completed 50 focus sessions"
  }'
```

### 3. Check notifications (you should see coins added)
```bash
curl -X GET "$BASE_URL/api/notifications?isRead=false" \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Browse catalog
```bash
curl -X GET "$BASE_URL/api/rewards/catalog" \
  -H "Authorization: Bearer $TOKEN"
```

### 5. Claim a reward (get notification)
```bash
curl -X POST "$BASE_URL/api/rewards/catalog/claim" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rewardCatalogId": 1,
    "recipientName": "Test User",
    "recipientEmail": "test@example.com"
  }'
```

### 6. Check notifications again (reward claimed + possibly low balance)
```bash
curl -X GET "$BASE_URL/api/notifications?isRead=false" \
  -H "Authorization: Bearer $TOKEN"
```

### 7. Check your transactions
```bash
curl -X GET "$BASE_URL/api/rewards/transactions" \
  -H "Authorization: Bearer $TOKEN"
```

### 8. Admin updates order status (get notification)
Admin marks order as shipped, you receive notification.

### 9. Check final notification
```bash
curl -X GET "$BASE_URL/api/notifications" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Notification Types Reference

| Type | Trigger | Deeplink |
|------|---------|----------|
| `coins_added` | Coins earned | `app://rewards/coins` |
| `reward_claimed` | Reward claimed | `app://rewards/transactions/{txn}` |
| `transaction_status` | Order status changed | `app://rewards/transactions/{txn}` |
| `low_balance` | Balance ≤ 100 after purchase | `app://challenges` |
| `reward_back_in_stock` | Item restocked | `app://rewards/catalog/{id}` |

---

## Common Issues

### No Notifications Received
1. Check if notification queue consumer is started: `GET /api/notifications/queue/stats`
2. Verify user has Firebase token registered
3. Check server logs for errors
4. Ensure notifications are being saved to database: `GET /api/notifications`

### Duplicate Notifications
This shouldn't happen as the system uses idempotent operations, but if it does:
- Check if the same request is being made multiple times
- Review transaction logs

### Notification Delays
- Normal delay: < 1 second
- If delays > 5 seconds, check `queueSize` in stats
- Consider increasing worker count if queue is backing up

---

## Production Considerations

### Environment Variables
```bash
export BASE_URL="https://your-production-domain.com"
export TOKEN="production_token"
```

### Rate Limiting
Be mindful of rate limits when testing:
- Coins add: Max 10 per minute per user
- Catalog claim: Max 1 per minute per user
- Status updates: Admin only, no user limit

### Monitoring
Set up alerts for:
- `queueSize > 100` (queue backing up)
- `totalFailed > 10` (high failure rate)
- `isStarted == false` (consumer not running)

---

## Quick Debug Commands

```bash
# One-liner to check system health
curl -s "$BASE_URL/api/notifications/queue/stats" -H "Authorization: Bearer $TOKEN" | jq

# Check your recent notifications
curl -s "$BASE_URL/api/notifications?limit=5" -H "Authorization: Bearer $TOKEN" | jq

# Check coin balance and recent transactions
curl -s "$BASE_URL/api/rewards/coins" -H "Authorization: Bearer $TOKEN" | jq
```

---

## Support

For issues or questions:
1. Check server logs: `tail -f server.log`
2. Review notification queue stats
3. Verify database entries: Check `notifications` table
4. Test with different notification types to isolate the issue

All notification operations are logged with emojis for easy identification:
- ✅ Success
- ❌ Error
- ⚠️ Warning
- 💰 Coins
- 🎁 Rewards
- 📦 Orders

