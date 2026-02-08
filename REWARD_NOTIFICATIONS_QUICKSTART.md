# Reward Notifications - Quick Start Guide

⚡ **Get started with reward and coins notifications in 5 minutes**

---

## 🎯 What You Need to Know

The notification system is **fully automated** and requires **no code changes** for basic usage. Notifications are automatically sent when:

- ✅ Users earn coins
- ✅ Users claim rewards
- ✅ Order status changes
- ✅ Balance gets low
- ✅ Items are restocked (manual trigger)

---

## 🚀 Quick Test (3 Steps)

### Step 1: Add Coins
```bash
curl -X POST "http://localhost:8080/api/rewards/coins/add" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "your_user_id",
    "amount": 1000,
    "source": "DAILY_LOGIN",
    "description": "Daily login bonus"
  }'
```

### Step 2: Check Notifications
```bash
curl -X GET "http://localhost:8080/api/notifications?isRead=false" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Step 3: Verify Queue
```bash
curl -X GET "http://localhost:8080/api/notifications/queue/stats" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected:** You should see a "Coins Earned! 💰" notification.

---

## 📱 Notification Types at a Glance

| Icon | When | Where It Takes You |
|------|------|-------------------|
| 💰 | Coins earned | Coin history |
| 🎁 | Reward claimed | Transaction details |
| 📦 | Order update | Transaction details |
| ⚠️ | Low balance | Challenges page |
| 🎉 | Item restocked | Catalog item |

---

## 🔧 For Developers

### Adding a New Notification Type

**1. Add Message Model** (`QueueModels.kt`)
```kotlin
@Serializable
data class YourNotificationMessage(
    override val messageId: String,
    override val timestamp: Long,
    val userId: String,
    // ... your fields
) : NotificationMessage()
```

**2. Add Service Method** (`NotificationService.kt`)
```kotlin
suspend fun sendYourNotification(userId: String, ...) {
    createAndSendNotification(
        userId = userId,
        title = "Your Title",
        text = "Your message",
        type = "your_type",
        deeplink = "app://your/path"
    )
}
```

**3. Add Queue Method** (`NotificationQueueService.kt`)
```kotlin
suspend fun enqueueYourNotification(userId: String, ...) {
    val message = YourNotificationMessage(...)
    enqueue(message)
}
```

**4. Add Processing** (`NotificationQueueService.kt` in `processMessages`)
```kotlin
is YourNotificationMessage -> {
    notificationService.sendYourNotification(
        userId = message.userId,
        ...
    )
    mutex.withLock {
        queue.remove(message)
        totalProcessed++
    }
}
```

**5. Integrate** (e.g., in `RewardService.kt`)
```kotlin
NotificationQueueService.enqueueYourNotification(
    userId = userId,
    ...
)
```

---

## 🎨 Notification Template

Copy this template for consistent notifications:

```kotlin
suspend fun sendYourNotification(
    userId: String,
    param1: String,
    param2: Long
) {
    createAndSendNotification(
        userId = userId,
        title = "Action Completed! 🎊",  // Use emoji for visual appeal
        text = "Description with $param1 and $param2",  // Be specific
        type = "your_type",  // Lowercase, snake_case
        deeplink = "app://path/$param2"  // Where user should go
    )
}
```

**Best Practices:**
- ✅ Use emojis in titles (but not too many)
- ✅ Include specific details (numbers, names)
- ✅ Keep text under 120 characters
- ✅ Provide relevant deeplink
- ✅ Use consistent type naming

---

## 🐛 Debugging

### Is the queue working?
```bash
curl -X GET "http://localhost:8080/api/notifications/queue/stats" \
  -H "Authorization: Bearer YOUR_TOKEN" | jq
```

Look for:
- `"isStarted": true` ✅
- `"queueSize"` should be small (< 10)
- `"totalProcessed"` should be increasing
- `"totalFailed"` should be 0

### Are notifications being saved?
```bash
curl -X GET "http://localhost:8080/api/notifications" \
  -H "Authorization: Bearer YOUR_TOKEN" | jq
```

Should return list of notifications.

### Are push notifications working?
1. Check user has Firebase token: `GET /api/users/profile`
2. Check server logs for FCM errors
3. Test with in-app notifications first (always work)

---

## 📊 Monitoring Dashboard

Create a simple monitoring script:

```bash
#!/bin/bash
# monitor-notifications.sh

TOKEN="your_token_here"
BASE_URL="http://localhost:8080"

echo "📊 Notification System Status"
echo "=============================="
echo ""

# Queue Stats
echo "📬 Queue Statistics:"
curl -s "$BASE_URL/api/notifications/queue/stats" \
  -H "Authorization: Bearer $TOKEN" | jq

echo ""
echo "📱 Recent Notifications:"
curl -s "$BASE_URL/api/notifications?limit=5" \
  -H "Authorization: Bearer $TOKEN" | jq '.notifications[] | {title, type, createdAt}'
```

Run: `chmod +x monitor-notifications.sh && ./monitor-notifications.sh`

---

## 🎓 Common Scenarios

### Scenario 1: User Claims Reward
**What Happens Automatically:**
1. ✅ Reward claimed notification sent
2. ✅ If balance ≤ 100, low balance warning sent
3. ✅ Both saved to database
4. ✅ Both sent as push notifications

**Your Code:**
```kotlin
rewardService.claimRewardCatalog(userId, request)
// That's it! Notifications are automatic
```

### Scenario 2: Admin Updates Order Status
**What Happens Automatically:**
1. ✅ Status update notification sent
2. ✅ Includes tracking number if provided
3. ✅ User gets order update

**Your Code:**
```kotlin
rewardService.updateTransactionStatus(transactionId, request)
// Notification sent automatically
```

### Scenario 3: Adding Coins
**What Happens Automatically:**
1. ✅ Coins added notification sent (if not challenge win)
2. ✅ Source and description included

**Your Code:**
```kotlin
rewardService.addCoins(request)
// Notification queued automatically
```

---

## 🔐 Security Notes

- ✅ All endpoints require authentication
- ✅ Users can only see their own notifications
- ✅ Admin-only endpoints are protected
- ✅ No sensitive data in notification text
- ✅ Deeplinks are safe (handled by app)

---

## 📈 Performance

| Metric | Value |
|--------|-------|
| API Response Time | No impact (async) |
| Notification Delivery | < 1 second |
| Queue Processing | 5 concurrent workers |
| Database Impact | Minimal (indexed) |
| Push Notification | Best effort (graceful fail) |

---

## 🎯 Testing Checklist

Before deploying:
- [ ] Queue consumer is started
- [ ] Test coins notification
- [ ] Test reward claim notification
- [ ] Test status update notification
- [ ] Test low balance notification
- [ ] Check queue stats endpoint
- [ ] Verify database entries
- [ ] Test push notifications (if configured)
- [ ] Monitor queue for 5 minutes
- [ ] Check for any errors in logs

---

## 📚 Documentation

| Document | When to Read |
|----------|-------------|
| `REWARD_NOTIFICATIONS_QUICKSTART.md` | ⭐ Start here |
| `REWARD_NOTIFICATIONS_CURL.md` | For testing |
| `REWARD_NOTIFICATIONS.md` | For deep dive |
| `REWARD_NOTIFICATIONS_SUMMARY.md` | For overview |

---

## 💡 Pro Tips

1. **Monitor the Queue**
   - Add queue stats to your admin dashboard
   - Alert if `queueSize > 50`

2. **Log Analysis**
   - Search for emojis: `grep "💰\|🎁\|📦" server.log`
   - Filter by user: `grep "user123" server.log | grep "notification"`

3. **Testing in Development**
   - Use curl for quick tests
   - Check database directly: `SELECT * FROM notifications ORDER BY created_at DESC LIMIT 5;`

4. **Production Ready**
   - Configure Firebase properly
   - Set up monitoring alerts
   - Have fallback for notification delivery

---

## 🆘 Help

**Something not working?**

1. Check queue: `GET /api/notifications/queue/stats`
2. Check logs: `tail -f server.log`
3. Check database: `SELECT COUNT(*) FROM notifications;`
4. Review docs: `REWARD_NOTIFICATIONS.md`

**Need to disable notifications temporarily?**
```kotlin
// In RewardService, comment out notification calls:
// NotificationQueueService.enqueue...
```

---

## ✅ Success!

If you can:
- ✅ See notifications in database
- ✅ Queue stats show `isStarted: true`
- ✅ Receive push notifications (if Firebase configured)

**You're all set!** 🎉

---

## 🚀 Next Steps

1. **Customize** notification text for your brand
2. **Add** notification preferences
3. **Implement** wishlist for back-in-stock
4. **Monitor** usage patterns
5. **Optimize** based on data

---

**Quick Links:**
- Test: `REWARD_NOTIFICATIONS_CURL.md`
- Learn: `REWARD_NOTIFICATIONS.md`
- Overview: `REWARD_NOTIFICATIONS_SUMMARY.md`

**Status:** ✅ Production Ready
**Version:** 1.0
**Last Updated:** January 27, 2026

