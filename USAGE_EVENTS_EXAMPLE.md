# Usage Events API - Request/Response Examples

## Important Note

The `duration` field in your request object is **not part of the API model**. The API calculates usage time automatically from `MOVE_TO_FOREGROUND` and `MOVE_TO_BACKGROUND` event pairs.

## Correct Request Format

### POST /api/usage/events/batch

**Your request object (with duration - will be ignored):**
```json
{
  "packageName": "com.nothing.camera",
  "appName": "Camera",
  "isSystemApp": false,
  "eventType": "MOVE_TO_BACKGROUND",
  "eventTimestamp": "2025-11-17T01:52:57.500Z",
  "duration": 1291  // ⚠️ This field is NOT accepted by the API
}
```

**Correct format for batch endpoint:**
```json
{
  "syncTime": "2025-11-17T01:52:57.500Z",
  "events": [
    {
      "packageName": "com.nothing.camera",
      "appName": "Camera",
      "isSystemApp": false,
      "eventType": "MOVE_TO_BACKGROUND",
      "eventTimestamp": "2025-11-17T01:52:57.500Z"
    }
  ]
}
```

**cURL Command:**
```bash
curl -X POST 'https://ec3bedfc01c5.ngrok-free.app/api/usage/events/batch' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer YOUR_USER_ID' \
  -d '{
    "syncTime": "2025-11-17T01:52:57.500Z",
    "events": [
      {
        "packageName": "com.nothing.camera",
        "appName": "Camera",
        "isSystemApp": false,
        "eventType": "MOVE_TO_BACKGROUND",
        "eventTimestamp": "2025-11-17T01:52:57.500Z"
      }
    ]
  }'
```

## Batch Submission Response

**Response:**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "events": [
      {
        "id": 123,
        "userId": "user_a2fd3076",
        "packageName": "com.nothing.camera",
        "appName": "Camera",
        "isSystemApp": false,
        "eventType": "MOVE_TO_BACKGROUND",
        "eventTimestamp": "2025-11-17T01:52:57.500Z",
        "createdAt": "2025-11-17T01:53:00.123Z"
      }
    ],
    "count": 1
  },
  "message": "Batch events submitted successfully",
  "timestamp": "2025-11-17T01:53:00.123Z",
  "error": null
}
```

## Daily Stats Response

**After submitting the event, query daily stats:**

```bash
curl -X GET 'https://ec3bedfc01c5.ngrok-free.app/api/usage/stats/daily?date=2025-11-17&targetUserId=user_a2fd3076' \
  -H 'Authorization: Bearer YOUR_USER_ID'
```

**Expected Response:**

⚠️ **Important:** If you only submit a `MOVE_TO_BACKGROUND` event without a corresponding `MOVE_TO_FOREGROUND` event, the usage time will be **0** because the system needs both events to calculate duration.

**Response (if only MOVE_TO_BACKGROUND event exists):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "stats": [
      {
        "packageName": "com.nothing.camera",
        "appName": "Camera",
        "usageTime": 0,
        "duration": 0,
        "isSystemApp": false,
        "category": null,
        "totalScreenTime": 0,
        "lastUsed": "2025-11-17T01:52:57.500Z",
        "openedAt": "2025-11-17T01:52:57.500Z"
      }
    ],
    "session": {
      "requestingUserId": "...",
      "targetUserId": "user_a2fd3076",
      "targetUsername": "...",
      "verifiedAt": "...",
      "expiresAt": "...",
      "remainingSeconds": 3600,
      "remainingMinutes": 60,
      "isValid": true
    }
  },
  "message": "Daily usage stats retrieved successfully. Session expires in 60 minutes."
}
```

## To Get Usage Time, Submit Both Events

**Complete example with both events:**

```json
{
  "syncTime": "2025-11-17T01:52:57.500Z",
  "events": [
    {
      "packageName": "com.nothing.camera",
      "appName": "Camera",
      "isSystemApp": false,
      "eventType": "MOVE_TO_FOREGROUND",
      "eventTimestamp": "2025-11-17T01:50:00.000Z"
    },
    {
      "packageName": "com.nothing.camera",
      "appName": "Camera",
      "isSystemApp": false,
      "eventType": "MOVE_TO_BACKGROUND",
      "eventTimestamp": "2025-11-17T01:52:57.500Z"
    }
  ]
}
```

**This will calculate usage time:**
- Start: `2025-11-17T01:50:00.000Z`
- End: `2025-11-17T01:52:57.500Z`
- Duration: ~2 minutes 57.5 seconds = **177500 milliseconds**

**Daily Stats Response (with both events):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "stats": [
      {
        "packageName": "com.nothing.camera",
        "appName": "Camera",
        "usageTime": 177500,
        "duration": 177500,
        "isSystemApp": false,
        "category": null,
        "totalScreenTime": 177500,
        "lastUsed": "2025-11-17T01:52:57.500Z",
        "openedAt": "2025-11-17T01:50:00.000Z"
      }
    ],
    "session": { ... }
  },
  "message": "Daily usage stats retrieved successfully. Session expires in 60 minutes."
}
```

## Get Last Sync Time

### GET /api/usage/stats/last-sync

Get the last sync time for app usage stats. Returns the most recent `eventTimestamp` from the `app_usage_events` table for the authenticated user.

**cURL Command:**
```bash
curl -X GET 'https://7d76ddc7b74f.ngrok-free.app/api/usage/stats/last-sync' \
  -H 'Authorization: Bearer YOUR_USER_ID' \
  -H 'Accept: application/json'
```

**Response (when user has events):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "userId": "b002e76b-baa5-4bea-9d97-958f5405beba",
    "lastSyncTime": "2025-11-21T20:17:53.961Z",
    "hasEvents": true
  },
  "message": "Last sync time retrieved successfully",
  "timestamp": "2025-11-21T20:18:00.123Z",
  "error": null
}
```

**Response (when user has no events):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "userId": "b002e76b-baa5-4bea-9d97-958f5405beba",
    "lastSyncTime": null,
    "hasEvents": false
  },
  "message": "Last sync time retrieved successfully",
  "timestamp": "2025-11-21T20:18:00.123Z",
  "error": null
}
```

**Note:** This endpoint requires authentication. The `lastSyncTime` is the most recent `eventTimestamp` from all app usage events submitted by the user.

## Summary

1. **Remove `duration` field** - It's not part of the API model
2. **Wrap in batch format** - Use `syncTime` at top level and `events` array
3. **Submit both events** - Need `MOVE_TO_FOREGROUND` + `MOVE_TO_BACKGROUND` to calculate usage time
4. **Usage time is calculated** - Automatically from event timestamps, not from a duration field
5. **Get last sync time** - Use `/api/usage/stats/last-sync` to get the most recent event timestamp

