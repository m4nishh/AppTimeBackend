# Frontend Decryption Guide

## Overview

The API uses **user-specific encryption keys** derived from the user's ID (Bearer token). This means:
- ✅ Each user has their own unique encryption key
- ✅ Frontend can derive the key from the userId they already have
- ✅ No shared secret needs to be exposed in frontend code
- ✅ Only the authenticated user can decrypt their own data

## How It Works

1. **Server Side**: 
   - Takes `baseKey` (from `ENCRYPTION_KEY` env var or default)
   - Combines with `userId`: `"$baseKey:$userId"`
   - Creates SHA-256 hash and takes first 16 bytes for AES-128 key
   - Encrypts data with this user-specific key

2. **Frontend Side**:
   - Uses the same `baseKey` (can be in config/env)
   - Uses the `userId` from Bearer token
   - Derives the same key using the same algorithm
   - Decrypts the data

## Frontend Implementation Examples

### JavaScript/TypeScript

```javascript
import CryptoJS from 'crypto-js';

// Base key - should match server's ENCRYPTION_KEY or default
const BASE_KEY = process.env.REACT_APP_ENCRYPTION_KEY || 'default-encryption-key-base';

/**
 * Derive user-specific encryption key from userId
 */
function deriveUserKey(userId) {
  const combined = `${BASE_KEY}:${userId}`;
  // Create SHA-256 hash
  const hash = CryptoJS.SHA256(combined);
  // Get first 16 bytes (32 hex chars = 16 bytes)
  const keyHex = hash.toString().substring(0, 32);
  return CryptoJS.enc.Hex.parse(keyHex);
}

/**
 * Decrypt encrypted data using user-specific key
 */
function decrypt(encryptedData, userId) {
  try {
    const key = deriveUserKey(userId);
    const decrypted = CryptoJS.AES.decrypt(encryptedData, key, {
      mode: CryptoJS.mode.ECB,
      padding: CryptoJS.pad.Pkcs7
    });
    return decrypted.toString(CryptoJS.enc.Utf8);
  } catch (error) {
    throw new Error(`Decryption failed: ${error.message}`);
  }
}

// Usage example
const userId = '61a7126f-e1d9-58dd-b79e-333928e83f03'; // From Bearer token
const encryptedData = 'QEGvoj27RuTMbMBzRf3VuA=='; // From API response

const decryptedJson = decrypt(encryptedData, userId);
const stats = JSON.parse(decryptedJson);
console.log(stats);
```

### Kotlin (Android)

```kotlin
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object EncryptionUtil {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"
    
    // Base key - should match server's ENCRYPTION_KEY or default
    private val BASE_KEY = System.getenv("ENCRYPTION_KEY") 
        ?: "default-encryption-key-base"
    
    /**
     * Derive user-specific encryption key from userId
     */
    private fun deriveUserKey(userId: String): ByteArray {
        val combined = "$BASE_KEY:$userId"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(combined.toByteArray(Charsets.UTF_8))
        // Take first 16 bytes for AES-128
        return hashBytes.sliceArray(0..15)
    }
    
    /**
     * Decrypt encrypted data using user-specific key
     */
    fun decrypt(encryptedData: String, userId: String): String {
        val keyBytes = deriveUserKey(userId)
        val key = SecretKeySpec(keyBytes, ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key)
        val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData))
        return String(decryptedBytes, Charsets.UTF_8)
    }
}

// Usage example
val userId = "61a7126f-e1d9-58dd-b79e-333928e83f03" // From Bearer token
val encryptedData = "QEGvoj27RuTMbMBzRf3VuA==" // From API response

val decryptedJson = EncryptionUtil.decrypt(encryptedData, userId)
val json = Json { ignoreUnknownKeys = true }
val stats = json.decodeFromString<List<DailyUsageStatsItem>>(decryptedJson)
```

### Python

```python
import hashlib
import base64
from Crypto.Cipher import AES
from Crypto.Util.Padding import unpad
import json

# Base key - should match server's ENCRYPTION_KEY or default
BASE_KEY = os.getenv('ENCRYPTION_KEY', 'default-encryption-key-base')

def derive_user_key(user_id):
    """Derive user-specific encryption key from userId"""
    combined = f"{BASE_KEY}:{user_id}"
    # Create SHA-256 hash
    hash_bytes = hashlib.sha256(combined.encode('utf-8')).digest()
    # Take first 16 bytes for AES-128
    return hash_bytes[:16]

def decrypt(encrypted_data, user_id):
    """Decrypt encrypted data using user-specific key"""
    key = derive_user_key(user_id)
    cipher = AES.new(key, AES.MODE_ECB)
    decrypted = cipher.decrypt(base64.b64decode(encrypted_data))
    # Remove PKCS5 padding
    decrypted = unpad(decrypted, AES.block_size)
    return decrypted.decode('utf-8')

# Usage example
user_id = "61a7126f-e1d9-58dd-b79e-333928e83f03"  # From Bearer token
encrypted_data = "QEGvoj27RuTMbMBzRf3VuA=="  # From API response

decrypted_json = decrypt(encrypted_data, user_id)
stats = json.loads(decrypted_json)
print(stats)
```

## Key Points

1. **Base Key**: 
   - Default: `"default-encryption-key-base"`
   - Or set via `ENCRYPTION_KEY` environment variable
   - Must match between server and client

2. **User ID**: 
   - This is the Bearer token (userId) the frontend already has
   - Example: `"61a7126f-e1d9-58dd-b79e-333928e83f03"`

3. **Key Derivation**:
   - Combine: `"$baseKey:$userId"`
   - SHA-256 hash the combined string
   - Take first 16 bytes for AES-128 key

4. **Decryption**:
   - Use AES-128 with ECB mode and PKCS5 padding
   - Decode Base64 first, then decrypt

## Security Notes

- ✅ Each user has a unique key - users can't decrypt each other's data
- ✅ Base key is not exposed in API responses
- ✅ Frontend only needs the base key (can be in environment/config)
- ✅ User ID is already known to the frontend (Bearer token)
- ⚠️ Base key should be kept secret - use environment variables
- ⚠️ Use HTTPS in production to protect data in transit

## Testing

Test decryption with the test endpoint:

```bash
curl -X GET 'http://localhost:8080/api/usage/stats/daily/test-decrypt?encryptedData=QEGvoj27RuTMbMBzRf3VuA==' \
  -H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

This will show you the decrypted data and verify your implementation works correctly.

