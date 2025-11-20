package users

import java.nio.ByteBuffer
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * TOTP (Time-based One-Time Password) Utility
 * 
 * Implements RFC 6238 TOTP generation and validation
 * Based on the Java implementation provided
 * - TOTP codes are valid for 60 seconds
 * - Uses HMAC-SHA1 algorithm (compatible with Google Authenticator)
 * - 6-digit codes
 */
object TOTPUtil {
    
    // TOTP configuration
    private const val HMAC_ALGO = "HmacSHA1" // RFC default
    private const val DIGITS = 6 // 6-digit code
    private const val TIME_STEP_SECONDS = 60L // 60 seconds = 1 minute
    
    /**
     * Generate a new TOTP secret key
     * Returns a Base32-encoded secret key
     * Store this in the user's totpSecret field in the database
     */
    fun generateSecret(): String {
        val random = java.security.SecureRandom()
        val bytes = ByteArray(20) // 160 bits = 20 bytes (recommended for TOTP)
        random.nextBytes(bytes)
        
        return base32Encode(bytes)
    }
    
    /**
     * Generate a TOTP code for the supplied Base32-encoded secret.
     *
     * @param base32Secret Base32-encoded secret key
     * @return zero-padded numeric code as String
     */
    fun generateTOTP(base32Secret: String): String {
        val key = base32Decode(base32Secret)
        val timestamp = Instant.now().epochSecond
        val counter = timestamp / TIME_STEP_SECONDS
        return generateTOTPFromKeyAndCounter(key, counter, DIGITS, HMAC_ALGO)
    }
    
    /**
     * Generate TOTP code for a specific time counter
     */
    private fun generateTOTPFromKeyAndCounter(
        key: ByteArray,
        counter: Long,
        digits: Int,
        hmacAlgo: String
    ): String {
        try {
            // 8-byte big-endian counter
            val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()
            
            val mac = Mac.getInstance(hmacAlgo)
            val keySpec = SecretKeySpec(key, hmacAlgo)
            mac.init(keySpec)
            val hmac = mac.doFinal(counterBytes)
            
            // Dynamic truncation (RFC4226)
            val offset = hmac[hmac.size - 1].toInt() and 0x0F
            val binary = ((hmac[offset].toInt() and 0x7f) shl 24) or
                    ((hmac[offset + 1].toInt() and 0xff) shl 16) or
                    ((hmac[offset + 2].toInt() and 0xff) shl 8) or
                    (hmac[offset + 3].toInt() and 0xff)
            
            val otp = binary % (10.0.pow(digits).toInt())
            return String.format("%0${digits}d", otp)
        } catch (e: Exception) {
            throw RuntimeException("TOTP generation failed", e)
        }
    }
    
    /**
     * Validate TOTP code against a secret
     * 
     * @param secret Base32-encoded secret key (from database)
     * @param code TOTP code to validate (6-digit string)
     * @param timeWindowTolerance Number of time windows to check (default: 1)
     *                            Allows for clock skew. 1 = current window ± 1 (3 windows total)
     * @return true if code is valid, false otherwise
     */
    fun validateCode(secret: String, code: String, timeWindowTolerance: Int = 1): Boolean {
        if (secret.isBlank() || code.isBlank()) {
            return false
        }
        
        // Normalize code (remove spaces, ensure 6 digits)
        val normalizedCode = code.replace(" ", "").trim()
        if (normalizedCode.length != DIGITS || !normalizedCode.all { it.isDigit() }) {
            return false
        }
        
        try {
            val key = base32Decode(secret)
            val timestamp = Instant.now().epochSecond
            val currentCounter = timestamp / TIME_STEP_SECONDS
            
            // Check current time window and adjacent windows (for clock skew tolerance)
            for (i in -timeWindowTolerance..timeWindowTolerance) {
                val counter = currentCounter + i
                val generatedCode = generateTOTPFromKeyAndCounter(key, counter, DIGITS, HMAC_ALGO)
                
                if (generatedCode == normalizedCode) {
                    return true
                }
            }
            
            return false
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Minimal Base32 decoder (RFC 4648, no padding required).
     * Matches the Java implementation provided.
     */
    private fun base32Decode(base32: String): ByteArray {
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        // remove padding and whitespace, uppercase
        val s = base32.trim().replace("=", "").replace(Regex("\\s+"), "").uppercase()
        
        // each 8 chars -> 5 bytes
        val outputLength = s.length * 5 / 8
        val result = ByteArray(outputLength)
        
        var buffer = 0
        var bitsLeft = 0
        var index = 0
        
        for (c in s.toCharArray()) {
            val val_ = base32Chars.indexOf(c)
            if (val_ < 0) throw IllegalArgumentException("Invalid Base32 character: $c")
            
            buffer = (buffer shl 5) or val_
            bitsLeft += 5
            
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                result[index++] = ((buffer shr bitsLeft) and 0xFF).toByte()
            }
        }
        
        // if index < result.length, return trimmed array
        return if (index != result.size) {
            result.copyOf(index)
        } else {
            result
        }
    }
    
    /**
     * Base32 encoder (RFC 4648)
     * Used for generating new secrets
     */
    private fun base32Encode(bytes: ByteArray): String {
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val result = StringBuilder()
        
        var buffer = 0
        var bitsLeft = 0
        
        for (byte in bytes) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bitsLeft += 8
            
            while (bitsLeft >= 5) {
                val index = (buffer shr (bitsLeft - 5)) and 0x1F
                result.append(base32Chars[index])
                bitsLeft -= 5
            }
        }
        
        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1F
            result.append(base32Chars[index])
        }
        
        return result.toString()
    }
    
    /**
     * Get remaining seconds until TOTP code expires
     * 
     * @return Seconds remaining in current time window (0-59)
     */
    fun getRemainingSeconds(): Int {
        val currentTimeSeconds = Instant.now().epochSecond
        val remaining = TIME_STEP_SECONDS - (currentTimeSeconds % TIME_STEP_SECONDS)
        return remaining.toInt()
    }
    
    /**
     * Check if TOTP is enabled for a user
     * Helper function to check if user has TOTP configured
     * 
     * @param totpSecret User's TOTP secret from database (can be null)
     * @param totpEnabled User's TOTP enabled flag from database
     * @return true if TOTP is properly configured and enabled
     */
    fun isTOTPEnabled(totpSecret: String?, totpEnabled: Boolean): Boolean {
        return totpEnabled && !totpSecret.isNullOrBlank()
    }
}
