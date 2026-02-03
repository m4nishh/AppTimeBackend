# Welcome Bonus - Quick Testing Guide

## 🚀 Test in 3 Steps

### Step 1: Register a New User
```bash
curl -X POST "http://localhost:8080/api/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceInfo": {
      "deviceId": "welcome_test_001",
      "manufacturer": "Samsung",
      "model": "Galaxy S21",
      "brand": "Samsung",
      "product": "Galaxy",
      "device": "SM-G991B",
      "hardware": "exynos",
      "androidVersion": "12",
      "sdkVersion": "31"
    },
    "firebaseToken": "test_token"
  }'
```

**Expected:** User created successfully ✅

### Step 2: Verify Coins Were Added

First, extract the `userId` from the registration response, then get an auth token and check coins:

```bash
# Replace USER_TOKEN with actual token
curl -X GET "http://localhost:8080/api/rewards/coins" \
  -H "Authorization: Bearer USER_TOKEN"
```

**Expected:**
```json
{
  "success": true,
  "data": {
    "totalCoins": 10,
    "coinHistory": [
      {
        "amount": 10,
        "source": "ADMIN_GRANT",
        "description": "Welcome bonus! Thanks for joining our platform."
      }
    ]
  }
}
```

### Step 3: Check Notification

```bash
curl -X GET "http://localhost:8080/api/notifications" \
  -H "Authorization: Bearer USER_TOKEN"
```

**Expected:**
```json
{
  "success": true,
  "data": {
    "notifications": [
      {
        "title": "Coins Earned! 💰",
        "text": "You earned 10 coins for admin grant: Welcome bonus! Thanks for joining our platform.",
        "type": "coins_added"
      }
    ]
  }
}
```

---

## ✅ Success Checklist

After testing, you should see:
- [x] User registered successfully
- [x] User has 10 coins in balance
- [x] Coin source is "ADMIN_GRANT"
- [x] Description mentions "Welcome bonus"
- [x] Notification sent with "Coins Earned" title
- [x] Server logs show welcome bonus messages

---

## 🔍 Server Logs to Look For

```bash
# Check server logs
tail -f server.log | grep "Welcome Bonus"
```

**Expected logs:**
```
🎉 [Welcome Bonus] New user registered: <userId>
💰 [Welcome Bonus] Added 10 coins to user <userId>, coin ID: <id>
✅ [Welcome Bonus] Enqueued welcome notification for user <userId>
```

---

## 🧪 Test Scenarios

### Scenario 1: New User ✅
**Action:** Register with unique device ID
**Expected:** Gets 10 coins + notification

### Scenario 2: Existing User 🔄
**Action:** Register with same device ID again
**Expected:** No bonus (logs show "Existing user logged in")

### Scenario 3: Multiple New Users 👥
**Action:** Register 3 different users with unique device IDs
**Expected:** Each gets 10 coins independently

---

## 🐛 Troubleshooting

### Problem: User registered but no coins

**Check:**
1. Server logs for errors
```bash
grep "Welcome Bonus" server.log | grep "Failed"
```

2. Database connection
```bash
# Check if coins table is accessible
curl -X GET "http://localhost:8080/api/notifications/queue/stats" \
  -H "Authorization: Bearer TOKEN"
```

3. Reward system is initialized
```bash
# Try adding coins manually
curl -X POST "http://localhost:8080/api/rewards/coins/add" \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test_user",
    "amount": 10,
    "source": "ADMIN_GRANT",
    "description": "Test"
  }'
```

### Problem: No notification received

**Check:**
1. Notification queue is running
```bash
curl -X GET "http://localhost:8080/api/notifications/queue/stats" \
  -H "Authorization: Bearer TOKEN"
```
Look for `"isStarted": true`

2. Notifications are being saved
```bash
curl -X GET "http://localhost:8080/api/notifications" \
  -H "Authorization: Bearer TOKEN"
```

---

## 📊 Verification Queries

### Check Database Directly

```sql
-- Check if user received welcome bonus
SELECT 
    u.user_id,
    u.username,
    c.amount,
    c.description,
    c.created_at
FROM users u
LEFT JOIN coins c ON u.user_id = c.user_id 
WHERE u.device_id = 'welcome_test_001';

-- Count welcome bonuses given today
SELECT COUNT(*) 
FROM coins 
WHERE source = 'ADMIN_GRANT' 
  AND description LIKE '%Welcome bonus%'
  AND DATE(created_at) = CURRENT_DATE;

-- Check notifications sent
SELECT 
    n.user_id,
    n.title,
    n.text,
    n.created_at
FROM notifications n
WHERE n.type = 'coins_added'
  AND n.text LIKE '%Welcome bonus%'
ORDER BY n.created_at DESC
LIMIT 10;
```

---

## 🎯 Expected Results Summary

| Test | Expected Result |
|------|----------------|
| New user registration | ✅ Success |
| Coins added | ✅ 10 coins |
| Coin source | ✅ ADMIN_GRANT |
| Coin description | ✅ "Welcome bonus..." |
| Notification sent | ✅ "Coins Earned! 💰" |
| Existing user re-registration | ✅ No bonus |
| Server logs | ✅ Welcome bonus messages |
| Queue stats | ✅ Processing normally |

---

## 🚀 Quick Test Script

Save this as `test-welcome-bonus.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"
DEVICE_ID="test_$(date +%s)"

echo "🧪 Testing Welcome Bonus Feature"
echo "================================"
echo ""

# Step 1: Register new user
echo "📝 Step 1: Registering new user with device ID: $DEVICE_ID"
RESPONSE=$(curl -s -X POST "$BASE_URL/api/users/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"deviceInfo\": {
      \"deviceId\": \"$DEVICE_ID\",
      \"manufacturer\": \"TestDevice\",
      \"model\": \"Test Model\",
      \"brand\": \"Test\",
      \"product\": \"Test\",
      \"device\": \"test\",
      \"hardware\": \"test\",
      \"androidVersion\": \"12\",
      \"sdkVersion\": \"31\"
    }
  }")

echo "Response: $RESPONSE"
echo ""

# Extract userId (requires jq)
USER_ID=$(echo $RESPONSE | jq -r '.data.userId')
echo "✅ User ID: $USER_ID"
echo ""

echo "⏳ Waiting 2 seconds for welcome bonus to process..."
sleep 2

echo ""
echo "📊 Check server logs for these messages:"
echo "  🎉 [Welcome Bonus] New user registered"
echo "  💰 [Welcome Bonus] Added 10 coins"
echo "  ✅ [Welcome Bonus] Enqueued welcome notification"
echo ""

echo "🔍 To verify manually:"
echo "1. Check coins: GET /api/rewards/coins (requires auth)"
echo "2. Check notifications: GET /api/notifications (requires auth)"
echo "3. Check server logs: grep 'Welcome Bonus' server.log"
echo ""

echo "✅ Test complete!"
```

Run:
```bash
chmod +x test-welcome-bonus.sh
./test-welcome-bonus.sh
```

---

## 📈 Success Metrics

After deploying this feature, track:
- ✅ % of new users who receive welcome bonus (should be 100%)
- ✅ Average time to first coin spend
- ✅ User retention rate (welcome bonus vs no bonus)
- ✅ Conversion to active users

---

**Status:** ✅ Ready to Test
**Last Updated:** January 27, 2026

