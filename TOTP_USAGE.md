# TOTP Utility Usage Guide

## Overview
TOTP (Time-based One-Time Password) generation and validation logic has been implemented in `users/TOTPUtil.kt`.

**Important**: This is NOT an API endpoint - it's utility functions that can be used internally.

---

## Features
- ✅ TOTP codes valid for **60 seconds**
- ✅ **6-digit codes** (compatible with Google Authenticator)
- ✅ **HMAC-SHA1 algorithm** (RFC 6238 compliant)
- ✅ Secret keys stored in database (`users.totp_secret` column)
- ✅ **Secret keys NEVER exposed in API responses**

---

## Available Functions

### 1. Generate Secret Key
```kotlin
import users.TOTPUtil

val secret = TOTPUtil.generateSecret()
// Returns: Base32-encoded secret key (e.g., "JBSWY3DPEHPK3PXP")
// Store this in user's totpSecret field in database
```

### 2. Generate TOTP Code
```kotlin
import users.TOTPUtil

// Get secret from database
val secret = user.totpSecret // From database

// Generate current TOTP code
val code = TOTPUtil.generateCode(secret)
// Returns: 6-digit code as String (e.g., "123456")
```

### 3. Validate TOTP Code
```kotlin
import users.TOTPUtil

// Get secret from database
val secret = user.totpSecret // From database

// Validate user-provided code
val userCode = "123456"
val isValid = TOTPUtil.validateCode(secret, userCode)
// Returns: true if valid, false otherwise
```

### 4. Get Remaining Seconds
```kotlin
import users.TOTPUtil

val remaining = TOTPUtil.getRemainingSeconds()
// Returns: Seconds remaining in current time window (0-59)
```

### 5. Check if TOTP is Enabled
```kotlin
import users.TOTPUtil

val isEnabled = TOTPUtil.isTOTPEnabled(user.totpSecret, user.totpEnabled)
// Returns: true if TOTP is properly configured and enabled
```

---

## Example Usage in Service Layer

```kotlin
// Example: Generate secret when user enables TOTP
fun enableTOTP(userId: String) {
    val secret = TOTPUtil.generateSecret()
    
    // Store in database (never return to client)
    Users.update({ Users.userId eq userId }) {
        it[totpSecret] = secret
        it[totpEnabled] = true
    }
}

// Example: Validate TOTP code during login
fun validateTOTPCode(userId: String, code: String): Boolean {
    val user = Users.select { Users.userId eq userId }.firstOrNull()
    val secret = user?.get(Users.totpSecret)
    
    if (secret == null) {
        return false
    }
    
    return TOTPUtil.validateCode(secret, code)
}
```

---

## Security Notes

### ✅ DO:
- Store secrets in database (`users.totp_secret` column)
- Use secrets only for generation/validation internally
- Validate codes server-side
- Use time window tolerance for clock skew

### ❌ DON'T:
- **Never send `totpSecret` in API responses**
- Never log secrets
- Never expose secrets in error messages
- Never store secrets in plain text logs

---

## Configuration

- **Time Step**: 60 seconds
- **Code Length**: 6 digits
- **Algorithm**: HMAC-SHA1
- **Clock Skew Tolerance**: ±1 time window (default)

---

## Database Schema

The `users` table already has:
- `totp_secret` (VARCHAR, nullable) - Base32-encoded secret key
- `totp_enabled` (BOOLEAN, default false) - Whether TOTP is enabled

---

## Compatibility

- ✅ Google Authenticator
- ✅ Microsoft Authenticator
- ✅ Authy
- ✅ Any RFC 6238 compliant authenticator app

---

## Notes

1. **Not an API**: These are utility functions, not API endpoints
2. **Server-side only**: All TOTP operations happen server-side
3. **Secret management**: Secrets are stored securely in the database
4. **No exposure**: Secrets are never included in API responses (already fixed in `DeviceRegistrationResponse`)

