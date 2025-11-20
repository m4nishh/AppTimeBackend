# Focus History API - Time Filtering Examples

## Endpoint
```
GET /api/focus/history
```

## Query Parameters

### startDate (optional)
- **Format**: ISO 8601 timestamp
- **Example**: `2024-01-15T00:00:00Z`
- **Description**: Filter sessions from this time onwards

### endDate (optional)
- **Format**: ISO 8601 timestamp
- **Example**: `2024-01-15T23:59:59Z`
- **Description**: Filter sessions until this time

---

## Usage Examples

### 1. Get All Sessions (No Filter)
**URL:**
```
http://localhost:8080/api/focus/history
```

**cURL:**
```bash
curl -X GET 'http://localhost:8080/api/focus/history' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

---

### 2. Filter by Date Range
**URL:**
```
http://localhost:8080/api/focus/history?startDate=2024-01-01T00:00:00Z&endDate=2024-01-31T23:59:59Z
```

**cURL:**
```bash
curl -X GET 'http://localhost:8080/api/focus/history?startDate=2024-01-01T00:00:00Z&endDate=2024-01-31T23:59:59Z' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

**What it does:** Returns all sessions between January 1, 2024 00:00:00 and January 31, 2024 23:59:59

---

### 3. Filter from Start Date to Now
**URL:**
```
http://localhost:8080/api/focus/history?startDate=2024-01-15T00:00:00Z
```

**cURL:**
```bash
curl -X GET 'http://localhost:8080/api/focus/history?startDate=2024-01-15T00:00:00Z' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

**What it does:** Returns all sessions from January 15, 2024 00:00:00 until now

---

### 4. Filter from Beginning to End Date
**URL:**
```
http://localhost:8080/api/focus/history?endDate=2024-01-15T23:59:59Z
```

**cURL:**
```bash
curl -X GET 'http://localhost:8080/api/focus/history?endDate=2024-01-15T23:59:59Z' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

**What it does:** Returns all sessions from the beginning until January 15, 2024 23:59:59

---

### 5. Get Sessions for a Specific Day
**URL:**
```
http://localhost:8080/api/focus/history?startDate=2024-01-15T00:00:00Z&endDate=2024-01-15T23:59:59Z
```

**cURL:**
```bash
curl -X GET 'http://localhost:8080/api/focus/history?startDate=2024-01-15T00:00:00Z&endDate=2024-01-15T23:59:59Z' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

**What it does:** Returns all sessions on January 15, 2024 (entire day)

---

### 6. Get Sessions for Today
**URL (replace date with today's date):**
```
http://localhost:8080/api/focus/history?startDate=2024-01-15T00:00:00Z&endDate=2024-01-15T23:59:59Z
```

**cURL:**
```bash
curl -X GET 'http://localhost:8080/api/focus/history?startDate=2024-01-15T00:00:00Z&endDate=2024-01-15T23:59:59Z' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

---

### 7. Get Sessions for This Week
**URL:**
```
http://localhost:8080/api/focus/history?startDate=2024-01-08T00:00:00Z&endDate=2024-01-15T23:59:59Z
```

**cURL:**
```bash
curl -X GET 'http://localhost:8080/api/focus/history?startDate=2024-01-08T00:00:00Z&endDate=2024-01-15T23:59:59Z' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

---

## Time Format

All timestamps must be in **ISO 8601 format**:
- Format: `YYYY-MM-DDTHH:mm:ssZ`
- Example: `2024-01-15T10:30:00Z`
- The `Z` indicates UTC timezone

### Common Time Patterns:

| Pattern | Example |
|---------|---------|
| Start of day | `2024-01-15T00:00:00Z` |
| End of day | `2024-01-15T23:59:59Z` |
| Specific time | `2024-01-15T14:30:00Z` |
| Start of month | `2024-01-01T00:00:00Z` |
| End of month | `2024-01-31T23:59:59Z` |

---

## Response Format

All requests return the same format:

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
      }
    ],
    "totalFocusTime": 1800000,
    "averageSessionDuration": 1800000
  },
  "message": "Focus history retrieved successfully",
  "timestamp": "2024-01-15T16:00:00Z"
}
```

---

## Summary

- **No parameters**: Returns ALL sessions
- **startDate only**: Returns sessions from startDate to now
- **endDate only**: Returns sessions from beginning to endDate
- **Both parameters**: Returns sessions in the specified time range

