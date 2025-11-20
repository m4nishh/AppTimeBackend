# Focus Data Submission - JSON Format

## Request JSON Format

### POST /api/focus/submit
**Submit a focus session** (Requires Bearer token)

### JSON Request Body:

```json
{
  "focusDuration": 1800000,
  "startTime": "2024-01-15T10:00:00Z",
  "endTime": "2024-01-15T10:30:00Z",
  "sessionType": "work"
}
```

### Field Descriptions:

- **`focusDuration`** (Long, required): Duration of the focus session in **milliseconds**
  - Example: `1800000` = 30 minutes (30 * 60 * 1000)
  - Example: `3600000` = 1 hour (60 * 60 * 1000)
  - Example: `900000` = 15 minutes (15 * 60 * 1000)

- **`startTime`** (String, required): Start time of the focus session in **ISO 8601 format**
  - Format: `YYYY-MM-DDTHH:mm:ssZ` or `YYYY-MM-DDTHH:mm:ss+00:00`
  - Example: `"2024-01-15T10:00:00Z"`
  - Example: `"2024-01-15T14:30:00+05:30"` (with timezone)

- **`endTime`** (String, required): End time of the focus session in **ISO 8601 format**
  - Format: `YYYY-MM-DDTHH:mm:ssZ` or `YYYY-MM-DDTHH:mm:ss+00:00`
  - Example: `"2024-01-15T10:30:00Z"`
  - Example: `"2024-01-15T15:00:00+05:30"` (with timezone)

- **`sessionType`** (String, optional): Type of focus session
  - Allowed values: `"work"`, `"study"`, `"break"`, or any custom string
  - Can be `null` or omitted

---

## Example JSON Requests

### Example 1: Work Session (30 minutes)
```json
{
  "focusDuration": 1800000,
  "startTime": "2024-01-15T09:00:00Z",
  "endTime": "2024-01-15T09:30:00Z",
  "sessionType": "work"
}
```

### Example 2: Study Session (1 hour)
```json
{
  "focusDuration": 3600000,
  "startTime": "2024-01-15T14:00:00Z",
  "endTime": "2024-01-15T15:00:00Z",
  "sessionType": "study"
}
```

### Example 3: Break Session (15 minutes)
```json
{
  "focusDuration": 900000,
  "startTime": "2024-01-15T12:00:00Z",
  "endTime": "2024-01-15T12:15:00Z",
  "sessionType": "break"
}
```

### Example 4: Custom Session Type (2 hours)
```json
{
  "focusDuration": 7200000,
  "startTime": "2024-01-15T08:00:00Z",
  "endTime": "2024-01-15T10:00:00Z",
  "sessionType": "deep-work"
}
```

### Example 5: Without Session Type
```json
{
  "focusDuration": 2700000,
  "startTime": "2024-01-15T16:00:00Z",
  "endTime": "2024-01-15T16:45:00Z"
}
```

---

## cURL Examples

### Basic POST Request:
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

### Complete Example:
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

### One-liner:
```bash
curl -X POST 'http://localhost:8080/api/focus/submit' \
-H 'Content-Type: application/json' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03' \
-d '{"focusDuration":1800000,"startTime":"2024-01-15T10:00:00Z","endTime":"2024-01-15T10:30:00Z","sessionType":"work"}'
```

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

**Formula:** `milliseconds = minutes × 60 × 1000` or `hours × 60 × 60 × 1000`

---

## Expected Response

### Success Response (201 Created):
```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": 1,
    "focusDuration": 1800000,
    "startTime": "2024-01-15T10:00:00Z",
    "endTime": "2024-01-15T10:30:00Z",
    "sessionType": "work",
    "createdAt": "2024-01-15T10:30:05Z"
  },
  "message": "Focus session submitted successfully",
  "timestamp": "2024-01-15T10:30:05Z"
}
```

### Error Response (400 Bad Request):
```json
{
  "success": false,
  "status": 400,
  "data": null,
  "message": "Invalid request: focusDuration is required",
  "error": {
    "code": "BAD_REQUEST",
    "message": "Invalid request: focusDuration is required"
  },
  "timestamp": "2024-01-15T10:30:05Z"
}
```

---

## Notes

1. **Time Format**: Use ISO 8601 format for timestamps. The `Z` suffix indicates UTC timezone.

2. **Duration Calculation**: The `focusDuration` should match the difference between `endTime` and `startTime` (in milliseconds).

3. **Session Types**: Common values are `"work"`, `"study"`, `"break"`, but you can use any custom string.

4. **Authentication**: Requires Bearer token (userId) in the Authorization header.

5. **Validation**: 
   - `focusDuration` must be a positive number
   - `startTime` and `endTime` must be valid ISO 8601 timestamps
   - `endTime` should be after `startTime`

