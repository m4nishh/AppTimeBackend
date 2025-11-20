# Ngrok Deployment - Challenges Feature

## 🎉 Deployment Status: LIVE

### Public URL
```
https://7d76ddc7b74f.ngrok-free.app
```

### Server Status
- ✅ Server running on port 8080
- ✅ Database connected
- ✅ Challenges seeded (8 challenges)
- ✅ Ngrok tunnel active

---

## 📋 Quick Test Endpoints

### 1. Health Check
```bash
curl "https://7d76ddc7b74f.ngrok-free.app/health"
```

### 2. Get Active Challenges (Public)
```bash
curl -H "ngrok-skip-browser-warning: true" \
  "https://7d76ddc7b74f.ngrok-free.app/api/challenges/active"
```

### 3. Join a Challenge (Requires Auth)
```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  -H "ngrok-skip-browser-warning: true" \
  -d '{"challengeId": 1}' \
  "https://7d76ddc7b74f.ngrok-free.app/api/challenges/join"
```

### 4. Get Challenge Details
```bash
curl -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  -H "ngrok-skip-browser-warning: true" \
  "https://7d76ddc7b74f.ngrok-free.app/api/challenges/1"
```

### 5. Get Rankings
```bash
curl -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  -H "ngrok-skip-browser-warning: true" \
  "https://7d76ddc7b74f.ngrok-free.app/api/challenges/1/rankings"
```

---

## 🎯 Available Challenges

### LESS_SCREENTIME Challenges (Lower screen time = better rank)

1. **Digital Detox Challenge** (ID: 1)
   - Duration: 2 weeks
   - Reward: Digital Wellness Badge + Premium Features (1 month)

2. **Weekend Warrior Challenge** (ID: 3)
   - Duration: 1 week
   - Reward: Weekend Warrior Badge

3. **Evening Unplug Challenge** (ID: 5)
   - Duration: 1 week
   - Reward: Sleep Well Badge + Meditation App Subscription (1 month)

4. **Social Media Detox Week** (ID: 7)
   - Duration: 1 week
   - Reward: Social Detox Champion Badge

### MORE_SCREENTIME Challenges (Higher screen time = better rank)

5. **Focus Mode Marathon** (ID: 2)
   - Duration: 1 month
   - Reward: Focus Master Badge + Exclusive Profile Badge

6. **Productivity Power Hour** (ID: 4)
   - Duration: 2 weeks
   - Reward: Productivity Pro Badge + Learning Resources Pack

7. **30-Day Learning Streak** (ID: 6)
   - Duration: 1 month
   - Reward: Lifelong Learner Badge + Course Discount (50% off)

8. **Deep Work Challenge** (ID: 8)
   - Duration: 2 weeks
   - Reward: Deep Work Master Badge + Productivity Tools Bundle

---

## 📝 Important Notes

### Ngrok Free Tier Limitations
- ⚠️ **Browser Warning**: Free ngrok tunnels show a warning page. Use the `ngrok-skip-browser-warning: true` header in API requests
- ⚠️ **Session Timeout**: Free tunnels expire after 2 hours of inactivity
- ⚠️ **URL Changes**: The ngrok URL changes each time you restart ngrok (unless you have a paid plan with reserved domains)

### Server Management

**Check if server is running:**
```bash
curl http://localhost:8080/health
```

**Check ngrok status:**
```bash
curl http://localhost:4040/api/tunnels
```

**View ngrok web interface:**
Open http://localhost:4040 in your browser

**Stop server:**
```bash
kill $(cat /tmp/ktor-server.pid)
```

**Stop ngrok:**
```bash
pkill -f "ngrok http"
```

---

## 🔄 Restarting Deployment

If you need to restart the deployment:

```bash
# 1. Stop existing processes
kill $(cat /tmp/ktor-server.pid) 2>/dev/null
pkill -f "ngrok http" 2>/dev/null

# 2. Rebuild and start server
cd /Users/amankumar/IdeaProjects/AppTimeBackend
./gradlew build --no-daemon
./gradlew run --no-daemon > /tmp/ktor-server.log 2>&1 &
echo $! > /tmp/ktor-server.pid

# 3. Wait for server to start
sleep 8

# 4. Start ngrok
ngrok http 8080 --log=stdout > ngrok.log 2>&1 &
sleep 5

# 5. Get new URL
curl -s http://localhost:4040/api/tunnels | \
  python3 -c "import sys, json; data = json.load(sys.stdin); print(data['tunnels'][0]['public_url'] if data.get('tunnels') else 'No tunnels')"
```

Or simply use the deployment script:
```bash
./deploy-ngrok.sh
```

---

## 📚 Full API Documentation

See `CHALLENGES_API_CURL.md` for complete API documentation with all endpoints and examples.

---

## 🐛 Troubleshooting

### Challenges not appearing
- Check database connection: `curl http://localhost:8080/health`
- Verify server logs: `tail -f /tmp/ktor-server.log`
- Check if challenges were seeded: Look for "✅ Seeded X challenges" in logs

### 404 on challenges endpoint
- Ensure server was restarted after adding challenges code
- Check that routes are registered in `Application.kt`
- Verify the endpoint path: `/api/challenges/active`

### Ngrok connection issues
- Check ngrok is running: `curl http://localhost:4040/api/tunnels`
- View ngrok logs: `tail -f ngrok.log`
- Restart ngrok if needed

---

**Last Updated:** 2025-11-20  
**Deployment URL:** https://7d76ddc7b74f.ngrok-free.app

