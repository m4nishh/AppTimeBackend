# TOTP API - cURL Examples

## Base URL
```
http://localhost:8080
```

## Important Notes
- **userId is NEVER exposed** in any API response
- All endpoints use **username** instead of userId
- User A needs User B's TOTP code to see User B's profile
- TOTP codes are valid for 60 seconds

---

## Complete Flow: User A wants to see User B's details

### Step 1: User A searches for User B
**GET /api/users/search**

```bash
curl -X GET 'http://localhost:8080/api/users/search?q=john'
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "username": "john_doe",
      "name": "John Doe",
      "email": "john@example.com",
      "createdAt": "2024-01-15T10:00:00Z",
      "isActive": true
    }
  ]
}
```
**Note:** No `userId` in response, only `username`

---

### Step 2: User B generates TOTP code on their app
**GET /api/users/{username}/totp/generate**

User B (john_doe) generates their TOTP code:
```bash
curl -X GET 'http://localhost:8080/api/users/john_doe/totp/generate'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "code": "123456",
    "remainingSeconds": 45,
    "expiresAt": "2024-01-15T10:30:45Z"
  }
}
```

User B shares this code with User A.

---

### Step 3: User A gets User B's profile using TOTP code
**POST /api/users/{username}/profile**

User A provides User B's username and TOTP code:
```bash
curl -X POST 'http://localhost:8080/api/users/john_doe/profile' \
-H 'Content-Type: application/json' \
-d '{
  "code": "123456"
}'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "username": "john_doe",
    "name": "John Doe",
    "email": "john@example.com",
    "createdAt": "2024-01-15T10:00:00Z",
    "updatedAt": "2024-01-15T10:05:00Z",
    "lastSyncTime": 1705312800000
  }
}
```
**Note:** No `userId` in response

---

## Endpoints

### 1. GET /api/users/{username}/totp/generate
**Generate TOTP code for a user by username** (Public - No authentication required)

**Example:**
```bash
curl -X GET 'http://localhost:8080/api/users/john_doe/totp/generate'
```

**Response (200 OK):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "code": "123456",
    "remainingSeconds": 45,
    "expiresAt": "2024-01-15T10:30:45Z"
  },
  "message": "TOTP code generated successfully"
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "status": 400,
  "message": "TOTP is not enabled for this user"
}
```

---

### 2. POST /api/users/{username}/totp/verify
**Verify TOTP code for a user by username** (Public - No authentication required)

**Request Body:**
```json
{
  "code": "123456"
}
```

**Example:**
```bash
curl -X POST 'http://localhost:8080/api/users/john_doe/totp/verify' \
-H 'Content-Type: application/json' \
-d '{
  "code": "123456"
}'
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "valid": true,
    "message": "TOTP code is valid"
  },
  "message": "TOTP code verified successfully"
}
```

**Invalid Code Response (400 Bad Request):**
```json
{
  "success": false,
  "status": 400,
  "message": "Invalid TOTP code"
}
```

---

### 3. POST /api/users/{username}/profile
**Get user profile by username after TOTP verification** (Public - No authentication required)

**Use Case:** User A needs User B's TOTP code to see User B's details

**Request Body:**
```json
{
  "code": "123456"
}
```

**Example:**
```bash
curl -X POST 'http://localhost:8080/api/users/john_doe/profile' \
-H 'Content-Type: application/json' \
-d '{
  "code": "123456"
}'
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "username": "john_doe",
    "name": "John Doe",
    "email": "john@example.com",
    "createdAt": "2024-01-15T10:00:00Z",
    "updatedAt": "2024-01-15T10:05:00Z",
    "lastSyncTime": 1705312800000
  },
  "message": "User profile retrieved successfully"
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "status": 400,
  "message": "Invalid TOTP code"
}
```

---

## Complete Flow Example

### Scenario: User A wants to see User B's profile

**Step 1: User A searches for User B**
```bash
curl -X GET 'http://localhost:8080/api/users/search?q=john'
```
**Result:** Gets `username: "john_doe"` (no userId)

**Step 2: User B generates TOTP on their app**
```bash
curl -X GET 'http://localhost:8080/api/users/john_doe/totp/generate'
```
**Result:** User B gets code `"123456"` and shares it with User A

**Step 3: User A uses User B's TOTP to get profile**
```bash
curl -X POST 'http://localhost:8080/api/users/john_doe/profile' \
-H 'Content-Type: application/json' \
-d '{"code":"123456"}'
```
**Result:** User A gets User B's profile (without userId)

---

## Security Features

✅ **userId Never Exposed**: All public APIs use username only
✅ **TOTP Protection**: User A needs User B's TOTP code to see User B's details
✅ **Secret Never Exposed**: TOTP secret is never returned in any API response
✅ **Time-based**: TOTP codes expire after 60 seconds
✅ **Clock Skew Tolerance**: Allows ±1 time window for validation

---

## Quick Reference

**Search Users:**
```bash
curl -X GET 'http://localhost:8080/api/users/search?q=john'
```

**Generate TOTP (User B's app):**
```bash
curl -X GET 'http://localhost:8080/api/users/john_doe/totp/generate'
```

**Get Profile with TOTP (User A):**
```bash
curl -X POST 'http://localhost:8080/api/users/john_doe/profile' \
-H 'Content-Type: application/json' \
-d '{"code":"123456"}'
```

**Verify TOTP:**
```bash
curl -X POST 'http://localhost:8080/api/users/john_doe/totp/verify' \
-H 'Content-Type: application/json' \
-d '{"code":"123456"}'
```
