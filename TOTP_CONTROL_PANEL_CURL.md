# TOTP Control Panel API - cURL Commands

## Base URL
Replace `https://api.example.com` with your actual API base URL.

Replace `YOUR_TOKEN` with your actual Bearer token.

---

## 1. Get TOTP Status

```bash
curl -X GET "https://api.example.com/api/v1/user/totp/control/status" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

---

## 2. Enable TOTP

```bash
curl -X POST "https://api.example.com/api/v1/user/totp/control/enable" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Response includes:**
- `secret`: Base32-encoded TOTP secret
- `qrCodeUrl`: QR code URL for authenticator app setup

---

## 3. Disable TOTP

```bash
curl -X POST "https://api.example.com/api/v1/user/totp/control/disable" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Note:** This automatically revokes all active verification sessions.

---

## 4. Regenerate TOTP Secret

```bash
curl -X POST "https://api.example.com/api/v1/user/totp/control/regenerate" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Note:** This invalidates the old secret and revokes all active sessions.

---

## 5. Get Active Sessions

```bash
curl -X GET "https://api.example.com/api/v1/user/totp/control/sessions" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Response includes:**
- List of all active sessions
- Session IDs, usernames, expiration times
- Remaining time for each session

---

## 6. Revoke Specific Session

Replace `{sessionId}` with the actual session ID from the Get Active Sessions response.

```bash
curl -X DELETE "https://api.example.com/api/v1/user/totp/control/sessions/123" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Example with session ID 123:**
```bash
curl -X DELETE "https://api.example.com/api/v1/user/totp/control/sessions/123" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

---

## 7. Revoke All Sessions

```bash
curl -X DELETE "https://api.example.com/api/v1/user/totp/control/sessions" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Note:** This revokes all active verification sessions immediately.

---

## Complete Workflow Examples

### Initial TOTP Setup

```bash
# Step 1: Check current status
curl -X GET "https://api.example.com/api/v1/user/totp/control/status" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Step 2: Enable TOTP (if not enabled)
curl -X POST "https://api.example.com/api/v1/user/totp/control/enable" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"

# Step 3: Save the secret from response and generate QR code
# Step 4: Scan QR code with authenticator app
```

### View and Manage Active Sessions

```bash
# Step 1: Get all active sessions
curl -X GET "https://api.example.com/api/v1/user/totp/control/sessions" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Step 2: Revoke a specific session (use sessionId from response)
curl -X DELETE "https://api.example.com/api/v1/user/totp/control/sessions/123" \
  -H "Authorization: Bearer YOUR_TOKEN"

# OR revoke all sessions
curl -X DELETE "https://api.example.com/api/v1/user/totp/control/sessions" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Regenerate Secret (Lost/Compromised)

```bash
# Regenerate secret
curl -X POST "https://api.example.com/api/v1/user/totp/control/regenerate" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"

# Update authenticator app with new secret from response
```

### Temporarily Disable TOTP

```bash
# Disable TOTP (sessions are automatically revoked)
curl -X POST "https://api.example.com/api/v1/user/totp/control/disable" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"

# Later, re-enable (uses existing secret if available)
curl -X POST "https://api.example.com/api/v1/user/totp/control/enable" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

---

## Pretty Print JSON Responses

Add `| jq` to format JSON responses (requires jq installed):

```bash
curl -X GET "https://api.example.com/api/v1/user/totp/control/status" \
  -H "Authorization: Bearer YOUR_TOKEN" | jq
```

---

## Save Responses to File

```bash
# Save response to file
curl -X GET "https://api.example.com/api/v1/user/totp/control/status" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o totp_status.json

# Save and pretty print
curl -X GET "https://api.example.com/api/v1/user/totp/control/status" \
  -H "Authorization: Bearer YOUR_TOKEN" | jq > totp_status.json
```

---

## Verbose Output (Debugging)

Add `-v` flag for verbose output:

```bash
curl -v -X GET "https://api.example.com/api/v1/user/totp/control/status" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## Environment Variables (Recommended)

Set your token and base URL as environment variables:

```bash
export API_BASE_URL="https://api.example.com"
export API_TOKEN="YOUR_TOKEN"
```

Then use in commands:

```bash
curl -X GET "${API_BASE_URL}/api/v1/user/totp/control/status" \
  -H "Authorization: Bearer ${API_TOKEN}"
```

---

## Quick Reference

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/v1/user/totp/control/status` | GET | Get TOTP status |
| `/api/v1/user/totp/control/enable` | POST | Enable TOTP |
| `/api/v1/user/totp/control/disable` | POST | Disable TOTP |
| `/api/v1/user/totp/control/regenerate` | POST | Regenerate secret |
| `/api/v1/user/totp/control/sessions` | GET | Get active sessions |
| `/api/v1/user/totp/control/sessions/{id}` | DELETE | Revoke specific session |
| `/api/v1/user/totp/control/sessions` | DELETE | Revoke all sessions |

---

## Testing with Local Server

If testing locally (e.g., `http://localhost:8080`):

```bash
curl -X GET "http://localhost:8080/api/v1/user/totp/control/status" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## Error Handling

All endpoints return standard error responses. Check the `success` field:

```bash
# Example error response
{
  "success": false,
  "status": 400,
  "message": "Invalid request",
  "error": {
    "message": "Session not found or already expired"
  }
}
```

---

**Note:** Replace `YOUR_TOKEN` and `https://api.example.com` with your actual values before running these commands.


