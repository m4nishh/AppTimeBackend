# Consents API - cURL Examples

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

### 1. GET /api/consents
**Get all available consent templates** (Public - No authentication required)

```bash
curl --location --request GET 'http://localhost:8080/api/consents'
```

**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": 1,
      "name": "Data Sharing",
      "description": "Allow sharing of usage data for analytics",
      "isMandatory": false
    },
    {
      "id": 2,
      "name": "Analytics",
      "description": "Allow collection of analytics data",
      "isMandatory": false
    }
  ],
  "message": "Consent templates retrieved successfully",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

### 2. GET /api/consents/user
**Get authenticated user's submitted consents** (Requires Bearer token)

```bash
curl --location --request GET 'http://localhost:8080/api/consents/user' \
--header 'Authorization: Bearer YOUR_USER_ID_HERE'
```

**Example:**
```bash
curl --location --request GET 'http://localhost:8080/api/consents/user' \
--header 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": 1,
      "consentId": 1,
      "consentName": "Data Sharing",
      "value": "accepted",
      "submittedAt": "2024-01-15T10:30:00Z"
    },
    {
      "id": 2,
      "consentId": 2,
      "consentName": "Analytics",
      "value": "rejected",
      "submittedAt": "2024-01-15T10:31:00Z"
    }
  ],
  "message": "User consents retrieved successfully",
  "timestamp": "2024-01-15T10:35:00Z"
}
```

---

### 3. POST /api/consents
**Submit user consents** (Requires Bearer token)

**Basic Example:**
```bash
curl --location --request POST 'http://localhost:8080/api/consents' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_USER_ID_HERE' \
--data '{
  "consents": [
    {
      "id": 1,
      "value": "accepted"
    },
    {
      "id": 2,
      "value": "rejected"
    }
  ]
}'
```

**Complete Example (with all seeded consent templates):**
```bash
curl --location --request POST 'http://localhost:8080/api/consents' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03' \
--data '{
  "consents": [
    {
      "id": 1,
      "value": "accepted"
    },
    {
      "id": 2,
      "value": "accepted"
    },
    {
      "id": 3,
      "value": "rejected"
    },
    {
      "id": 4,
      "value": "accepted"
    },
    {
      "id": 5,
      "value": "accepted"
    }
  ]
}'
```

**Single Consent Example:**
```bash
curl --location --request POST 'http://localhost:8080/api/consents' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03' \
--data '{
  "consents": [
    {
      "id": 1,
      "value": "accepted"
    }
  ]
}'
```

**One-liner (compact format):**
```bash
curl -X POST 'http://localhost:8080/api/consents' \
-H 'Content-Type: application/json' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03' \
-d '{"consents":[{"id":1,"value":"accepted"},{"id":2,"value":"rejected"}]}'
```

**Response:**
```json
{
  "success": true,
  "status": 201,
  "data": [
    {
      "id": 1,
      "consentId": 1,
      "consentName": "Data Sharing",
      "value": "accepted",
      "submittedAt": "2024-01-15T10:30:00Z"
    },
    {
      "id": 2,
      "consentId": 2,
      "consentName": "Analytics",
      "value": "rejected",
      "submittedAt": "2024-01-15T10:30:00Z"
    }
  ],
  "message": "Consents submitted successfully",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## Notes

1. **Authentication**: The Bearer token is the `userId` (encrypted device ID) that you receive from the registration endpoint.

2. **Consent Values**: The `value` field must be either `"accepted"` or `"rejected"`.

3. **Consent ID**: The `id` in the request body refers to the `consentId` from the consent templates. After seeding, the IDs are:
   - `1` = Data Sharing
   - `2` = Analytics
   - `3` = Marketing Communications
   - `4` = Leaderboard Participation
   - `5` = Terms of Service (mandatory)

4. **Update Behavior**: If you submit a consent that already exists for the user, it will be updated with the new value.

5. **Multiple Consents**: You can submit multiple consents in a single request. All consents in the array will be processed.

6. **Error Responses**: 
   - `400 Bad Request`: Invalid request (missing fields, invalid values, consent template not found, etc.)
   - `401 Unauthorized`: Missing or invalid Bearer token
   - `500 Internal Server Error`: Server error

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

### Step 2: Get available consent templates
```bash
curl --location --request GET 'http://localhost:8080/api/consents'
```

### Step 3: Submit consents (use userId from Step 1 as Bearer token)
```bash
curl --location --request POST 'http://localhost:8080/api/consents' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03' \
--data '{
  "consents": [
    {
      "id": 1,
      "value": "accepted"
    },
    {
      "id": 2,
      "value": "accepted"
    },
    {
      "id": 5,
      "value": "accepted"
    }
  ]
}'
```

### Step 4: Get user's submitted consents
```bash
curl --location --request GET 'http://localhost:8080/api/consents/user' \
--header 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

