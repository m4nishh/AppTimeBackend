# TOTP Control Panel Documentation

## Overview

The TOTP Control Panel allows users to manage who has access to their profile, location, and stats data. Users can view all active access sessions, revoke access, whitelist users for permanent access, and extend access time for existing sessions.

## Features

1. **View Accessors** - See all users who currently have access to your data (via TOTP or whitelist)
2. **Revoke Access** - Immediately revoke access for any user
3. **Whitelist Management** - Add or remove users from whitelist (permanent access without TOTP)
4. **Extend Access Time** - Increase the access duration for active TOTP sessions

## Access Types

### TOTP Sessions
- Temporary access granted after verifying a TOTP code
- Has an expiration time (default: 1 hour, max: 24 hours)
- Can be extended up to 7 days
- Can be revoked at any time

### Whitelist
- Permanent access without requiring TOTP verification
- No expiration time
- Users on whitelist can access data anytime
- Can be removed from whitelist at any time

## API Endpoints

All endpoints require authentication via Bearer token and are available under `/api/v1/user/totp/`.

### 1. Get Control Panel Overview

**Endpoint:** `GET /api/v1/user/totp/control-panel`

**Description:** Get a complete overview of all users who have access to your data, including both TOTP sessions and whitelisted users.

**Authentication:** Required

**Response:**
```json
{
  "success": true,
  "message": "Control panel data retrieved successfully",
  "data": {
    "activeSessions": [
      {
        "requestingUserId": "user-123",
        "requestingUsername": "john_doe",
        "verifiedAt": "2024-01-15T10:00:00Z",
        "expiresAt": "2024-01-15T11:00:00Z",
        "remainingSeconds": 3600,
        "accessType": "TOTP"
      },
      {
        "requestingUserId": "user-456",
        "requestingUsername": "jane_smith",
        "verifiedAt": "2024-01-15T09:00:00Z",
        "expiresAt": null,
        "remainingSeconds": null,
        "accessType": "WHITELIST"
      }
    ],
    "whitelistedUsers": [
      {
        "userId": "user-456",
        "username": "jane_smith",
        "whitelistedAt": "2024-01-15T09:00:00Z",
        "createdAt": "2024-01-15T09:00:00Z"
      }
    ]
  }
}
```

**Response Fields:**
- `activeSessions`: List of all users with current access
  - `requestingUserId`: ID of the user who has access
  - `requestingUsername`: Username of the user who has access
  - `verifiedAt`: When access was granted (ISO 8601 timestamp)
  - `expiresAt`: When access expires (ISO 8601 timestamp, null for whitelist)
  - `remainingSeconds`: Seconds until expiration (null for whitelist)
  - `accessType`: Either "TOTP" or "WHITELIST"
- `whitelistedUsers`: List of all whitelisted users

---

### 2. Revoke Access

**Endpoint:** `POST /api/v1/user/totp/revoke-access`

**Description:** Revoke access for a specific user by invalidating their TOTP session. This only works for TOTP sessions, not whitelisted users.

**Authentication:** Required

**Request Body:**
```json
{
  "requestingUserId": "user-123"
}
```

**Request Fields:**
- `requestingUserId` (required): ID of the user whose access should be revoked

**Response:**
```json
{
  "success": true,
  "message": "Access revoked successfully",
  "data": {
    "success": true,
    "message": "Access revoked successfully"
  }
}
```

**Error Responses:**
- `400 Bad Request`: Invalid request (missing requestingUserId)
- `404 Not Found`: No active session found to revoke
- `500 Internal Server Error`: Server error

**Note:** To remove a whitelisted user, use the remove whitelist endpoint instead.

---

### 3. Add User to Whitelist

**Endpoint:** `POST /api/v1/user/totp/whitelist/add`

**Description:** Add a user to your whitelist, granting them permanent access to your data without requiring TOTP verification.

**Authentication:** Required

**Request Body:**
```json
{
  "username": "jane_smith"
}
```

**Request Fields:**
- `username` (required): Username of the user to whitelist

**Response:**
```json
{
  "success": true,
  "message": "User added to whitelist successfully",
  "data": {
    "success": true,
    "message": "User added to whitelist successfully"
  }
}
```

**Error Responses:**
- `400 Bad Request`: Invalid request (missing username, user not found, or attempting to whitelist yourself)
- `500 Internal Server Error`: Server error

**Notes:**
- You cannot whitelist yourself
- If the user is already whitelisted, the entry will be updated
- Whitelisted users have permanent access until removed

---

### 4. Remove User from Whitelist

**Endpoint:** `POST /api/v1/user/totp/whitelist/remove`

**Description:** Remove a user from your whitelist, revoking their permanent access.

**Authentication:** Required

**Request Body:**
```json
{
  "username": "jane_smith"
}
```

**Request Fields:**
- `username` (required): Username of the user to remove from whitelist

**Response:**
```json
{
  "success": true,
  "message": "User removed from whitelist successfully",
  "data": {
    "success": true,
    "message": "User removed from whitelist successfully"
  }
}
```

**Error Responses:**
- `400 Bad Request`: Invalid request (missing username or user not found)
- `404 Not Found`: User not found in whitelist
- `500 Internal Server Error`: Server error

---

### 5. Extend Access Time

**Endpoint:** `POST /api/v1/user/totp/extend-access`

**Description:** Extend the access time for an active TOTP session. This only works for TOTP sessions, not whitelisted users.

**Authentication:** Required

**Request Body:**
```json
{
  "requestingUserId": "user-123",
  "additionalSeconds": 3600
}
```

**Request Fields:**
- `requestingUserId` (required): ID of the user whose access should be extended
- `additionalSeconds` (required): Additional seconds to add to current expiration (must be > 0, max: 604800 = 7 days)

**Response:**
```json
{
  "success": true,
  "message": "Access time extended successfully",
  "data": {
    "success": true,
    "message": "Access time extended successfully",
    "newExpiresAt": "2024-01-15T12:00:00Z",
    "remainingSeconds": 7200
  }
}
```

**Response Fields:**
- `success`: Whether the operation was successful
- `message`: Human-readable message
- `newExpiresAt`: New expiration time (ISO 8601 timestamp)
- `remainingSeconds`: Total remaining seconds after extension

**Error Responses:**
- `400 Bad Request`: Invalid request (missing fields, invalid additionalSeconds)
- `404 Not Found`: No active session found to extend
- `500 Internal Server Error`: Server error

**Notes:**
- Maximum extension is 7 days (604800 seconds)
- If you request more than 7 days, it will be capped at 7 days
- Only works for active TOTP sessions (not whitelisted users)

---

## Usage Examples

### Example 1: View All Accessors

```bash
curl -X GET "https://api.example.com/api/v1/user/totp/control-panel" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Example 2: Revoke Access

```bash
curl -X POST "https://api.example.com/api/v1/user/totp/revoke-access" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "requestingUserId": "user-123"
  }'
```

### Example 3: Add User to Whitelist

```bash
curl -X POST "https://api.example.com/api/v1/user/totp/whitelist/add" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane_smith"
  }'
```

### Example 4: Remove User from Whitelist

```bash
curl -X POST "https://api.example.com/api/v1/user/totp/whitelist/remove" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane_smith"
  }'
```

### Example 5: Extend Access Time

```bash
curl -X POST "https://api.example.com/api/v1/user/totp/extend-access" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "requestingUserId": "user-123",
    "additionalSeconds": 7200
  }'
```

---

## Data Models

### TOTPAccessorInfo
```kotlin
data class TOTPAccessorInfo(
    val requestingUserId: String,
    val requestingUsername: String?,
    val verifiedAt: String, // ISO 8601 timestamp
    val expiresAt: String?, // ISO 8601 timestamp (null for whitelist)
    val remainingSeconds: Int?, // null for whitelist (infinite access)
    val accessType: String // "TOTP" or "WHITELIST"
)
```

### WhitelistedUserInfo
```kotlin
data class WhitelistedUserInfo(
    val userId: String,
    val username: String?,
    val whitelistedAt: String, // ISO 8601 timestamp
    val createdAt: String // ISO 8601 timestamp
)
```

### TOTPControlPanelResponse
```kotlin
data class TOTPControlPanelResponse(
    val activeSessions: List<TOTPAccessorInfo>,
    val whitelistedUsers: List<WhitelistedUserInfo>
)
```

### RevokeAccessRequest
```kotlin
data class RevokeAccessRequest(
    val requestingUserId: String
)
```

### AddWhitelistRequest
```kotlin
data class AddWhitelistRequest(
    val username: String
)
```

### RemoveWhitelistRequest
```kotlin
data class RemoveWhitelistRequest(
    val username: String
)
```

### ExtendAccessRequest
```kotlin
data class ExtendAccessRequest(
    val requestingUserId: String,
    val additionalSeconds: Int
)
```

### ExtendAccessResponse
```kotlin
data class ExtendAccessResponse(
    val success: Boolean,
    val message: String,
    val newExpiresAt: String?, // ISO 8601 timestamp
    val remainingSeconds: Int?
)
```

---

## How It Works

### Access Check Flow

When a user tries to access another user's data (profile, location, stats), the system checks access in this order:

1. **Whitelist Check**: First checks if the requesting user is whitelisted by the target user
   - If whitelisted → Access granted (permanent)
   - If not whitelisted → Continue to step 2

2. **TOTP Session Check**: Checks if there's an active TOTP verification session
   - If valid session exists → Access granted (temporary, expires at session expiration)
   - If no valid session → Access denied

### Session Management

- **TOTP Sessions**: Created when a user successfully verifies a TOTP code
  - Default duration: 1 hour (3600 seconds)
  - Minimum duration: 1 minute (60 seconds)
  - Maximum duration: 24 hours (86400 seconds)
  - Can be extended up to 7 days total

- **Whitelist**: Permanent access entries
  - No expiration
  - Must be explicitly removed
  - Takes precedence over TOTP sessions

### Revocation

- **Revoking TOTP Access**: Invalidates the session by setting expiration to current time
- **Removing from Whitelist**: Deletes the whitelist entry
- Both operations are immediate and take effect on the next access check

---

## Best Practices

1. **Regular Review**: Periodically review your control panel to see who has access
2. **Whitelist Trusted Users**: Use whitelist for family members or trusted contacts who need regular access
3. **Temporary Access**: Use TOTP sessions for one-time or temporary access needs
4. **Revoke Unused Access**: Revoke access for users who no longer need it
5. **Monitor Active Sessions**: Check expiration times and extend if needed

---

## Error Handling

All endpoints follow the standard API response format:

**Success Response:**
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... }
}
```

**Error Response:**
```json
{
  "success": false,
  "message": "Error description",
  "error": "ERROR_CODE"
}
```

**Common HTTP Status Codes:**
- `200 OK`: Success
- `400 Bad Request`: Invalid request parameters
- `401 Unauthorized`: Missing or invalid authentication token
- `403 Forbidden`: Access denied
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

---

## Security Considerations

1. **Authentication Required**: All endpoints require valid Bearer token authentication
2. **User Verification**: Users can only manage access to their own data
3. **Self-Protection**: Users cannot whitelist themselves
4. **Immediate Effect**: Revocation takes effect immediately
5. **Session Validation**: Expired sessions are automatically invalidated

---

## Database Schema

### TOTP Verification Sessions Table
Stores temporary TOTP verification sessions:
- `id`: Primary key
- `requesting_user_id`: User requesting access
- `target_user_id`: User whose data is being accessed
- `target_username`: Username of target user
- `verified_at`: When session was created
- `expires_at`: When session expires
- `created_at`: Record creation time

### TOTP Whitelist Table
Stores permanent whitelist entries:
- `id`: Primary key
- `target_user_id`: User whose data is being accessed
- `whitelisted_user_id`: User who has permanent access
- `whitelisted_username`: Username for easier lookup
- `created_at`: When whitelist entry was created
- `updated_at`: When whitelist entry was last updated

---

## Related Endpoints

- `GET /api/users/{username}/totp/generate` - Generate TOTP code
- `POST /api/users/{username}/totp/verify` - Verify TOTP code
- `GET /api/users/{username}/totp/status` - Check access status
- `POST /api/users/{username}/profile` - Get user profile (requires access)
- `GET /api/location/user/{username}/last` - Get user location (requires access)

---

## Support

For issues or questions, please contact the development team or refer to the main API documentation.

