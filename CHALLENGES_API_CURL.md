# Challenges API - cURL Examples

This document provides cURL examples for all Challenge API endpoints.

## Base URL
Replace `YOUR_BASE_URL` with your server URL (e.g., `http://localhost:8080` or `https://your-domain.com`)

## Authentication
Most endpoints require Bearer token authentication. Replace `YOUR_AUTH_TOKEN` with your JWT token.

---

## 1. Get All Active Challenges

Get a list of all currently active challenges.

**Endpoint:** `GET /api/challenges/active`  
**Authentication:** Optional  
- Without a token: returns basic challenge info  
- With a Bearer token: adds `hasJoined` flag indicating if you already joined each challenge

```bash
curl -X GET "http://localhost:8080/api/challenges/active" \
  -H "Content-Type: application/json"
```

**Response Example:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "challenges": [
      {
        "id": 1,
        "title": "Reduce Screen Time Challenge",
        "description": "Reduce your daily screen time by 30%",
        "reward": "Premium Badge",
        "startTime": "2024-01-15T00:00:00Z",
        "endTime": "2024-01-31T23:59:59Z",
        "thumbnail": "https://example.com/thumbnails/challenge1.jpg",
        "hasJoined": true
      }
    ]
  },
  "message": "Active challenges retrieved successfully"
}
```

---

## 2. Join a Challenge

Register/join a challenge as a participant.

**Endpoint:** `POST /api/challenges/join`  
**Authentication:** Required

```bash
curl -X POST "http://localhost:8080/api/challenges/join" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  -d '{
    "challengeId": 1
  }'
```

**Response Example:**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "challengeId": 1,
    "userId": "user123",
    "joinedAt": "2024-01-20T10:30:00Z",
    "message": "Successfully joined challenge: Reduce Screen Time Challenge"
  },
  "message": "Successfully joined challenge: Reduce Screen Time Challenge"
}
```

---

## 3. Get User's Challenges

Get all challenges that the authenticated user has joined (including past challenges).

**Endpoint:** `GET /api/challenges/user`  
**Authentication:** Required

```bash
curl -X GET "http://localhost:8080/api/challenges/user" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN"
```

**Response Example:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "challenges": [
      {
        "id": 1,
        "title": "Reduce Screen Time Challenge",
        "description": "Reduce your daily screen time by 30%",
        "reward": "Premium Badge",
        "startTime": "2024-01-15T00:00:00Z",
        "endTime": "2024-01-31T23:59:59Z",
        "thumbnail": "https://example.com/thumbnails/challenge1.jpg",
        "challengeType": "LESS_SCREENTIME",
        "isActive": true,
        "joinedAt": "2024-01-20T10:30:00Z",
        "isPast": false
      },
      {
        "id": 2,
        "title": "Focus Mode Marathon",
        "description": "Complete 100 hours of focus mode",
        "reward": "Focus Master Badge",
        "startTime": "2024-01-01T00:00:00Z",
        "endTime": "2024-01-10T23:59:59Z",
        "thumbnail": "https://example.com/thumbnails/challenge2.jpg",
        "challengeType": "MORE_SCREENTIME",
        "isActive": false,
        "joinedAt": "2024-01-02T08:00:00Z",
        "isPast": true
      }
    ]
  },
  "message": "User challenges retrieved successfully"
}
```

---

## 4. Get Challenge Details

Get detailed information about a specific challenge including participant count.

**Endpoint:** `GET /api/challenges/{challengeId}`  
**Authentication:** Required

```bash
curl -X GET "http://localhost:8080/api/challenges/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN"
```

**Response Example:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": 1,
    "title": "Reduce Screen Time Challenge",
    "description": "Reduce your daily screen time by 30%",
    "reward": "Premium Badge",
    "startTime": "2024-01-15T00:00:00Z",
    "endTime": "2024-01-31T23:59:59Z",
    "thumbnail": "https://example.com/thumbnails/challenge1.jpg",
    "challengeType": "LESS_SCREENTIME",
    "isActive": true,
    "participantCount": 150,
    "createdAt": "2024-01-10T12:00:00Z"
  },
  "message": "Challenge details retrieved successfully"
}
```

---

## 5. Submit Challenge Stats (Single)

Submit a single app usage stat for a challenge participant.

**Endpoint:** `POST /api/challenges/stats`  
**Authentication:** Required

```bash
curl -X POST "http://localhost:8080/api/challenges/stats" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  -d '{
    "challengeId": 1,
    "appName": "Instagram",
    "packageName": "com.instagram.android",
    "startSyncTime": "2024-01-20T10:00:00Z",
    "endSyncTime": "2024-01-20T10:30:00Z",
    "duration": 1800000
  }'
```

**Response Example:**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "message": "Challenge stats submitted successfully"
  },
  "message": "Challenge stats submitted successfully"
}
```

**Note:** 
- `duration` is in milliseconds
- `startSyncTime` and `endSyncTime` should be in ISO 8601 format
- The duration should match the difference between endSyncTime and startSyncTime (within 1 second tolerance)

---

## 6. Submit Challenge Stats (Batch)

Submit multiple app usage stats for a challenge participant in a single request.

**Endpoint:** `POST /api/challenges/stats/batch`  
**Authentication:** Required

```bash
curl -X POST "http://localhost:8080/api/challenges/stats/batch" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  -d '{
    "challengeId": 1,
    "stats": [
      {
        "challengeId": 1,
        "appName": "Instagram",
        "packageName": "com.instagram.android",
        "startSyncTime": "2024-01-20T10:00:00Z",
        "endSyncTime": "2024-01-20T10:30:00Z",
        "duration": 1800000
      },
      {
        "challengeId": 1,
        "appName": "Twitter",
        "packageName": "com.twitter.android",
        "startSyncTime": "2024-01-20T11:00:00Z",
        "endSyncTime": "2024-01-20T11:15:00Z",
        "duration": 900000
      },
      {
        "challengeId": 1,
        "appName": "YouTube",
        "packageName": "com.google.android.youtube",
        "startSyncTime": "2024-01-20T14:00:00Z",
        "endSyncTime": "2024-01-20T15:00:00Z",
        "duration": 3600000
      }
    ]
  }'
```

**Response Example:**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "submitted": 3,
    "totalDuration": 6300000
  },
  "message": "Challenge stats submitted successfully"
}
```

**Note:** All stats in the batch must belong to the same challenge (challengeId).

---

## 7. Get Challenge Rankings

Get the top 10 rankings for a challenge. Ranking calculation:
- **LESS_SCREENTIME**: Ranked by total duration ascending (lower screen time = better rank)
- **MORE_SCREENTIME**: Ranked by total duration descending (higher screen time = better rank)

**Endpoint:** `GET /api/challenges/{challengeId}/rankings`  
**Authentication:** Required

```bash
curl -X GET "http://localhost:8080/api/challenges/1/rankings" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN"
```

**Response Example (LESS_SCREENTIME challenge):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "challengeId": 1,
    "challengeTitle": "Reduce Screen Time Challenge",
    "challengeType": "LESS_SCREENTIME",
    "rankings": [
      {
        "rank": 1,
        "userId": "user456",
        "totalDuration": 7200000,
        "appCount": 5
      },
      {
        "rank": 2,
        "userId": "user789",
        "totalDuration": 10800000,
        "appCount": 8
      },
      {
        "rank": 3,
        "userId": "user123",
        "totalDuration": 14400000,
        "appCount": 12
      },
      {
        "rank": 4,
        "userId": "user321",
        "totalDuration": 18000000,
        "appCount": 10
      },
      {
        "rank": 5,
        "userId": "user654",
        "totalDuration": 21600000,
        "appCount": 15
      },
      {
        "rank": 6,
        "userId": "user987",
        "totalDuration": 25200000,
        "appCount": 18
      },
      {
        "rank": 7,
        "userId": "user111",
        "totalDuration": 28800000,
        "appCount": 20
      },
      {
        "rank": 8,
        "userId": "user222",
        "totalDuration": 32400000,
        "appCount": 22
      },
      {
        "rank": 9,
        "userId": "user333",
        "totalDuration": 36000000,
        "appCount": 25
      },
      {
        "rank": 10,
        "userId": "user444",
        "totalDuration": 39600000,
        "appCount": 28
      }
    ],
    "userRank": {
      "rank": 3,
      "userId": "user123",
      "totalDuration": 14400000,
      "appCount": 12
    },
    "totalParticipants": 150
  },
  "message": "Challenge rankings retrieved successfully"
}
```

**Response Example (MORE_SCREENTIME challenge):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "challengeId": 2,
    "challengeTitle": "Focus Mode Marathon",
    "challengeType": "MORE_SCREENTIME",
    "rankings": [
      {
        "rank": 1,
        "userId": "user999",
        "totalDuration": 360000000,
        "appCount": 3
      },
      {
        "rank": 2,
        "userId": "user888",
        "totalDuration": 324000000,
        "appCount": 5
      },
      {
        "rank": 3,
        "userId": "user777",
        "totalDuration": 288000000,
        "appCount": 4
      }
    ],
    "userRank": {
      "rank": 15,
      "userId": "user123",
      "totalDuration": 144000000,
      "appCount": 2
    },
    "totalParticipants": 50
  },
  "message": "Challenge rankings retrieved successfully"
}
```

**Note:**
- `totalDuration` is in milliseconds
- `appCount` is the number of distinct apps tracked for that user
- `userRank` is only included if the authenticated user is participating in the challenge
- Rankings show top 10 players, but `userRank` shows the user's actual rank even if they're not in top 10

---

## Error Responses

All endpoints may return error responses in the following format:

```json
{
  "success": false,
  "status": 400,
  "data": null,
  "message": "Error message here",
  "error": {
    "code": "ERROR_CODE",
    "message": "Error message here",
    "details": null
  }
}
```

**Common Error Codes:**
- `400 Bad Request` - Invalid request parameters
- `401 Unauthorized` - Missing or invalid authentication token
- `404 Not Found` - Challenge not found
- `500 Internal Server Error` - Server error

---

## Complete Workflow Example

Here's a complete workflow example:

```bash
# 1. Get all active challenges
curl -X GET "http://localhost:8080/api/challenges/active"

# 2. Join a challenge (requires authentication)
curl -X POST "http://localhost:8080/api/challenges/join" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  -d '{"challengeId": 1}'

# 3. Submit app usage stats
curl -X POST "http://localhost:8080/api/challenges/stats" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  -d '{
    "challengeId": 1,
    "appName": "Instagram",
    "packageName": "com.instagram.android",
    "startSyncTime": "2024-01-20T10:00:00Z",
    "endSyncTime": "2024-01-20T10:30:00Z",
    "duration": 1800000
  }'

# 4. Check your rankings
curl -X GET "http://localhost:8080/api/challenges/1/rankings" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN"

# 5. Get challenge details
curl -X GET "http://localhost:8080/api/challenges/1" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN"

# 6. Get all your challenges
curl -X GET "http://localhost:8080/api/challenges/user" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN"
```

---

## Notes

1. **Time Format**: All timestamps use ISO 8601 format (e.g., `2024-01-20T10:30:00Z`)
2. **Duration**: All durations are in milliseconds
3. **Challenge Types**:
   - `LESS_SCREENTIME`: Lower total duration = better rank
   - `MORE_SCREENTIME`: Higher total duration = better rank
4. **Authentication**: Most endpoints require a valid JWT Bearer token
5. **Participant Stats**: The system tracks app usage with appName, packageName, startSyncTime, endSyncTime, and duration

