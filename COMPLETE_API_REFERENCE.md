# Complete API Reference

**Base URL:** `https://344cdbb499c9.ngrok-free.app` (or your server URL)

**Note:** For ngrok requests, include header: `ngrok-skip-browser-warning: true`

---

## Table of Contents

1. [Health & System](#health--system)
2. [User Management](#user-management)
3. [Challenges](#challenges)
4. [App Usage Events](#app-usage-events)
5. [Focus Mode](#focus-mode)
6. [Leaderboard](#leaderboard)
7. [Consents](#consents)

---

## Health & System

### GET /
**Description:** Root endpoint - check if server is running  
**Auth:** Not required

```bash
curl "https://344cdbb499c9.ngrok-free.app/"
```

### GET /health
**Description:** Health check endpoint  
**Auth:** Not required

```bash
curl "https://344cdbb499c9.ngrok-free.app/health"
```

---

## User Management

### POST /api/users/register
**Description:** Register a new device/user  
**Auth:** Not required  
**Body:**
```json
{
  "deviceInfo": {
    "deviceId": "device123",
    "manufacturer": "Google",
    "model": "Pixel 7",
    "brand": "google",
    "product": "product_name",
    "device": "device_name",
    "hardware": "hardware_name",
    "androidVersion": "14",
    "sdkVersion": 34
  },
  "firebaseToken": "your-firebase-token-here" // Optional
}
```

### GET /api/users/search
**Description:** Search users by name  
**Auth:** Required  
**Query Params:** `q` (required, min 2 characters)

```bash
curl -H "Authorization: Bearer TOKEN" \
  "https://344cdbb499c9.ngrok-free.app/api/users/search?q=john"
```

### GET /api/users/{username}/totp/generate
**Description:** Generate TOTP code for a user by username  
**Auth:** Not required

```bash
curl "https://344cdbb499c9.ngrok-free.app/api/users/john_doe/totp/generate"
```

### POST /api/users/{username}/totp/verify
**Description:** Verify TOTP code for a user by username  
**Auth:** Required  
**Body:**
```json
{
  "code": "123456"
}
```

### POST /api/users/{username}/profile
**Description:** Get user profile after TOTP verification  
**Auth:** Required (needs valid TOTP verification session)

### GET /api/v1/user/profile
**Description:** Get authenticated user's profile  
**Auth:** Required

```bash
curl -H "Authorization: Bearer TOKEN" \
  "https://344cdbb499c9.ngrok-free.app/api/v1/user/profile"
```

### PUT /api/v1/user/profile
**Description:** Update authenticated user's profile (username and/or firebaseToken)  
**Auth:** Required  
**Body:**
```json
{
  "username": "newusername", // Optional
  "firebaseToken": "new-firebase-token" // Optional
}
```

### POST /api/v1/user/username
**Description:** Change username for authenticated user  
**Auth:** Required  
**Body:**
```json
{
  "username": "newusername"
}
```

### GET /api/v1/user/totp/generate
**Description:** Generate TOTP code for authenticated user  
**Auth:** Required

### GET /api/v1/user/totp/sessions
**Description:** Get all active TOTP verification sessions for authenticated user  
**Auth:** Required

### GET /api/v1/user/sync/status
**Description:** Get sync status for authenticated user  
**Auth:** Required

---

## Challenges

### GET /api/challenges/active
**Description:** Get all active challenges  
**Auth:** Optional (if authenticated, includes `hasJoined` flag)

```bash
curl "https://344cdbb499c9.ngrok-free.app/api/challenges/active"
```

### POST /api/challenges/join
**Description:** Join a challenge  
**Auth:** Required  
**Body:**
```json
{
  "challengeId": 1
}
```

### GET /api/challenges/user
**Description:** Get all challenges for authenticated user (including past ones)  
**Auth:** Required

### GET /api/challenges/{challengeId}
**Description:** Get challenge details including participant count  
**Auth:** Required

### POST /api/challenges/stats
**Description:** Submit challenge participant stats (single)  
**Auth:** Required  
**Body:**
```json
{
  "challengeId": 1,
  "appName": "Instagram",
  "packageName": "com.instagram.android",
  "startSyncTime": "2024-01-15T10:00:00Z",
  "endSyncTime": "2024-01-15T10:30:00Z",
  "duration": 1800000
}
```

### POST /api/challenges/stats/batch
**Description:** Submit multiple challenge participant stats  
**Auth:** Required  
**Body:**
```json
{
  "challengeId": 1,
  "stats": [
    {
      "challengeId": 1,
      "appName": "Instagram",
      "packageName": "com.instagram.android",
      "startSyncTime": "2024-01-15T10:00:00Z",
      "endSyncTime": "2024-01-15T10:30:00Z",
      "duration": 1800000
    }
  ]
}
```

### GET /api/challenges/{challengeId}/rankings
**Description:** Get challenge rankings (top 10 players)  
**Auth:** Required

### GET /api/challenges/{challengeId}/last-sync
**Description:** Get last sync time for authenticated user in a specific challenge  
**Auth:** Required

---

## App Usage Events

### POST /api/usage/events
**Description:** Submit a single app usage event  
**Auth:** Required  
**Body:**
```json
{
  "syncTime": "2024-01-15T10:00:00Z",
  "event": {
    "packageName": "com.instagram.android",
    "appName": "Instagram",
    "eventType": "FOREGROUND",
    "eventTimestamp": "2024-01-15T10:00:00Z"
  }
}
```

### POST /api/usage/events/batch
**Description:** Submit multiple app usage events (max 100 per batch)  
**Auth:** Required  
**Body:**
```json
{
  "syncTime": "2024-01-15T10:00:00Z",
  "events": [
    {
      "packageName": "com.instagram.android",
      "appName": "Instagram",
      "eventType": "FOREGROUND",
      "eventTimestamp": "2024-01-15T10:00:00Z"
    }
  ]
}
```

### GET /api/usage/stats/users
**Description:** Get all userIds that have data in the database  
**Auth:** Not required

### GET /api/usage/stats/daily
**Description:** Get daily usage stats for a user  
**Auth:** Required  
**Query Params:** 
- `date` (required) - YYYY-MM-DD format
- `targetUserId` (optional) - User ID or username (requires TOTP verification)

```bash
curl -H "Authorization: Bearer TOKEN" \
  "https://344cdbb499c9.ngrok-free.app/api/usage/stats/daily?date=2024-01-15"
```

### GET /api/usage/stats/debug
**Description:** Get debug information about user's app usage data  
**Auth:** Required  
**Query Params:** `date` (optional) - YYYY-MM-DD format

### GET /api/usage/stats/raw
**Description:** Get all raw events from database for authenticated user  
**Auth:** Required

### GET /api/usage/stats/last-sync
**Description:** Get the last sync time for app usage stats  
**Auth:** Required

---

## Focus Mode

### POST /api/focus/submit
**Description:** Submit focus sessions (single or array)  
**Auth:** Required  
**Body (Single):**
```json
{
  "focusDuration": 3600000,
  "startTime": "2024-01-15T10:00:00Z",
  "endTime": "2024-01-15T11:00:00Z",
  "sessionType": "DEEP_WORK"
}
```

**Body (Array):**
```json
{
  "sessions": [
    {
      "focusDuration": 3600000,
      "startTime": "2024-01-15T10:00:00Z",
      "endTime": "2024-01-15T11:00:00Z",
      "sessionType": "DEEP_WORK"
    }
  ]
}
```

### GET /api/focus/history
**Description:** Get focus history for authenticated user  
**Auth:** Required  
**Query Params:**
- `startDate` (optional) - ISO 8601 format
- `endDate` (optional) - ISO 8601 format

### GET /api/focus/stats
**Description:** Get focus statistics for authenticated user  
**Auth:** Required

### POST /api/focus-mode-stats
**Description:** Submit focus mode stats  
**Auth:** Required  
**Body:**
```json
{
  "startTime": 1234567890000,
  "endTime": 1234567895000
}
```

### GET /api/focus-mode-stats
**Description:** Get focus mode stats with optional time-based filtering  
**Auth:** Required  
**Query Params:**
- `startTimeMs` (optional) - Filter stats with startTime >= this value (milliseconds)
- `endTimeMs` (optional) - Filter stats with endTime <= this value (milliseconds)

---

## Leaderboard

### GET /api/leaderboard/daily
**Description:** Get daily leaderboard  
**Auth:** Optional (if authenticated, includes userRank)  
**Query Params:** `date` (optional) - YYYY-MM-DD format, defaults to today

```bash
curl "https://344cdbb499c9.ngrok-free.app/api/leaderboard/daily?date=2024-01-15"
```

### GET /api/leaderboard/weekly
**Description:** Get weekly leaderboard  
**Auth:** Optional (if authenticated, includes userRank)  
**Query Params:** `weekDate` (optional) - YYYY-WW format, defaults to current week

```bash
curl "https://344cdbb499c9.ngrok-free.app/api/leaderboard/weekly?weekDate=2024-03"
```

---

## Consents

### GET /api/consents
**Description:** Get all available consent templates  
**Auth:** Not required

```bash
curl "https://344cdbb499c9.ngrok-free.app/api/consents"
```

### GET /api/consents/user
**Description:** Get authenticated user's submitted consents  
**Auth:** Required

### POST /api/consents
**Description:** Submit user consents  
**Auth:** Required  
**Body:**
```json
{
  "consents": [
    {
      "templateId": 1,
      "accepted": true
    }
  ]
}
```

---

## Authentication

Most endpoints require Bearer token authentication. Include the token in the Authorization header:

```
Authorization: Bearer YOUR_AUTH_TOKEN
```

The token is returned when you register a device via `POST /api/users/register` (the `userId` field is your auth token).

---

## Response Format

All API responses follow this format:

```json
{
  "success": true,
  "status": 200,
  "data": { ... },
  "message": "Success message",
  "timestamp": "2024-01-15T10:00:00Z",
  "error": null
}
```

Error responses:

```json
{
  "success": false,
  "status": 400,
  "data": null,
  "message": "Error message",
  "timestamp": "2024-01-15T10:00:00Z",
  "error": {
    "code": "ERROR_CODE",
    "message": "Error message",
    "details": null
  }
}
```

---

## Common HTTP Status Codes

- `200 OK` - Success
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request parameters
- `401 Unauthorized` - Missing or invalid authentication token
- `403 Forbidden` - Valid token but insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

---

## Notes

1. **Time Format:** All timestamps use ISO 8601 format (e.g., `2024-01-15T10:00:00Z`)
2. **Duration:** All durations are in milliseconds
3. **Challenge Types:**
   - `LESS_SCREENTIME`: Lower total duration = better rank
   - `MORE_SCREENTIME`: Higher total duration = better rank
4. **Challenge Display Types:** `SPECIAL`, `TRENDING`, `QUICK_JOIN`, `FEATURE`
5. **Challenge Tags:** `browser`, `study`, `gaming`, `social media`, `wellness`, `productivity`, `learning`

