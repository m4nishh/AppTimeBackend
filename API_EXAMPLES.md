# API Examples - ScreenTime Backend

## Base URL
- **Local:** `http://localhost:8080`
- **Ngrok:** `https://your-ngrok-url.ngrok-free.app`

## Authentication
For all APIs except registration, include the Bearer token in the Authorization header:
```
Authorization: Bearer <encrypted_user_id>
```

---

## 1. Registration API

### Register Device/User
**Endpoint:** `POST /api/users/register`

**Request Body:**
```json
{
  "deviceInfo": {
    "deviceId": "android_device_unique_id_12345",
    "manufacturer": "Samsung",
    "model": "SM-G950F",
    "brand": "samsung",
    "product": "dreamlte",
    "device": "dreamlte",
    "hardware": "samsungexynos8895",
    "androidVersion": "13",
    "sdkVersion": 33
  }
}
```

**cURL Command:**
```bash
curl -X POST "http://localhost:8080/api/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceInfo": {
      "deviceId": "android_device_unique_id_12345",
      "manufacturer": "Samsung",
      "model": "SM-G950F",
      "brand": "samsung",
      "product": "dreamlte",
      "device": "dreamlte",
      "hardware": "samsungexynos8895",
      "androidVersion": "13",
      "sdkVersion": 33
    }
  }'
```

**With Ngrok (if deployed):**
```bash
curl -X POST "https://your-ngrok-url.ngrok-free.app/api/users/register" \
  -H "Content-Type: application/json" \
  -H "ngrok-skip-browser-warning: true" \
  -d '{
    "deviceInfo": {
      "deviceId": "android_device_unique_id_12345",
      "manufacturer": "Samsung",
      "model": "SM-G950F",
      "brand": "samsung",
      "product": "dreamlte",
      "device": "dreamlte",
      "hardware": "samsungexynos8895",
      "androidVersion": "13",
      "sdkVersion": 33
    }
  }'
```

**Response (Success - 201 Created):**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "username": "user_a1b2c3d4",
    "createdAt": "2024-01-15T10:30:00Z",
    "totpSecret": null,
    "totpEnabled": false
  },
  "message": "Device registered successfully",
  "timestamp": "2024-01-15T10:30:00Z",
  "error": null
}
```

**Note:** The `userId` in the response is the encrypted UserId generated from the deviceId. **Save this userId - it's your Bearer token for all other APIs!**

---

## 2. Get User Profile (Requires Authentication)

### Get User Profile
**Endpoint:** `GET /api/v1/user/profile`

**cURL Command:**
```bash
# Replace <ENCRYPTED_USER_ID> with the userId from registration response
curl -X GET "http://localhost:8080/api/v1/user/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ENCRYPTED_USER_ID>"
```

**Note:** Sync time is always included in the response.

**Response (Success - 200 OK):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "username": "user_a1b2c3d4",
    "email": null,
    "name": null,
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z",
    "lastSyncTime": 1705312200000
  },
  "message": "Profile retrieved successfully",
  "timestamp": "2024-01-15T10:30:00Z",
  "error": null
}
```

---

## 3. Update User Profile (Requires Authentication)

### Update User Profile - Username Only
**Endpoint:** `PUT /api/v1/user/profile`

**Note:** Currently, only the username can be updated. Other fields (email, name) cannot be changed.

**Request Body:**
```json
{
  "username": "new_username_here"
}
```

**cURL Command:**
```bash
curl -X PUT "http://localhost:8080/api/v1/user/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ENCRYPTED_USER_ID>" \
  -d '{
    "username": "my_new_username"
  }'
```

**Response (Success - 200 OK):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "username": "my_new_username",
    "email": null,
    "name": null,
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:35:00Z",
    "lastSyncTime": 1705312200000
  },
  "message": "Profile updated successfully",
  "timestamp": "2024-01-15T10:35:00Z",
  "error": null
}
```

**Error Response (Username Already Taken - 400 Bad Request):**
```json
{
  "success": false,
  "status": 400,
  "data": null,
  "message": "Username already taken",
  "timestamp": "2024-01-15T10:35:00Z",
  "error": {
    "code": null,
    "message": "Username already taken"
  }
}
```

---

## Error Responses

### 401 Unauthorized (Missing/Invalid Token)
```json
{
  "success": false,
  "status": 401,
  "data": null,
  "message": "Authentication required. Please provide a valid Bearer token.",
  "timestamp": "2024-01-15T10:30:00Z",
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Invalid or missing Bearer token"
  }
}
```


### 404 Not Found
```json
{
  "success": false,
  "status": 404,
  "data": null,
  "message": "User not found",
  "timestamp": "2024-01-15T10:30:00Z",
  "error": null
}
```

---

## Quick Test Script

Save this as `test_registration.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"
# BASE_URL="https://your-ngrok-url.ngrok-free.app"  # Uncomment for ngrok

echo "=== Registering Device ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/api/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceInfo": {
      "deviceId": "test_device_'$(date +%s)'",
      "manufacturer": "Samsung",
      "model": "SM-G950F",
      "brand": "samsung",
      "product": "dreamlte",
      "device": "dreamlte",
      "hardware": "samsungexynos8895",
      "androidVersion": "13",
      "sdkVersion": 33
    }
  }')

echo "$RESPONSE" | jq '.'

# Extract userId from response (requires jq)
USER_ID=$(echo "$RESPONSE" | jq -r '.data.userId')

if [ "$USER_ID" != "null" ] && [ -n "$USER_ID" ]; then
  echo ""
  echo "=== Getting User Profile ==="
  curl -s -X GET "$BASE_URL/api/v1/user/profile" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $USER_ID" | jq '.'
else
  echo "Registration failed!"
fi
```

Make it executable:
```bash
chmod +x test_registration.sh
./test_registration.sh
```

