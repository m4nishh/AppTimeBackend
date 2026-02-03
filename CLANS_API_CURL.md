# Focus Clans API - CURL Quick Reference

## Environment Setup

```bash
# Set your base URL and token
export BASE_URL="http://localhost:8080"
export TOKEN="your_auth_token_here"
```

## Clan Management

### Create a Clan
```bash
curl -X POST "${BASE_URL}/api/clans" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Pune Developers",
    "description": "A community of passionate developers from Pune",
    "tagline": "Code. Focus. Grow.",
    "clanType": "PUBLIC",
    "maxMembers": 50,
    "country": "India",
    "city": "Pune",
    "category": "Developers"
  }'
```

### List All Clans
```bash
# Basic list
curl -X GET "${BASE_URL}/api/clans"

# With filters
curl -X GET "${BASE_URL}/api/clans?category=Developers&city=Pune&page=1&pageSize=20" \
  -H "Authorization: Bearer ${TOKEN}"

# Search clans
curl -X GET "${BASE_URL}/api/clans?q=developers" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Get Clan Details
```bash
curl -X GET "${BASE_URL}/api/clans/1" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Update Clan (Admin Only)
```bash
curl -X PATCH "${BASE_URL}/api/clans/1" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Updated description",
    "tagline": "New tagline",
    "maxMembers": 75
  }'
```

### Delete Clan (Creator Only)
```bash
curl -X DELETE "${BASE_URL}/api/clans/1" \
  -H "Authorization: Bearer ${TOKEN}"
```

## Membership Operations

### Join a Public Clan
```bash
curl -X POST "${BASE_URL}/api/clans/join" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "clanId": 1
  }'
```

### Join with Invite Code
```bash
curl -X POST "${BASE_URL}/api/clans/join" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "inviteCode": "ABC12345"
  }'
```

### Request to Join Private Clan
```bash
curl -X POST "${BASE_URL}/api/clans/join" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "clanId": 1,
    "message": "I am a passionate developer from Pune. Would love to join!"
  }'
```

### Leave Clan
```bash
curl -X POST "${BASE_URL}/api/clans/1/leave" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Get My Clan Info
```bash
curl -X GET "${BASE_URL}/api/clans/my-clan" \
  -H "Authorization: Bearer ${TOKEN}"
```

## Clan Members

### Get Clan Members
```bash
curl -X GET "${BASE_URL}/api/clans/1/members" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Update Member Role (Admin Only)
```bash
curl -X PATCH "${BASE_URL}/api/clans/1/members/role" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user456",
    "role": "MODERATOR"
  }'
```

### Remove Member (Admin/Moderator Only)
```bash
curl -X POST "${BASE_URL}/api/clans/1/members/remove" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user789",
    "reason": "Inactive for 30 days"
  }'
```

## Invites

### Create Invite (Admin/Moderator Only)
```bash
# Single-use invite for specific user
curl -X POST "${BASE_URL}/api/clans/1/invites" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "inviteeUserId": "user456",
    "maxUses": 1,
    "expiresInDays": 7
  }'

# Multi-use invite (anyone can use)
curl -X POST "${BASE_URL}/api/clans/1/invites" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "maxUses": 10,
    "expiresInDays": 7
  }'

# Unlimited invite that never expires
curl -X POST "${BASE_URL}/api/clans/1/invites" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "maxUses": -1
  }'
```

### Accept Invite
```bash
curl -X POST "${BASE_URL}/api/clans/invites/accept" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "inviteCode": "ABC12345"
  }'
```

## Join Requests (Private Clans)

### Get Pending Join Requests (Admin/Moderator Only)
```bash
curl -X GET "${BASE_URL}/api/clans/1/join-requests" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Approve Join Request
```bash
curl -X POST "${BASE_URL}/api/clans/1/join-requests/5/review" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "approved": true
  }'
```

### Reject Join Request
```bash
curl -X POST "${BASE_URL}/api/clans/1/join-requests/5/review" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "approved": false
  }'
```

## Leaderboards

### Get Daily Leaderboard
```bash
# Today's leaderboard
curl -X GET "${BASE_URL}/api/clans/leaderboard?period=daily" \
  -H "Authorization: Bearer ${TOKEN}"

# Specific date
curl -X GET "${BASE_URL}/api/clans/leaderboard?period=daily&periodDate=2026-02-02" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Get Weekly Leaderboard
```bash
# This week's leaderboard
curl -X GET "${BASE_URL}/api/clans/leaderboard?period=weekly" \
  -H "Authorization: Bearer ${TOKEN}"

# Specific week
curl -X GET "${BASE_URL}/api/clans/leaderboard?period=weekly&periodDate=2026-W05" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Get Monthly Leaderboard
```bash
# This month's leaderboard
curl -X GET "${BASE_URL}/api/clans/leaderboard?period=monthly" \
  -H "Authorization: Bearer ${TOKEN}"

# Specific month
curl -X GET "${BASE_URL}/api/clans/leaderboard?period=monthly&periodDate=2026-02" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Get Top 50 Clans
```bash
curl -X GET "${BASE_URL}/api/clans/leaderboard?period=weekly&limit=50" \
  -H "Authorization: Bearer ${TOKEN}"
```

## Stats & Badges

### Get Clan Stats
```bash
curl -X GET "${BASE_URL}/api/clans/1/stats" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Get Clan Badges
```bash
curl -X GET "${BASE_URL}/api/clans/1/badges" \
  -H "Authorization: Bearer ${TOKEN}"
```

## Complete User Flow Example

### Scenario: Create a clan, invite friends, and check leaderboard

```bash
#!/bin/bash

# Set variables
export BASE_URL="http://localhost:8080"
export CREATOR_TOKEN="creator_token"
export FRIEND_TOKEN="friend_token"

echo "Step 1: Create a clan"
CLAN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/clans" \
  -H "Authorization: Bearer ${CREATOR_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Pune Developers",
    "description": "Focus together, grow together",
    "clanType": "PUBLIC",
    "maxMembers": 50,
    "category": "Developers",
    "city": "Pune"
  }')

CLAN_ID=$(echo $CLAN_RESPONSE | jq -r '.id')
echo "Created clan with ID: ${CLAN_ID}"

echo "\nStep 2: Create an invite"
INVITE_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/clans/${CLAN_ID}/invites" \
  -H "Authorization: Bearer ${CREATOR_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "maxUses": 10,
    "expiresInDays": 7
  }')

INVITE_CODE=$(echo $INVITE_RESPONSE | jq -r '.inviteCode')
echo "Invite code: ${INVITE_CODE}"

echo "\nStep 3: Friend joins using invite"
curl -s -X POST "${BASE_URL}/api/clans/invites/accept" \
  -H "Authorization: Bearer ${FRIEND_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"inviteCode\": \"${INVITE_CODE}\"}"

echo "\nStep 4: Friend submits focus session"
curl -s -X POST "${BASE_URL}/api/focus/submit" \
  -H "Authorization: Bearer ${FRIEND_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "focusDuration": 3600000,
    "startTime": "2026-02-02T10:00:00Z",
    "endTime": "2026-02-02T11:00:00Z",
    "sessionType": "work"
  }'

echo "\nStep 5: Check clan details"
curl -s -X GET "${BASE_URL}/api/clans/${CLAN_ID}" \
  -H "Authorization: Bearer ${CREATOR_TOKEN}" | jq

echo "\nStep 6: Check leaderboard position"
curl -s -X GET "${BASE_URL}/api/clans/leaderboard?period=daily" \
  -H "Authorization: Bearer ${CREATOR_TOKEN}" | jq
```

## Testing Different Clan Types

### PUBLIC Clan
```bash
# Anyone can join directly
curl -X POST "${BASE_URL}/api/clans" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Open Developers",
    "clanType": "PUBLIC",
    "maxMembers": 100
  }'
```

### PRIVATE Clan
```bash
# Requires join request approval
curl -X POST "${BASE_URL}/api/clans" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Elite Developers",
    "clanType": "PRIVATE",
    "maxMembers": 25
  }'
```

### INVITE_ONLY Clan
```bash
# Can only join with invite code
curl -X POST "${BASE_URL}/api/clans" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Secret Developers",
    "clanType": "INVITE_ONLY",
    "maxMembers": 10
  }'
```

## Common Queries

### Find Clans in My City
```bash
curl -X GET "${BASE_URL}/api/clans?city=Pune&page=1" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Find Developer Clans
```bash
curl -X GET "${BASE_URL}/api/clans?category=Developers" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Search for Specific Clan
```bash
curl -X GET "${BASE_URL}/api/clans?q=UPSC" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Check My Rank in Clan
```bash
# Get your clan info
curl -X GET "${BASE_URL}/api/clans/my-clan" \
  -H "Authorization: Bearer ${TOKEN}" | jq '.contributionStats.rankInClan'
```

### Check Clan's Leaderboard Position
```bash
curl -X GET "${BASE_URL}/api/clans/leaderboard?period=weekly" \
  -H "Authorization: Bearer ${TOKEN}" | jq '.userClanRank'
```

## Error Handling Examples

### Try to Join When Already in a Clan
```bash
# Will return 409 Conflict
curl -X POST "${BASE_URL}/api/clans/join" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"clanId": 2}'
```

### Try to Update Clan Without Permission
```bash
# Will return 403 Forbidden
curl -X PATCH "${BASE_URL}/api/clans/1" \
  -H "Authorization: Bearer ${NON_ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"description": "Hacked!"}'
```

### Try to Accept Expired Invite
```bash
# Will return 400 Bad Request
curl -X POST "${BASE_URL}/api/clans/invites/accept" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"inviteCode": "EXPIRED123"}'
```

## Response Examples

### Successful Clan Creation
```json
{
  "id": 1,
  "name": "Pune Developers",
  "description": "Focus together, grow together",
  "tagline": null,
  "logoUrl": null,
  "clanType": "PUBLIC",
  "maxMembers": 50,
  "currentMembers": 1,
  "totalFocusHours": 0,
  "creatorId": "user123",
  "isActive": true,
  "country": "India",
  "city": "Pune",
  "category": "Developers",
  "createdAt": "2026-02-02T10:00:00Z",
  "updatedAt": "2026-02-02T10:00:00Z",
  "rank": null,
  "userRole": "ADMIN",
  "isMember": true
}
```

### Clan Details Response
```json
{
  "clan": {...},
  "members": [
    {
      "id": 1,
      "userId": "user123",
      "username": "john_doe",
      "role": "ADMIN",
      "contributedFocusHours": 72000000,
      "joinedAt": "2026-02-02T10:00:00Z"
    }
  ],
  "stats": {
    "totalFocusHours": 72000000,
    "dailyFocusHours": 7200000,
    "weeklyFocusHours": 36000000,
    "monthlyFocusHours": 72000000,
    "topContributors": [...]
  },
  "badges": [
    {
      "id": 1,
      "badgeType": "MILESTONE_100H",
      "title": "100 Hour Milestone",
      "description": "Achieved 100 total focus hours",
      "earnedAt": "2026-02-01T15:30:00Z"
    }
  ]
}
```

### Leaderboard Response
```json
{
  "period": "weekly",
  "periodDate": "2026-W05",
  "entries": [
    {
      "rank": 1,
      "clanId": 5,
      "clanName": "UPSC Warriors",
      "clanLogoUrl": null,
      "totalFocusHours": 360000000,
      "activeMembersCount": 25,
      "currentMembers": 30,
      "category": "Students",
      "city": "Delhi",
      "country": "India"
    }
  ],
  "userClanRank": 5,
  "totalClans": 150
}
```

## Tips

1. **Authentication**: All endpoints (except GET /api/clans list) require authentication
2. **Time Format**: All times are in ISO 8601 format (e.g., `2026-02-02T10:00:00Z`)
3. **Duration Units**: All durations are in milliseconds (3600000 = 1 hour)
4. **Pagination**: Use `page` and `pageSize` query params for listing
5. **Error Messages**: Check response body for detailed error messages

## Next Steps

After testing the API:
1. Integrate into your mobile app
2. Add UI for clan discovery and management
3. Show clan stats on focus completion
4. Implement push notifications for clan events
5. Add clan activity feed

---

**Happy Testing! 🚀**

