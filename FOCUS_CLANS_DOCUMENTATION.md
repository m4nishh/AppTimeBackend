# Focus Clans - Complete Documentation

## Overview

Focus Clans is a social feature inspired by Clash of Clans that allows users to form communities and compete together. Users can join clans (e.g., "Pune Developers," "UPSC Aspirants," "Lauftlab Team") and collectively track their focus hours. This creates a **social moat** - making it harder for users to leave the app since their clan members depend on their contributions.

## Key Features

### 1. **Clan Management**
- Create public, private, or invite-only clans
- Customize clan name, description, tagline, logo
- Set maximum member limits (2-200 members)
- Filter by category, city, or country
- Three roles: Admin, Moderator, Member

### 2. **Collective Focus Tracking**
- Automatic tracking of all clan members' focus hours
- Real-time aggregation of daily, weekly, and monthly stats
- Individual contribution tracking for each member
- Top contributors leaderboard within clan

### 3. **Clan Leaderboards**
- Daily, weekly, and monthly leaderboards
- Ranks clans by total collective focus hours
- Shows clan position and rank
- City/country-based filtering available

### 4. **Rewards & Badges**
- Milestone badges (100h, 500h, 1000h, 5000h)
- Leaderboard position badges (Top 10 daily/weekly/monthly)
- Exclusive digital rewards for top-performing clans
- Clan achievement showcase

### 5. **Member Management**
- Invite system with unique codes
- Join request approval for private clans
- Member role management (promote/demote)
- Member removal (admin/moderator only)
- Activity tracking

## Database Schema

### Tables Created

1. **clans** - Main clan information
2. **clan_members** - Membership tracking
3. **clan_stats** - Aggregated statistics by period
4. **clan_invites** - Invitation management
5. **clan_badges** - Achievement tracking
6. **clan_join_requests** - Join request workflow

## API Endpoints

### Clan Management

#### Create a Clan
```http
POST /api/clans
Authorization: Bearer <token>

Request:
{
  "name": "Pune Developers",
  "description": "A community of developers from Pune",
  "tagline": "Code. Focus. Grow.",
  "logoUrl": "https://example.com/logo.png",
  "clanType": "PUBLIC",  // PUBLIC, PRIVATE, INVITE_ONLY
  "maxMembers": 50,
  "country": "India",
  "city": "Pune",
  "category": "Developers"
}

Response: 201 Created
{
  "id": 1,
  "name": "Pune Developers",
  "description": "A community of developers from Pune",
  "clanType": "PUBLIC",
  "currentMembers": 1,
  "totalFocusHours": 0,
  "creatorId": "user123",
  "userRole": "ADMIN",
  "isMember": true,
  ...
}
```

#### List Clans
```http
GET /api/clans?category=Developers&city=Pune&page=1&pageSize=20
Authorization: Bearer <token> (optional)

Response: 200 OK
{
  "clans": [...],
  "totalCount": 150,
  "page": 1,
  "pageSize": 20
}
```

#### Get Clan Details
```http
GET /api/clans/{clanId}
Authorization: Bearer <token> (optional)

Response: 200 OK
{
  "clan": {...},
  "members": [...],
  "stats": {
    "totalFocusHours": 180000000,
    "dailyFocusHours": 7200000,
    "weeklyFocusHours": 54000000,
    "monthlyFocusHours": 162000000,
    "topContributors": [...]
  },
  "badges": [...]
}
```

#### Update Clan (Admin/Moderator Only)
```http
PATCH /api/clans/{clanId}
Authorization: Bearer <token>

Request:
{
  "description": "Updated description",
  "tagline": "New tagline",
  "maxMembers": 75
}

Response: 200 OK
```

#### Delete Clan (Creator Only)
```http
DELETE /api/clans/{clanId}
Authorization: Bearer <token>

Response: 200 OK
```

### Membership

#### Join a Clan
```http
POST /api/clans/join
Authorization: Bearer <token>

Request (Public Clan):
{
  "clanId": 1
}

Request (With Invite Code):
{
  "inviteCode": "ABC12345"
}

Request (Private Clan - Creates Join Request):
{
  "clanId": 1,
  "message": "I'm a passionate developer from Pune!"
}

Response: 200 OK
{
  "id": 10,
  "clanId": 1,
  "userId": "user123",
  "role": "MEMBER",
  "contributedFocusHours": 0,
  "joinedAt": "2026-02-02T10:00:00Z"
}
```

#### Leave a Clan
```http
POST /api/clans/{clanId}/leave
Authorization: Bearer <token>

Response: 200 OK
```

#### Get My Clan Info
```http
GET /api/clans/my-clan
Authorization: Bearer <token>

Response: 200 OK
{
  "clan": {...},
  "memberInfo": {...},
  "contributionStats": {
    "totalContributed": 36000000,  // milliseconds
    "dailyContribution": 7200000,
    "weeklyContribution": 21600000,
    "monthlyContribution": 72000000,
    "rankInClan": 3
  }
}
```

### Invites

#### Create Invite (Admin/Moderator Only)
```http
POST /api/clans/{clanId}/invites
Authorization: Bearer <token>

Request:
{
  "inviteeUserId": "user456",  // Optional - specific user
  "maxUses": 1,  // 1 for single use, -1 for unlimited
  "expiresInDays": 7  // null for never expires
}

Response: 201 Created
{
  "id": 1,
  "clanId": 1,
  "clanName": "Pune Developers",
  "inviteCode": "ABC12345",
  "status": "PENDING",
  "maxUses": 1,
  "currentUses": 0,
  "expiresAt": "2026-02-09T10:00:00Z",
  "createdAt": "2026-02-02T10:00:00Z"
}
```

#### Accept Invite
```http
POST /api/clans/invites/accept
Authorization: Bearer <token>

Request:
{
  "inviteCode": "ABC12345"
}

Response: 200 OK
```

### Member Management

#### Get Clan Members
```http
GET /api/clans/{clanId}/members

Response: 200 OK
[
  {
    "id": 1,
    "userId": "user123",
    "username": "john_doe",
    "role": "ADMIN",
    "contributedFocusHours": 72000000,
    "joinedAt": "2026-01-01T00:00:00Z"
  },
  ...
]
```

#### Update Member Role (Admin Only)
```http
PATCH /api/clans/{clanId}/members/role
Authorization: Bearer <token>

Request:
{
  "userId": "user456",
  "role": "MODERATOR"  // ADMIN, MODERATOR, MEMBER
}

Response: 200 OK
```

#### Remove Member (Admin/Moderator Only)
```http
POST /api/clans/{clanId}/members/remove
Authorization: Bearer <token>

Request:
{
  "userId": "user789",
  "reason": "Inactive for 30 days"
}

Response: 200 OK
```

### Join Requests (Private Clans)

#### Get Pending Join Requests (Admin/Moderator Only)
```http
GET /api/clans/{clanId}/join-requests
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 1,
    "userId": "user789",
    "username": "jane_smith",
    "message": "I'm a passionate developer!",
    "status": "PENDING",
    "createdAt": "2026-02-02T10:00:00Z"
  },
  ...
]
```

#### Review Join Request (Admin/Moderator Only)
```http
POST /api/clans/{clanId}/join-requests/{requestId}/review
Authorization: Bearer <token>

Request:
{
  "approved": true
}

Response: 200 OK
```

### Leaderboards

#### Get Clan Leaderboard
```http
GET /api/clans/leaderboard?period=weekly&limit=20
Authorization: Bearer <token> (optional)

Parameters:
- period: daily, weekly, monthly (default: weekly)
- periodDate: YYYY-MM-DD (daily), YYYY-WW (weekly), YYYY-MM (monthly)
- limit: number of clans to return (default: 20)

Response: 200 OK
{
  "period": "weekly",
  "periodDate": "2026-W05",
  "entries": [
    {
      "rank": 1,
      "clanId": 5,
      "clanName": "UPSC Warriors",
      "clanLogoUrl": "https://example.com/logo.png",
      "totalFocusHours": 360000000,  // 100 hours in milliseconds
      "activeMembersCount": 25,
      "currentMembers": 30,
      "category": "Students",
      "city": "Delhi",
      "country": "India"
    },
    ...
  ],
  "userClanRank": 5,  // Your clan's rank (if you're in one)
  "totalClans": 150
}
```

### Badges & Stats

#### Get Clan Badges
```http
GET /api/clans/{clanId}/badges

Response: 200 OK
[
  {
    "id": 1,
    "badgeType": "TOP_1_WEEKLY",
    "title": "Top 1 - Weekly",
    "description": "Ranked #1 in the weekly leaderboard on 2026-W05",
    "earnedAt": "2026-02-02T00:00:00Z"
  },
  {
    "id": 2,
    "badgeType": "MILESTONE_100H",
    "title": "100 Hour Milestone",
    "description": "Achieved 100 total focus hours",
    "earnedAt": "2026-01-15T10:30:00Z"
  },
  ...
]
```

#### Get Clan Stats
```http
GET /api/clans/{clanId}/stats

Response: 200 OK
{
  "totalFocusHours": 360000000,  // lifetime total
  "dailyFocusHours": 14400000,   // today
  "weeklyFocusHours": 108000000,  // this week
  "monthlyFocusHours": 324000000, // this month
  "topContributors": [
    {
      "userId": "user123",
      "username": "john_doe",
      "contributedFocusHours": 72000000,
      "rank": 1
    },
    ...
  ]
}
```

## Automatic Integration with Focus Tracking

When a user submits a focus session via `/api/focus/submit`, the system automatically:

1. Updates the user's personal leaderboard stats
2. **Updates their clan's collective stats** (if they're in a clan)
3. Updates the clan member's contribution counter
4. Recalculates daily, weekly, and monthly clan aggregates
5. Checks for milestone achievements and awards badges

**No additional API calls needed** - it's fully integrated!

## Badge Types

### Milestone Badges
- `MILESTONE_100H` - 100 hours
- `MILESTONE_500H` - 500 hours
- `MILESTONE_1000H` - 1000 hours
- `MILESTONE_5000H` - 5000 hours

### Leaderboard Badges
- `TOP_1_DAILY` to `TOP_10_DAILY`
- `TOP_1_WEEKLY` to `TOP_10_WEEKLY`
- `TOP_1_MONTHLY` to `TOP_10_MONTHLY`

Leaderboard badges are automatically awarded through a scheduled job (can be run daily/weekly/monthly).

## Usage Examples

### User Journey 1: Creating and Growing a Clan

```bash
# 1. User creates a clan
curl -X POST https://api.yourapp.com/api/clans \
  -H "Authorization: Bearer USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Pune Developers",
    "description": "Focus together, grow together",
    "clanType": "PUBLIC",
    "category": "Developers",
    "city": "Pune",
    "maxMembers": 50
  }'

# 2. User creates an invite for friends
curl -X POST https://api.yourapp.com/api/clans/1/invites \
  -H "Authorization: Bearer USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "maxUses": 10,
    "expiresInDays": 7
  }'
# Returns: { "inviteCode": "ABC12345", ... }

# 3. Friend joins using invite code
curl -X POST https://api.yourapp.com/api/clans/join \
  -H "Authorization: Bearer FRIEND_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "inviteCode": "ABC12345" }'

# 4. Friend completes a focus session (automatically updates clan stats)
curl -X POST https://api.yourapp.com/api/focus/submit \
  -H "Authorization: Bearer FRIEND_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "focusDuration": 3600000,
    "startTime": "2026-02-02T10:00:00Z",
    "endTime": "2026-02-02T11:00:00Z",
    "sessionType": "work"
  }'

# 5. Check clan leaderboard position
curl -X GET https://api.yourapp.com/api/clans/leaderboard?period=weekly \
  -H "Authorization: Bearer USER_TOKEN"
```

### User Journey 2: Joining an Existing Clan

```bash
# 1. Browse available clans
curl -X GET https://api.yourapp.com/api/clans?category=Students&city=Delhi

# 2. View clan details
curl -X GET https://api.yourapp.com/api/clans/5

# 3. Join public clan
curl -X POST https://api.yourapp.com/api/clans/join \
  -H "Authorization: Bearer USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "clanId": 5 }'

# 4. Check your clan info
curl -X GET https://api.yourapp.com/api/clans/my-clan \
  -H "Authorization: Bearer USER_TOKEN"
```

## Business Logic & Rules

### Clan Creation
- User must not already be in another clan
- Clan name must be unique
- Name must be 3-100 characters
- Max members: 2-200

### Joining Clans
- **PUBLIC**: Anyone can join immediately
- **PRIVATE**: Requires approval from admin/moderator
- **INVITE_ONLY**: Must have a valid invite code
- User can only be in one clan at a time
- Clan must have available slots

### Leaving Clans
- Creator must assign another admin before leaving (if other members exist)
- If last member leaves, clan becomes inactive

### Role Permissions
- **ADMIN**: Full control - manage members, settings, delete clan
- **MODERATOR**: Approve members, manage invites
- **MEMBER**: View clan info, contribute focus hours

## Frontend Integration Tips

### 1. Clan Discovery Screen
```typescript
// Fetch and display available clans
const response = await fetch('/api/clans?page=1&pageSize=20', {
  headers: { 'Authorization': `Bearer ${token}` }
});
const { clans, totalCount } = await response.json();

// Show filters for category, city, search
```

### 2. Clan Detail Screen
```typescript
// Show clan info, members, stats, badges
const clanDetails = await fetch(`/api/clans/${clanId}`);
const { clan, members, stats, badges } = await clanDetails.json();

// Display:
// - Clan header (name, logo, tagline)
// - Total focus hours progress bar
// - Member list with contributions
// - Badges showcase
// - Leaderboard position
```

### 3. My Clan Dashboard
```typescript
// User's clan summary
const myClan = await fetch('/api/clans/my-clan', {
  headers: { 'Authorization': `Bearer ${token}` }
});

const { clan, memberInfo, contributionStats } = await myClan.json();

// Show:
// - Your rank within clan
// - Your contributions (daily/weekly/monthly)
// - Clan's overall position in leaderboard
// - Recent clan activities
```

### 4. Focus Session Integration
```typescript
// After focus session, show clan contribution
const focusResponse = await submitFocusSession({...});

// Fetch updated clan stats
const updatedStats = await fetch('/api/clans/my-clan');

// Show toast: "You contributed 1 hour to Pune Developers! 🔥"
```

## Scheduled Jobs

Add to your cron scheduler:

```kotlin
// Award leaderboard badges daily at midnight
suspend fun awardDailyBadges() {
    val clanService = ClanService()
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    clanService.awardLeaderboardBadges("daily", today, topN = 10)
}

// Award weekly badges every Monday
suspend fun awardWeeklyBadges() {
    val clanService = ClanService()
    val weekDate = getWeekDate(LocalDate.now())
    clanService.awardLeaderboardBadges("weekly", weekDate, topN = 10)
}

// Award monthly badges on 1st of each month
suspend fun awardMonthlyBadges() {
    val clanService = ClanService()
    val monthDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    clanService.awardLeaderboardBadges("monthly", monthDate, topN = 10)
}
```

## Notification Integration (Recommended)

Send push notifications for:
- Clan member joins
- Clan reaches milestone (100h, 500h, etc.)
- Clan achieves leaderboard position
- Your contribution pushes clan to next rank
- Admin promotes you to moderator
- Join request approved

Example:
```kotlin
notificationService.sendClanNotification(
    userId = userId,
    title = "🏆 Pune Developers reached #1!",
    message = "Your clan is now ranked #1 in weekly leaderboard! Keep the momentum going!",
    clanId = clanId
)
```

## Testing Checklist

- [ ] Create clan (public, private, invite-only)
- [ ] Join clan (direct, invite code, join request)
- [ ] Leave clan
- [ ] Submit focus session and verify clan stats update
- [ ] Check clan leaderboard (daily, weekly, monthly)
- [ ] Earn milestone badges
- [ ] Promote/demote members
- [ ] Remove members
- [ ] Create and accept invites
- [ ] Review join requests
- [ ] Delete clan

## Scalability Considerations

- Clan stats are aggregated periodically (not real-time)
- Leaderboards show top N clans (configurable limit)
- Invites have expiration to prevent stale data
- Badges are computed asynchronously
- Member contributions cached in clan_members table

## Next Steps / Future Enhancements

1. **Clan Chat** - Add messaging system for clan members
2. **Clan Challenges** - Create clan-specific focus challenges
3. **Clan Wars** - Compete against other clans directly
4. **Clan Themes** - Unlock custom app themes for top clans
5. **Clan Rewards Pool** - Shared rewards that members can claim
6. **Activity Feed** - Show clan member activities in real-time
7. **Clan Levels** - Unlock features as clan grows
8. **Sub-clans** - Create divisions within large clans

## Support

For issues or questions about the Focus Clans feature:
- Check API responses for detailed error messages
- Verify authentication tokens
- Ensure user is not already in a clan when creating/joining
- Check clan type and permissions for operations

---

**Built with ❤️ to create social accountability and community in your focus app!**

