# Leaderboard API - cURL Examples

## Base URL
```
http://localhost:8080
```
Or use your ngrok URL when deployed.

## Endpoints

### 1. GET /api/leaderboard/daily
**Get daily leaderboard** (Public - No authentication required, but optional for userRank)

**Without Authentication:**
```bash
curl -X GET 'http://localhost:8080/api/leaderboard/daily'
```

**With Authentication (to get userRank):**
```bash
curl -X GET 'http://localhost:8080/api/leaderboard/daily' \
  -H 'Authorization: Bearer YOUR_USER_ID'
```

**With Date Filter:**
```bash
curl -X GET 'http://localhost:8080/api/leaderboard/daily?date=2025-11-14' \
  -H 'Authorization: Bearer YOUR_USER_ID'
```

**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "period": "daily",
    "periodDate": "2025-11-14",
    "entries": [
      {
        "userId": "user-001",
        "username": "john_doe",
        "name": "John Doe",
        "avatar": null,
        "totalScreenTime": 7200000,
        "rank": 1
      },
      {
        "userId": "user-002",
        "username": "jane_smith",
        "name": "Jane Smith",
        "avatar": null,
        "totalScreenTime": 5400000,
        "rank": 2
      },
      {
        "userId": "user-003",
        "username": "alex_brown",
        "name": "Alex Brown",
        "avatar": null,
        "totalScreenTime": 3600000,
        "rank": 3
      }
    ],
    "userRank": 2,
    "totalUsers": 10
  },
  "message": "Daily leaderboard retrieved successfully"
}
```

---

### 2. GET /api/leaderboard/weekly
**Get weekly leaderboard** (Public - No authentication required, but optional for userRank)

**Without Authentication:**
```bash
curl -X GET 'http://localhost:8080/api/leaderboard/weekly'
```

**With Authentication (to get userRank):**
```bash
curl -X GET 'http://localhost:8080/api/leaderboard/weekly' \
  -H 'Authorization: Bearer YOUR_USER_ID'
```

**With Week Date Filter:**
```bash
curl -X GET 'http://localhost:8080/api/leaderboard/weekly?weekDate=2025-W46' \
  -H 'Authorization: Bearer YOUR_USER_ID'
```

**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "period": "weekly",
    "periodDate": "2025-W46",
    "entries": [
      {
        "userId": "user-001",
        "username": "john_doe",
        "name": "John Doe",
        "avatar": null,
        "totalScreenTime": 50400000,
        "rank": 1
      },
      {
        "userId": "user-002",
        "username": "jane_smith",
        "name": "Jane Smith",
        "avatar": null,
        "totalScreenTime": 43200000,
        "rank": 2
      },
      {
        "userId": "user-003",
        "username": "alex_brown",
        "name": "Alex Brown",
        "avatar": null,
        "totalScreenTime": 36000000,
        "rank": 3
      }
    ],
    "userRank": 1,
    "totalUsers": 10
  },
  "message": "Weekly leaderboard retrieved successfully"
}
```

---

## Response Fields

### LeaderboardEntry
- `userId` (String): Unique user identifier
- `username` (String?): User's username
- `name` (String?): User's display name
- `avatar` (String?): User's avatar URL (null for now)
- `totalScreenTime` (Long): Total screen time in milliseconds
- `rank` (Int): User's rank in the leaderboard (1-based)

### LeaderboardResponse
- `period` (String): Period type - "daily" or "weekly"
- `periodDate` (String): Date/week identifier
  - Daily: "YYYY-MM-DD" format (e.g., "2025-11-14")
  - Weekly: "YYYY-WW" format (e.g., "2025-W46")
- `entries` (List<LeaderboardEntry>): List of leaderboard entries sorted by rank
- `userRank` (Int?): Current authenticated user's rank (null if not authenticated or user not found)
- `totalUsers` (Int): Total number of users in the leaderboard

---

## Notes

1. **Authentication**: Optional but recommended. If you provide a Bearer token, the response will include `userRank` showing where the authenticated user ranks.

2. **Screen Time**: All screen times are in milliseconds. To convert:
   - Milliseconds to seconds: divide by 1000
   - Milliseconds to minutes: divide by 60000
   - Milliseconds to hours: divide by 3600000

3. **Date Formats**:
   - Daily: Use `YYYY-MM-DD` format (e.g., "2025-11-14")
   - Weekly: Use `YYYY-WW` format (e.g., "2025-W46" for week 46 of 2025)

4. **Ranking**: Ranks are 1-based (1st place, 2nd place, etc.) and sorted by `totalScreenTime` in descending order (highest first).

---

## Example with ngrok URL

```bash
# Daily leaderboard
curl -X GET 'https://your-ngrok-url.ngrok-free.app/api/leaderboard/daily' \
  -H 'Authorization: Bearer YOUR_USER_ID'

# Weekly leaderboard
curl -X GET 'https://your-ngrok-url.ngrok-free.app/api/leaderboard/weekly' \
  -H 'Authorization: Bearer YOUR_USER_ID'
```

