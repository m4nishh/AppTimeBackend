# TOTP Control Panel API Documentation

## Overview

The TOTP Control Panel provides a comprehensive set of endpoints for users to manage their TOTP (Time-based One-Time Password) functionality. This includes enabling/disabling TOTP, managing secrets, and controlling who has access to their data through TOTP verification sessions.

**Base Path:** `/api/v1/user/totp/control/`

**Authentication:** All endpoints require Bearer token authentication.

---

## Table of Contents

1. [Get TOTP Status](#1-get-totp-status)
2. [Enable TOTP](#2-enable-totp)
3. [Disable TOTP](#3-disable-totp)
4. [Regenerate TOTP Secret](#4-regenerate-totp-secret)
5. [Get Active Sessions](#5-get-active-sessions)
6. [Revoke Specific Session](#6-revoke-specific-session)
7. [Revoke All Sessions](#7-revoke-all-sessions)

---

## 1. Get TOTP Status

Get the current TOTP status for the authenticated user.

### Endpoint
```
GET /api/v1/user/totp/control/status
```

### Headers
```
Authorization: Bearer <token>
```

### Response

**Success (200 OK)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "isEnabled": true,
    "hasSecret": true,
    "message": "TOTP is enabled and configured"
  },
  "message": "TOTP status retrieved successfully",
  "timestamp": "2024-01-15T10:00:00Z",
  "error": null
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `isEnabled` | Boolean | Whether TOTP is currently enabled |
| `hasSecret` | Boolean | Whether a TOTP secret exists (even if disabled) |
| `message` | String | Human-readable status message |

### Status Messages

- `"TOTP is enabled and configured"` - TOTP is active and ready to use
- `"TOTP secret exists but is disabled"` - Secret exists but TOTP is disabled
- `"TOTP is not configured"` - No secret exists, TOTP needs to be set up

### Example Request

```bash
curl -X GET "https://api.example.com/api/v1/user/totp/control/status" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 2. Enable TOTP

Enable TOTP for the authenticated user. If no secret exists, a new one will be generated.

### Endpoint
```
POST /api/v1/user/totp/control/enable
```

### Headers
```
Authorization: Bearer <token>
Content-Type: application/json
```

### Response

**Success (201 Created)**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "success": true,
    "message": "TOTP enabled successfully. Please save the secret and configure your authenticator app.",
    "secret": "JBSWY3DPEHPK3PXP",
    "qrCodeUrl": "otpauth://totp/AppTime:username?secret=JBSWY3DPEHPK3PXP&issuer=AppTime"
  },
  "message": "TOTP enabled successfully",
  "timestamp": "2024-01-15T10:00:00Z",
  "error": null
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `success` | Boolean | Operation success status |
| `message` | String | Human-readable message |
| `secret` | String | Base32-encoded TOTP secret (save this securely) |
| `qrCodeUrl` | String | QR code URL for authenticator app setup |

### Important Notes

- **Save the secret immediately**: The secret is only returned once when enabling TOTP
- **QR Code**: Use the `qrCodeUrl` to generate a QR code for easy setup in authenticator apps
- **Secret Format**: The secret is Base32-encoded and compatible with Google Authenticator, Microsoft Authenticator, Authy, etc.

### Example Request

```bash
curl -X POST "https://api.example.com/api/v1/user/totp/control/enable" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

### QR Code Generation

You can generate a QR code from the `qrCodeUrl` using any QR code library:

```javascript
// Example: Generate QR code using qrcode library
import QRCode from 'qrcode';

const qrCodeUrl = response.data.qrCodeUrl;
const qrCodeDataURL = await QRCode.toDataURL(qrCodeUrl);
```

---

## 3. Disable TOTP

Disable TOTP for the authenticated user. This will revoke all active verification sessions.

### Endpoint
```
POST /api/v1/user/totp/control/disable
```

### Headers
```
Authorization: Bearer <token>
Content-Type: application/json
```

### Response

**Success (200 OK)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "success": true,
    "message": "TOTP disabled successfully."
  },
  "message": "TOTP disabled successfully",
  "timestamp": "2024-01-15T10:00:00Z",
  "error": null
}
```

### Important Notes

- **Sessions Revoked**: All active TOTP verification sessions are automatically revoked
- **Secret Preserved**: The secret is not deleted, only disabled (can be re-enabled)
- **Re-enabling**: You can re-enable TOTP without generating a new secret if one exists

### Example Request

```bash
curl -X POST "https://api.example.com/api/v1/user/totp/control/disable" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

---

## 4. Regenerate TOTP Secret

Regenerate the TOTP secret for the authenticated user. This invalidates the old secret and revokes all active sessions.

### Endpoint
```
POST /api/v1/user/totp/control/regenerate
```

### Headers
```
Authorization: Bearer <token>
Content-Type: application/json
```

### Response

**Success (201 Created)**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "success": true,
    "message": "TOTP secret regenerated successfully. Please update your authenticator app with the new secret. All active sessions have been revoked.",
    "secret": "NEWSECRET123456"
  },
  "message": "TOTP secret regenerated successfully",
  "timestamp": "2024-01-15T10:00:00Z",
  "error": null
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `success` | Boolean | Operation success status |
| `message` | String | Human-readable message |
| `secret` | String | New Base32-encoded TOTP secret |

### Important Notes

- **Old Secret Invalidated**: The previous secret will no longer work
- **Sessions Revoked**: All active verification sessions are automatically revoked
- **Update Authenticator**: Users must update their authenticator app with the new secret
- **Use Case**: Use this if the secret is compromised or lost

### Example Request

```bash
curl -X POST "https://api.example.com/api/v1/user/totp/control/regenerate" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

---

## 5. Get Active Sessions

Get all active TOTP verification sessions for the authenticated user. Shows which users have access to this user's data.

### Endpoint
```
GET /api/v1/user/totp/control/sessions
```

### Headers
```
Authorization: Bearer <token>
```

### Response

**Success (200 OK)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "sessions": [
      {
        "sessionId": 123,
        "requestingUserId": "user-a-id",
        "requestingUsername": "user_a",
        "verifiedAt": "2024-01-15T10:00:00Z",
        "expiresAt": "2024-01-15T11:00:00Z",
        "remainingSeconds": 3600,
        "remainingMinutes": 60
      },
      {
        "sessionId": 124,
        "requestingUserId": "user-b-id",
        "requestingUsername": "user_b",
        "verifiedAt": "2024-01-15T09:30:00Z",
        "expiresAt": "2024-01-15T10:30:00Z",
        "remainingSeconds": 1800,
        "remainingMinutes": 30
      }
    ],
    "totalCount": 2
  },
  "message": "Active TOTP sessions retrieved successfully",
  "timestamp": "2024-01-15T10:00:00Z",
  "error": null
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `sessions` | Array | List of active verification sessions |
| `totalCount` | Integer | Total number of active sessions |

### Session Object Fields

| Field | Type | Description |
|-------|------|-------------|
| `sessionId` | Long | Unique session identifier (use for revocation) |
| `requestingUserId` | String | ID of the user who has access |
| `requestingUsername` | String | Username of the user who has access (nullable) |
| `verifiedAt` | String | ISO 8601 timestamp when TOTP was verified |
| `expiresAt` | String | ISO 8601 timestamp when access expires |
| `remainingSeconds` | Integer | Seconds until session expires |
| `remainingMinutes` | Integer | Minutes until session expires |

### Example Request

```bash
curl -X GET "https://api.example.com/api/v1/user/totp/control/sessions" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 6. Revoke Specific Session

Revoke a specific TOTP verification session by session ID.

### Endpoint
```
DELETE /api/v1/user/totp/control/sessions/{sessionId}
```

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `sessionId` | Long | The session ID to revoke (from Get Active Sessions) |

### Headers
```
Authorization: Bearer <token>
```

### Response

**Success (200 OK)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "success": true,
    "message": "TOTP verification session revoked successfully",
    "revokedCount": 1
  },
  "message": "Session revoked successfully",
  "timestamp": "2024-01-15T10:00:00Z",
  "error": null
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `success` | Boolean | Operation success status |
| `message` | String | Human-readable message |
| `revokedCount` | Integer | Number of sessions revoked (should be 1) |

### Error Responses

**404 Not Found / 400 Bad Request**
```json
{
  "success": false,
  "status": 400,
  "data": null,
  "message": "Session not found or already expired",
  "timestamp": "2024-01-15T10:00:00Z",
  "error": {
    "code": null,
    "message": "Session not found or already expired"
  }
}
```

### Example Request

```bash
curl -X DELETE "https://api.example.com/api/v1/user/totp/control/sessions/123" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 7. Revoke All Sessions

Revoke all active TOTP verification sessions for the authenticated user.

### Endpoint
```
DELETE /api/v1/user/totp/control/sessions
```

### Headers
```
Authorization: Bearer <token>
```

### Response

**Success (200 OK)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "success": true,
    "message": "All active TOTP verification sessions revoked successfully",
    "revokedCount": 3
  },
  "message": "All sessions revoked successfully",
  "timestamp": "2024-01-15T10:00:00Z",
  "error": null
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `success` | Boolean | Operation success status |
| `message` | String | Human-readable message |
| `revokedCount` | Integer | Number of sessions revoked |

### Important Notes

- **Immediate Effect**: All sessions are revoked immediately
- **No Recovery**: Revoked sessions cannot be restored
- **Use Case**: Use this if you suspect unauthorized access or want to reset all access

### Example Request

```bash
curl -X DELETE "https://api.example.com/api/v1/user/totp/control/sessions" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request
```json
{
  "success": false,
  "status": 400,
  "data": null,
  "message": "Invalid request",
  "timestamp": "2024-01-15T10:00:00Z",
  "error": {
    "code": null,
    "message": "Invalid request"
  }
}
```

### 401 Unauthorized
```json
{
  "success": false,
  "status": 401,
  "data": null,
  "message": "Authentication required. Please provide a valid Bearer token.",
  "timestamp": "2024-01-15T10:00:00Z",
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Invalid or missing Bearer token"
  }
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "status": 500,
  "data": null,
  "message": "Failed to perform operation: <error details>",
  "timestamp": "2024-01-15T10:00:00Z",
  "error": {
    "code": null,
    "message": "Failed to perform operation: <error details>"
  }
}
```

---

## Use Cases

### 1. Initial TOTP Setup

```bash
# 1. Check current status
GET /api/v1/user/totp/control/status

# 2. Enable TOTP (if not enabled)
POST /api/v1/user/totp/control/enable

# 3. Save the secret and generate QR code
# 4. Scan QR code with authenticator app
# 5. Verify TOTP code works
```

### 2. View Who Has Access

```bash
# Get all active sessions
GET /api/v1/user/totp/control/sessions

# Review the list of users with access
# Revoke specific sessions if needed
DELETE /api/v1/user/totp/control/sessions/{sessionId}
```

### 3. Revoke All Access

```bash
# Revoke all active sessions
DELETE /api/v1/user/totp/control/sessions
```

### 4. Regenerate Secret (Lost/Compromised)

```bash
# Regenerate secret
POST /api/v1/user/totp/control/regenerate

# Update authenticator app with new secret
# Old secret will no longer work
```

### 5. Temporarily Disable TOTP

```bash
# Disable TOTP (sessions are revoked)
POST /api/v1/user/totp/control/disable

# Later, re-enable (uses existing secret)
POST /api/v1/user/totp/control/enable
```

---

## Security Considerations

### Secret Management

- **Never log secrets**: TOTP secrets should never appear in logs
- **One-time display**: Secrets are only returned when enabling/regenerating
- **Secure storage**: If storing secrets client-side, use secure storage (Keychain, Keystore, etc.)
- **QR Code**: Use HTTPS when generating QR codes from the `qrCodeUrl`

### Session Management

- **Automatic expiration**: Sessions expire based on the duration set during verification
- **Immediate revocation**: Revoked sessions are invalidated immediately
- **No recovery**: Once revoked, sessions cannot be restored

### Best Practices

1. **Regular audits**: Periodically check active sessions and revoke unnecessary ones
2. **Secret rotation**: Regenerate secrets if compromised or lost
3. **Disable when not needed**: Disable TOTP if not actively using it
4. **Monitor access**: Regularly review who has access to your data

---

## Integration Examples

### JavaScript/TypeScript

```typescript
class TOTPControlPanel {
  private baseUrl: string;
  private token: string;

  constructor(baseUrl: string, token: string) {
    this.baseUrl = baseUrl;
    this.token = token;
  }

  async getStatus() {
    const response = await fetch(`${this.baseUrl}/api/v1/user/totp/control/status`, {
      headers: {
        'Authorization': `Bearer ${this.token}`
      }
    });
    return response.json();
  }

  async enableTOTP() {
    const response = await fetch(`${this.baseUrl}/api/v1/user/totp/control/enable`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      }
    });
    return response.json();
  }

  async getActiveSessions() {
    const response = await fetch(`${this.baseUrl}/api/v1/user/totp/control/sessions`, {
      headers: {
        'Authorization': `Bearer ${this.token}`
      }
    });
    return response.json();
  }

  async revokeSession(sessionId: number) {
    const response = await fetch(`${this.baseUrl}/api/v1/user/totp/control/sessions/${sessionId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${this.token}`
      }
    });
    return response.json();
  }
}
```

### Kotlin/Android

```kotlin
class TOTPControlPanel(private val apiClient: ApiClient) {
    
    suspend fun getStatus(): TOTPStatusResponse {
        return apiClient.get("/api/v1/user/totp/control/status")
    }
    
    suspend fun enableTOTP(): TOTPEnableResponse {
        return apiClient.post("/api/v1/user/totp/control/enable")
    }
    
    suspend fun getActiveSessions(): TOTPActiveSessionsResponse {
        return apiClient.get("/api/v1/user/totp/control/sessions")
    }
    
    suspend fun revokeSession(sessionId: Long): TOTPRevokeSessionResponse {
        return apiClient.delete("/api/v1/user/totp/control/sessions/$sessionId")
    }
}
```

---

## Related Endpoints

### TOTP Generation
- `GET /api/v1/user/totp/generate` - Generate TOTP code for authenticated user
- `GET /api/users/{username}/totp/generate` - Generate TOTP code by username

### TOTP Verification
- `POST /api/users/{username}/totp/verify` - Verify TOTP code and create session
- `GET /api/users/{username}/totp/status` - Check TOTP access status

### TOTP Sessions (Debug)
- `GET /api/v1/user/totp/sessions` - Get sessions where you have access (debug endpoint)

---

## FAQ

### Q: What happens if I lose my TOTP secret?
**A:** You can regenerate a new secret using the regenerate endpoint. The old secret will be invalidated.

### Q: Can I have multiple TOTP secrets?
**A:** No, each user has one TOTP secret. Regenerating replaces the old one.

### Q: What happens to active sessions when I disable TOTP?
**A:** All active sessions are automatically revoked when you disable TOTP.

### Q: Can I see who has access to my data?
**A:** Yes, use the Get Active Sessions endpoint to see all users with active access.

### Q: How long do verification sessions last?
**A:** Session duration is set when the TOTP is verified (default: 1 hour, max: 24 hours).

### Q: Can I revoke a specific user's access?
**A:** Yes, get the session ID from Get Active Sessions, then use Revoke Specific Session.

---

## Support

For issues or questions, please contact support or refer to the main API documentation.

---

**Last Updated:** 2024-01-15  
**API Version:** 1.0


