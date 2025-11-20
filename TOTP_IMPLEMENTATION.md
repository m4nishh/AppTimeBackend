# TOTP Implementation Guide

## Overview
TOTP (Time-based One-Time Password) utility functions have been implemented in `users/TOTPUtil.kt`.

## Features
- ✅ TOTP codes valid for 60 seconds
- ✅ 6-digit codes (compatible with Google Authenticator)
- ✅ HMAC-SHA1 algorithm
- ✅ Secret keys stored in database (never exposed in API)
- ✅ Generation and validation functions

## Usage

### Generate Secret Key
```kotlin
import users.TOTPUtil

val secret = TOTPUtil.generateSecret()
// Store this in user's totpSecret field in database
```

### Generate TOTP Code
```kotlin
import users.TOTPUtil

val secret = "user's_base32_secret_from_database"
val code = TOTPUtil.generateCode(secret)
// Returns 6-digit code as String (e.g., "123456")
```

### Validate TOTP Code
```kotlin
import users.TOTPUtil

val secret = "user's_base32_secret_from_database"
val userCode = "123456"
val isValid = TOTPUtil.validateCode(secret, userCode)
// Returns true if code is valid, false otherwise
```

### Get Remaining Seconds
```kotlin
import users.TOTPUtil

val remaining = TOTPUtil.getRemainingSeconds()
// Returns seconds remaining in current time window (0-59)
```

## Security Notes
- **Never expose secrets**: The `totpSecret` field is never returned in API responses
- **Store securely**: Secrets are stored in the database in the `users.totp_secret` column
- **Base32 encoding**: Secrets are Base32-encoded for compatibility with authenticator apps

## Configuration
- Time step: 60 seconds
- Code length: 6 digits
- Algorithm: HMAC-SHA1
- Clock skew tolerance: ±1 time window (default)

