# Focus API - cURL Examples

## Base URL
```
http://localhost:8080
```

## Test User ID
```
61a7126f-e1d9-58dd-b79e-333928e83f03
```
Use this as the Bearer token in all authenticated requests.

## Endpoints

### 1. POST /api/focus/submit
**Submit focus sessions (single or array)** (Requires Bearer token)

**Single Session Request:**
```json
{
  "focusDuration": 1800000,
  "startTime": "2024-01-15T10:00:00Z",
  "endTime": "2024-01-15T10:30:00Z",
  "sessionType": "work"
}
```

**Array of Sessions Request:**
```json
{
  "sessions": [
    {
      "focusDuration": 1800000,
      "startTime": "2024-01-15T10:00:00Z",
      "endTime": "2024-01-15T10:30:00Z",
      "sessionType": "work"
    },
    {
      "focusDuration": 3600000,
      "startTime": "2024-01-15T14:00:00Z",
      "endTime": "2024-01-15T15:00:00Z",
      "sessionType": "study"
    }
  ]
}
```

**Single Session cURL:**
```bash
curl --location --request POST 'http://localhost:8080/api/focus/submit' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_USER_ID_HERE' \
--data '{
  "focusDuration": 1800000,
  "startTime": "2024-01-15T10:00:00Z",
  "endTime": "2024-01-15T10:30:00Z",
  "sessionType": "work"
}'
```

**Array of Sessions cURL:**
```bash
curl --location --request POST 'http://localhost:8080/api/focus/submit' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_USER_ID_HERE' \
--data '{
  "sessions": [
    {
      "focusDuration": 1800000,
      "startTime": "2024-01-15T10:00:00Z",
      "endTime": "2024-01-15T10:30:00Z",
      "sessionType": "work"
    },
    {
      "focusDuration": 3600000,
      "startTime": "2024-01-15T14:00:00Z",
      "endTime": "2024-01-15T15:00:00Z",
      "sessionType": "study"
    }
  ]
}'
```

**Complete Example:**
```bash
curl --location --request POST 'http://localhost:8080/api/focus/submit' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03' \
--data '{
  "focusDuration": 3600000,
  "startTime": "2024-01-15T14:00:00Z",
  "endTime": "2024-01-15T15:00:00Z",
  "sessionType": "study"
}'
```

**One-liner:**
```bash
curl -X POST 'http://localhost:8080/api/focus/submit' \
-H 'Content-Type: application/json' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03' \
-d '{"focusDuration":1800000,"startTime":"2024-01-15T10:00:00Z","endTime":"2024-01-15T10:30:00Z","sessionType":"work"}'
```

**Single Session Response (201 Created):**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "submittedSessions": [
      {
        "id": 1,
        "focusDuration": 1800000,
        "startTime": "2024-01-15T10:00:00Z",
        "endTime": "2024-01-15T10:30:00Z",
        "sessionType": "work",
        "createdAt": "2024-01-15T10:30:05Z"
      }
    ],
    "totalSubmitted": 1,
    "totalFocusTime": 1800000
  },
  "message": "Focus session submitted successfully",
  "timestamp": "2024-01-15T10:30:05Z"
}
```

**Array Response (201 Created):**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "submittedSessions": [
      {
        "id": 1,
        "focusDuration": 1800000,
        "startTime": "2024-01-15T10:00:00Z",
        "endTime": "2024-01-15T10:30:00Z",
        "sessionType": "work",
        "createdAt": "2024-01-15T10:30:05Z"
      },
      {
        "id": 2,
        "focusDuration": 3600000,
        "startTime": "2024-01-15T14:00:00Z",
        "endTime": "2024-01-15T15:00:00Z",
        "sessionType": "study",
        "createdAt": "2024-01-15T15:00:05Z"
      }
    ],
    "totalSubmitted": 2,
    "totalFocusTime": 5400000
  },
  "message": "Focus sessions submitted successfully",
  "timestamp": "2024-01-15T15:00:05Z"
}
```

---

### 2. GET /api/focus/history
**Get focus history for authenticated user** (Requires Bearer token)

**URL Format:**
```
GET /api/focus/history?startDate=<ISO_8601>&endDate=<ISO_8601>
```

**Query Parameters (optional):**
- `startDate`: ISO 8601 format timestamp (e.g., `2024-01-01T00:00:00Z`) - Filters sessions from this time
- `endDate`: ISO 8601 format timestamp (e.g., `2024-01-31T23:59:59Z`) - Filters sessions until this time
- **If no dates provided, returns ALL sessions**

**Examples:**

**1. Get All Sessions (no time filter):**
```bash
curl -X GET 'http://localhost:8080/api/focus/history' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

**2. Filter by Date Range (both startDate and endDate):**
```bash
curl -X GET 'http://localhost:8080/api/focus/history?startDate=2024-01-01T00:00:00Z&endDate=2024-01-31T23:59:59Z' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

**3. Filter from Start Date to Now (only startDate):**
```bash
curl -X GET 'http://localhost:8080/api/focus/history?startDate=2024-01-15T00:00:00Z' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

**4. Filter from Beginning to End Date (only endDate):**
```bash
curl -X GET 'http://localhost:8080/api/focus/history?endDate=2024-01-15T23:59:59Z' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

**5. Get Sessions for a Specific Day:**
```bash
curl -X GET 'http://localhost:8080/api/focus/history?startDate=2024-01-15T00:00:00Z&endDate=2024-01-15T23:59:59Z' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

**6. Get Sessions for Today:**
```bash
# Today's start (00:00:00)
curl -X GET 'http://localhost:8080/api/focus/history?startDate=2024-01-15T00:00:00Z&endDate=2024-01-15T23:59:59Z' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

**7. Get Sessions for This Week:**
```bash
curl -X GET 'http://localhost:8080/api/focus/history?startDate=2024-01-08T00:00:00Z&endDate=2024-01-15T23:59:59Z' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

**Response (200 OK):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "focusSessions": [
      {
        "id": 1,
        "focusDuration": 1800000,
        "startTime": "2024-01-15T10:00:00Z",
        "endTime": "2024-01-15T10:30:00Z",
        "sessionType": "work",
        "createdAt": "2024-01-15T10:30:05Z"
      },
      {
        "id": 2,
        "focusDuration": 3600000,
        "startTime": "2024-01-15T14:00:00Z",
        "endTime": "2024-01-15T15:00:00Z",
        "sessionType": "study",
        "createdAt": "2024-01-15T15:00:05Z"
      }
    ],
    "totalFocusTime": 5400000,
    "averageSessionDuration": 2700000
  },
  "message": "Focus history retrieved successfully",
  "timestamp": "2024-01-15T16:00:00Z"
}
```

---

### 3. GET /api/focus/stats
**Get focus statistics for authenticated user** (Requires Bearer token)

**Example:**
```bash
curl --location --request GET 'http://localhost:8080/api/focus/stats' \
--header 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

**Response (200 OK):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "totalFocusTime": 10800000,
    "todayFocusTime": 5400000,
    "weeklyFocusTime": 7200000,
    "monthlyFocusTime": 9000000,
    "averageSessionDuration": 2700000,
    "totalSessions": 4
  },
  "message": "Focus stats retrieved successfully",
  "timestamp": "2024-01-15T16:00:00Z"
}
```

---

## Field Descriptions

### Request Fields (POST /api/focus/submit):

- **`focusDuration`** (Long, required): Duration in milliseconds
  - 30 minutes = `1800000`
  - 1 hour = `3600000`
  - 15 minutes = `900000`

- **`startTime`** (String, required): ISO 8601 format
  - Example: `"2024-01-15T10:00:00Z"`

- **`endTime`** (String, required): ISO 8601 format
  - Example: `"2024-01-15T10:30:00Z"`

- **`sessionType`** (String, optional): Type of session
  - Values: `"work"`, `"study"`, `"break"`, or any custom string
  - Can be omitted

---

## Duration Conversion Reference

| Duration | Milliseconds | JSON Value |
|----------|-------------|------------|
| 5 minutes | 300,000 | `300000` |
| 10 minutes | 600,000 | `600000` |
| 15 minutes | 900,000 | `900000` |
| 20 minutes | 1,200,000 | `1200000` |
| 25 minutes | 1,500,000 | `1500000` |
| 30 minutes | 1,800,000 | `1800000` |
| 45 minutes | 2,700,000 | `2700000` |
| 1 hour | 3,600,000 | `3600000` |
| 2 hours | 7,200,000 | `7200000` |
| 3 hours | 10,800,000 | `10800000` |

---

## Complete Flow Example

### Step 1: Register a device (to get userId)
```bash
curl --location --request POST 'http://localhost:8080/api/users/register' \
--header 'Content-Type: application/json' \
--data '{
  "deviceInfo": {
    "deviceId": "unique-device-id-12345",
    "manufacturer": "Samsung",
    "model": "Galaxy S21"
  }
}'
```

### Step 2: Submit a focus session
```bash
curl --location --request POST 'http://localhost:8080/api/focus/submit' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03' \
--data '{
  "focusDuration": 1800000,
  "startTime": "2024-01-15T10:00:00Z",
  "endTime": "2024-01-15T10:30:00Z",
  "sessionType": "work"
}'
```

### Step 3: Get focus history
```bash
curl --location --request GET 'http://localhost:8080/api/focus/history' \
--header 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

### Step 4: Get focus stats
```bash
curl --location --request GET 'http://localhost:8080/api/focus/stats' \
--header 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

---

## Notes

1. **Authentication**: All endpoints require Bearer token (userId) in the Authorization header.

2. **Leaderboard Integration**: When you submit a focus session, it automatically updates the leaderboard stats for:
   - Daily period
   - Weekly period
   - Monthly period

3. **Time Format**: Use ISO 8601 format for timestamps. The `Z` suffix indicates UTC timezone.

4. **Duration Validation**: The `focusDuration` should match the difference between `endTime` and `startTime` (in milliseconds).

5. **Session Types**: Common values are `"work"`, `"study"`, `"break"`, but you can use any custom string.

6. **Error Responses**: 
   - `400 Bad Request`: Invalid request (missing fields, invalid timestamps, duration mismatch, etc.)
   - `401 Unauthorized`: Missing or invalid Bearer token
   - `500 Internal Server Error`: Server error

---

## Example JSON Requests

### Work Session (30 minutes):
```json
{
  "focusDuration": 1800000,
  "startTime": "2024-01-15T09:00:00Z",
  "endTime": "2024-01-15T09:30:00Z",
  "sessionType": "work"
}
```

### Study Session (1 hour):
```json
{
  "focusDuration": 3600000,
  "startTime": "2024-01-15T14:00:00Z",
  "endTime": "2024-01-15T15:00:00Z",
  "sessionType": "study"
}
```

### Break Session (15 minutes):
```json
{
  "focusDuration": 900000,
  "startTime": "2024-01-15T12:00:00Z",
  "endTime": "2024-01-15T12:15:00Z",
  "sessionType": "break"
}
```

