# Focus Clans - Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Prerequisites
- Backend is running on `localhost:8080`
- You have a valid auth token

### Step 1: Create Your First Clan (30 seconds)

```bash
export TOKEN="your_auth_token"
export BASE_URL="http://localhost:8080"

curl -X POST "${BASE_URL}/api/clans" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My First Clan",
    "description": "Testing the clan system",
    "clanType": "PUBLIC",
    "maxMembers": 50
  }'
```

**Expected Response:**
```json
{
  "id": 1,
  "name": "My First Clan",
  "currentMembers": 1,
  "totalFocusHours": 0,
  "userRole": "ADMIN",
  "isMember": true
  ...
}
```

### Step 2: Complete a Focus Session (1 minute)

```bash
curl -X POST "${BASE_URL}/api/focus/submit" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "focusDuration": 3600000,
    "startTime": "2026-02-02T10:00:00Z",
    "endTime": "2026-02-02T11:00:00Z",
    "sessionType": "work"
  }'
```

**Magic Happens Here:**
- ✅ Your personal stats update
- ✅ Your clan's stats automatically update
- ✅ Your contribution counter increases
- ✅ Daily/weekly/monthly aggregates recalculate

### Step 3: Check Your Clan Stats (30 seconds)

```bash
curl -X GET "${BASE_URL}/api/clans/my-clan" \
  -H "Authorization: Bearer ${TOKEN}"
```

**You should see:**
```json
{
  "clan": {
    "id": 1,
    "name": "My First Clan",
    "totalFocusHours": 3600000  // 1 hour!
  },
  "memberInfo": {
    "contributedFocusHours": 3600000,
    "role": "ADMIN"
  },
  "contributionStats": {
    "totalContributed": 3600000,
    "dailyContribution": 3600000
  }
}
```

### Step 4: Invite a Friend (1 minute)

```bash
# Create invite
INVITE=$(curl -s -X POST "${BASE_URL}/api/clans/1/invites" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"maxUses": 10, "expiresInDays": 7}')

# Extract invite code
INVITE_CODE=$(echo $INVITE | jq -r '.inviteCode')
echo "Share this code: ${INVITE_CODE}"
```

### Step 5: Friend Joins (30 seconds)

```bash
# Friend uses the invite code
export FRIEND_TOKEN="friend_auth_token"

curl -X POST "${BASE_URL}/api/clans/invites/accept" \
  -H "Authorization: Bearer ${FRIEND_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"inviteCode\": \"${INVITE_CODE}\"}"
```

### Step 6: Check Leaderboard (30 seconds)

```bash
curl -X GET "${BASE_URL}/api/clans/leaderboard?period=daily" \
  -H "Authorization: Bearer ${TOKEN}"
```

**See your clan ranked:**
```json
{
  "period": "daily",
  "entries": [
    {
      "rank": 1,
      "clanName": "My First Clan",
      "totalFocusHours": 3600000,
      "currentMembers": 2
    }
  ],
  "userClanRank": 1
}
```

## 🎯 Common Use Cases

### Use Case 1: Create a Study Group

```bash
# Create private clan for your study group
curl -X POST "${BASE_URL}/api/clans" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "UPSC 2026 Batch",
    "description": "Study together for UPSC",
    "clanType": "PRIVATE",
    "maxMembers": 20,
    "category": "Students"
  }'

# Review join requests when people apply
curl -X GET "${BASE_URL}/api/clans/1/join-requests" \
  -H "Authorization: Bearer ${TOKEN}"

# Approve someone
curl -X POST "${BASE_URL}/api/clans/1/join-requests/5/review" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"approved": true}'
```

### Use Case 2: Create a Company Team

```bash
# Create invite-only clan for your team
curl -X POST "${BASE_URL}/api/clans" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "name": "Lauftlab Team",
    "description": "Our productivity champions",
    "clanType": "INVITE_ONLY",
    "maxMembers": 100,
    "category": "Professionals"
  }'

# Create unlimited invite for team email
curl -X POST "${BASE_URL}/api/clans/1/invites" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"maxUses": -1}'  # Unlimited uses
```

### Use Case 3: Track Your Contributions

```bash
# See your personal stats within the clan
curl -X GET "${BASE_URL}/api/clans/my-clan" \
  -H "Authorization: Bearer ${TOKEN}"

# See clan details
curl -X GET "${BASE_URL}/api/clans/1" \
  -H "Authorization: Bearer ${TOKEN}"

# See all members
curl -X GET "${BASE_URL}/api/clans/1/members" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Use Case 4: Manage Your Clan

```bash
# Promote someone to moderator
curl -X PATCH "${BASE_URL}/api/clans/1/members/role" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"userId": "user123", "role": "MODERATOR"}'

# Remove inactive member
curl -X POST "${BASE_URL}/api/clans/1/members/remove" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"userId": "user456", "reason": "Inactive"}'

# Update clan settings
curl -X PATCH "${BASE_URL}/api/clans/1" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"maxMembers": 75, "description": "Updated!"}'
```

## 📱 Frontend Integration Examples

### React/React Native Example

```typescript
// 1. Create Clan Component
const CreateClan = () => {
  const createClan = async (name: string) => {
    const response = await fetch('/api/clans', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        name,
        clanType: 'PUBLIC',
        maxMembers: 50
      })
    });
    return await response.json();
  };
};

// 2. Clan Leaderboard Component
const ClanLeaderboard = () => {
  const [leaderboard, setLeaderboard] = useState(null);
  
  useEffect(() => {
    fetch('/api/clans/leaderboard?period=weekly', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    .then(res => res.json())
    .then(data => setLeaderboard(data));
  }, []);
  
  return (
    <div>
      {leaderboard?.entries.map((clan, index) => (
        <ClanCard 
          key={clan.clanId}
          rank={clan.rank}
          name={clan.clanName}
          hours={clan.totalFocusHours / 3600000} // Convert to hours
          members={clan.currentMembers}
        />
      ))}
    </div>
  );
};

// 3. My Clan Dashboard Component
const MyClanDashboard = () => {
  const [clanInfo, setClanInfo] = useState(null);
  
  useEffect(() => {
    fetch('/api/clans/my-clan', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    .then(res => res.json())
    .then(data => setClanInfo(data));
  }, []);
  
  if (!clanInfo?.clan) {
    return <JoinClanPrompt />;
  }
  
  return (
    <div>
      <h1>{clanInfo.clan.name}</h1>
      <p>Your contribution: {clanInfo.contributionStats.totalContributed / 3600000}h</p>
      <p>Clan total: {clanInfo.clan.totalFocusHours / 3600000}h</p>
      <MembersList members={clanInfo.members} />
    </div>
  );
};

// 4. After Focus Session - Show Contribution
const onFocusComplete = async (session) => {
  // Submit focus session
  await submitFocus(session);
  
  // Show contribution toast
  const clanInfo = await fetch('/api/clans/my-clan');
  showToast(
    `🔥 You contributed ${session.duration}h to ${clanInfo.clan.name}!`
  );
  
  // Update UI with new stats
  updateClanStats(clanInfo);
};
```

### Flutter/Dart Example

```dart
// Clan Service
class ClanService {
  Future<Clan> createClan(String name, ClanType type) async {
    final response = await http.post(
      Uri.parse('$baseUrl/api/clans'),
      headers: {
        'Authorization': 'Bearer $token',
        'Content-Type': 'application/json'
      },
      body: jsonEncode({
        'name': name,
        'clanType': type.toString(),
        'maxMembers': 50
      })
    );
    return Clan.fromJson(jsonDecode(response.body));
  }
  
  Future<ClanLeaderboard> getLeaderboard(Period period) async {
    final response = await http.get(
      Uri.parse('$baseUrl/api/clans/leaderboard?period=${period.name}'),
      headers: {'Authorization': 'Bearer $token'}
    );
    return ClanLeaderboard.fromJson(jsonDecode(response.body));
  }
  
  Future<UserClanInfo> getMyClan() async {
    final response = await http.get(
      Uri.parse('$baseUrl/api/clans/my-clan'),
      headers: {'Authorization': 'Bearer $token'}
    );
    return UserClanInfo.fromJson(jsonDecode(response.body));
  }
}

// Focus completion with clan update
Future<void> onFocusSessionComplete(FocusSession session) async {
  // Submit focus
  await focusService.submitSession(session);
  
  // Get updated clan info
  final clanInfo = await clanService.getMyClan();
  
  // Show notification
  showNotification(
    '🎯 Contributed ${session.duration.inHours}h to ${clanInfo.clan?.name}!'
  );
  
  // Update UI
  setState(() {
    this.clanInfo = clanInfo;
  });
}
```

## 🎨 UI/UX Recommendations

### Key Screens

1. **Clan Discovery**
   - Search bar
   - Category filters (Developers, Students, etc.)
   - City/Country filters
   - Clan cards showing: name, members, total hours, category
   - "Join" button (conditional on clan type)

2. **My Clan Dashboard**
   - Clan header (logo, name, tagline)
   - Stats: Total hours, Today's hours, Weekly hours
   - Your rank and contribution
   - Top 5 contributors
   - Recent activities
   - Action buttons: Invite, Settings (if admin)

3. **Clan Leaderboard**
   - Toggle: Daily/Weekly/Monthly
   - Clan cards with rank badges
   - Your clan highlighted
   - Scroll through top 20-50

4. **Clan Details**
   - Full member list
   - All stats and graphs
   - Badge showcase
   - Join button / Leave button
   - Admin: Member management, Settings

5. **Create/Join Flow**
   - Simple form to create
   - Browse or invite code to join
   - Onboarding: "Your clan depends on you!"

### Visual Elements

```
Clan Card:
┌─────────────────────────────┐
│ 🏆 #1  Pune Developers      │
│ ├─ 500 hours | 25 members   │
│ └─ Developers • Pune, India │
│                      [Join →]│
└─────────────────────────────┘

Contribution Widget:
┌─────────────────────────────┐
│ Your Contribution Today     │
│ ████████░░ 2.5h / 3h goal   │
│ Clan: ████████░░░░ 45h / 60h│
└─────────────────────────────┘

Badge Display:
🥇 Top 1 Weekly
🏅 100 Hour Milestone  
⭐ 500 Hour Milestone
```

## 🔔 Notification Ideas

Engage users with clan notifications:

1. **"Your clan reached #3 on the leaderboard!"**
2. **"Pune Developers just hit 500 hours! 🎉"**
3. **"New member John joined your clan"**
4. **"You're now a Moderator in Pune Developers"**
5. **"Your clan needs 10 more hours to reach #1!"**
6. **"5 members contributed today. Will you?"**

## 🐛 Troubleshooting

### "User is already a member of another clan"
- Users can only be in one clan at a time
- Leave current clan first: `POST /api/clans/{id}/leave`

### "Clan name already exists"
- Clan names must be unique
- Try a different name or add location/year

### "Forbidden" when updating clan
- Check if user is ADMIN or MODERATOR
- Only admins can update most settings

### "Invalid invite code"
- Invite may have expired
- Check if max uses reached
- Request new invite from clan admin

### Clan stats not updating
- Stats update automatically with focus sessions
- May take a few seconds to propagate
- Check `/api/clans/my-clan` for latest stats

## 📊 Monitoring & Analytics

Track these metrics:

```bash
# Active clans
SELECT COUNT(*) FROM clans WHERE is_active = true;

# Users in clans vs solo users
SELECT 
  COUNT(DISTINCT user_id) as users_in_clans,
  (SELECT COUNT(*) FROM users) - COUNT(DISTINCT user_id) as solo_users
FROM clan_members WHERE is_active = true;

# Average focus hours: clan members vs solo
SELECT 
  'clan_members' as type,
  AVG(total_screen_time) as avg_hours
FROM leaderboardstats 
WHERE user_id IN (SELECT user_id FROM clan_members WHERE is_active = true)
UNION
SELECT 
  'solo_users' as type,
  AVG(total_screen_time) as avg_hours
FROM leaderboardstats 
WHERE user_id NOT IN (SELECT user_id FROM clan_members WHERE is_active = true);

# Top clans by total hours
SELECT name, total_focus_hours, current_members 
FROM clans 
ORDER BY total_focus_hours DESC 
LIMIT 10;
```

## ✅ Launch Checklist

Before going live:

- [ ] Test all API endpoints
- [ ] Create 5-10 seed clans for popular categories
- [ ] Setup scheduled jobs for badge awards
- [ ] Configure push notifications
- [ ] Prepare marketing materials
- [ ] Train support team on clan features
- [ ] Monitor error logs
- [ ] Set up analytics tracking
- [ ] Prepare celebration for first clan to hit 1000 hours!

## 🎉 Success!

You now have:
- ✅ Complete clan system
- ✅ Automatic focus tracking integration
- ✅ Leaderboards and badges
- ✅ Social accountability features
- ✅ Scalable architecture

**Next:** Integrate the frontend and watch your retention soar! 🚀

---

Need help? Check out:
- `FOCUS_CLANS_DOCUMENTATION.md` - Complete API documentation
- `CLANS_API_CURL.md` - All CURL commands
- `FOCUS_CLANS_SUMMARY.md` - Implementation overview

**Happy Building! 💪**

