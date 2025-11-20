# Focus Mode Stats API

## Overview
API endpoints for tracking focus mode statistics with start and end times in milliseconds.

## Table Structure
- **Table Name**: `focus_mode_stats`
- **Columns**:
  - `id` (Long, auto-increment, primary key)
  - `user_id` (String, indexed)
  - `start_time` (Long, milliseconds)
  - `end_time` (Long, milliseconds)
  - `created_at` (Timestamp)

## Endpoints

### 1. POST /api/focus-mode-stats
Submit focus mode stats (requires authentication)

**Request:**
```bash
curl -X POST 'https://your-url.ngrok-free.app/api/focus-mode-stats' \
  -H 'Authorization: Bearer YOUR_USER_ID' \
  -H 'Content-Type: application/json' \
  -d '{
    "startTime": 1734268800000,
    "endTime": 1734272400000
  }'
```

**Request Body:**
```json
{
  "startTime": 1734268800000,  // milliseconds (Unix timestamp)
  "endTime": 1734272400000     // milliseconds (Unix timestamp)
}
```

**Response:**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": 1,
    "userId": "user-id-here",
    "startTime": 1734268800000,
    "endTime": 1734272400000,
    "duration": 3600000,
    "createdAt": "2025-11-14T10:00:00Z"
  },
  "message": "Focus mode stats saved successfully"
}
```

**Validation:**
- `startTime` must be > 0
- `endTime` must be > 0
- `endTime` must be > `startTime`

---

### 2. GET /api/focus-mode-stats
Get focus mode stats with optional time-based filtering (requires authentication)

**Get All Stats:**
```bash
curl -X GET 'https://your-url.ngrok-free.app/api/focus-mode-stats' \
  -H 'Authorization: Bearer YOUR_USER_ID'
```

**Get Stats with Start Time Filter:**
```bash
curl -X GET 'https://your-url.ngrok-free.app/api/focus-mode-stats?startTimeMs=1734268800000' \
  -H 'Authorization: Bearer YOUR_USER_ID'
```

**Get Stats with End Time Filter:**
```bash
curl -X GET 'https://your-url.ngrok-free.app/api/focus-mode-stats?endTimeMs=1734272400000' \
  -H 'Authorization: Bearer YOUR_USER_ID'
```

**Get Stats with Time Range:**
```bash
curl -X GET 'https://your-url.ngrok-free.app/api/focus-mode-stats?startTimeMs=1734268800000&endTimeMs=1734272400000' \
  -H 'Authorization: Bearer YOUR_USER_ID'
```

**Query Parameters:**
- `startTimeMs` (optional): Filter stats with `startTime >= startTimeMs` (milliseconds)
- `endTimeMs` (optional): Filter stats with `endTime <= endTimeMs` (milliseconds)

**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "stats": [
      {
        "id": 1,
        "startTime": 1734268800000,
        "endTime": 1734272400000,
        "duration": 3600000,
        "createdAt": "2025-11-14T10:00:00Z"
      },
      {
        "id": 2,
        "startTime": 1734276000000,
        "endTime": 1734279600000,
        "duration": 3600000,
        "createdAt": "2025-11-14T12:00:00Z"
      }
    ],
    "totalCount": 2,
    "totalDuration": 7200000
  },
  "message": "All focus mode stats retrieved successfully"
}
```

**Response Fields:**
- `stats`: Array of focus mode stats items
- `totalCount`: Total number of stats returned
- `totalDuration`: Sum of all durations in milliseconds

---

## Examples

### Example 1: Submit Focus Mode Session
```bash
# Start time: 2025-11-14 10:00:00 UTC (1734268800000 ms)
# End time: 2025-11-14 11:00:00 UTC (1734272400000 ms)
# Duration: 1 hour (3600000 ms)

curl -X POST 'https://your-url.ngrok-free.app/api/focus-mode-stats' \
  -H 'Authorization: Bearer b002e76b-baa5-4bea-9d97-958f5405beba' \
  -H 'Content-Type: application/json' \
  -d '{
    "startTime": 1734268800000,
    "endTime": 1734272400000
  }'
```

### Example 2: Get All Stats for Today
```bash
# Get all stats from today (2025-11-14 00:00:00 UTC)
# Start of day: 1734220800000 ms

curl -X GET 'https://your-url.ngrok-free.app/api/focus-mode-stats?startTimeMs=1734220800000' \
  -H 'Authorization: Bearer b002e76b-baa5-4bea-9d97-958f5405beba'
```

### Example 3: Get Stats for Specific Time Range
```bash
# Get stats between 10:00 AM and 2:00 PM on 2025-11-14
# Start: 1734268800000 ms (10:00 AM)
# End: 1734282000000 ms (2:00 PM)

curl -X GET 'https://your-url.ngrok-free.app/api/focus-mode-stats?startTimeMs=1734268800000&endTimeMs=1734282000000' \
  -H 'Authorization: Bearer b002e76b-baa5-4bea-9d97-958f5405beba'
```

---

## Notes

1. **Time Format**: All times are in milliseconds (Unix timestamp)
2. **Authentication**: All endpoints require Bearer token authentication
3. **User ID**: Automatically extracted from the Bearer token
4. **Duration**: Automatically calculated as `endTime - startTime`
5. **Ordering**: GET results are ordered by `startTime` descending (newest first)
6. **Filtering**: 
   - `startTimeMs` filters: `startTime >= startTimeMs`
   - `endTimeMs` filters: `endTime <= endTimeMs`
   - Both filters can be used together for a time range

---

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "status": 400,
  "message": "endTime must be greater than startTime",
  "error": {
    "code": null,
    "message": "endTime must be greater than startTime"
  }
}
```

### 401 Unauthorized
```json
{
  "success": false,
  "status": 401,
  "message": "Authentication required. Please provide a valid Bearer token."
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "status": 500,
  "message": "Failed to save focus mode stats: [error details]"
}
```

