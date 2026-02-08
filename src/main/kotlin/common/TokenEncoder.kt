package com.apptime.code.common

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import java.security.SecureRandom

/**
 * Common utility class for encoding/decoding tokens
 * Encrypts data to hide sensitive information in URLs
 * Used for challenge shares, referrals, and other shareable links
 */
object TokenEncoder {
    
    private val algorithm = "AES/CBC/PKCS5Padding"
    private val keyAlgorithm = "AES"
    
    /**
     * Get encryption key from environment or use default
     * In production, set SHARE_TOKEN_SECRET in environment variables
     */
    private fun getSecretKey(): ByteArray {
        val secret = EnvLoader.getEnv("SHARE_TOKEN_SECRET") 
            ?: "AppTimeShareTokenSecretKey2024" // Default key (change in production!)
        
        // Ensure key is exactly 32 bytes (256 bits) for AES-256
        return secret.padEnd(32, '0').take(32).toByteArray(Charsets.UTF_8)
    }
    
    /**
     * Generate a random IV (Initialization Vector) for encryption
     */
    private fun generateIV(): ByteArray {
        val iv = ByteArray(16) // 128 bits for AES
        SecureRandom().nextBytes(iv)
        return iv
    }
    
    /**
     * Encode data into a secure token
     * @param data The data to encode (e.g., "challengeId|shareCode" or "referralCode")
     * @return Base64 URL-safe encoded token
     */
    fun encode(data: String): String {
        try {
            val key = SecretKeySpec(getSecretKey(), keyAlgorithm)
            val iv = generateIV()
            val ivSpec = IvParameterSpec(iv)
            
            val cipher = Cipher.getInstance(algorithm)
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
            
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            // Combine IV + encrypted data, then base64 encode
            val combined = iv + encrypted
            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined)
        } catch (e: Exception) {
            throw RuntimeException("Failed to encode token: ${e.message}", e)
        }
    }
    
    /**
     * Decode token back to original data
     * @param token The encrypted token
     * @return Decoded data string or null if invalid
     */
    fun decode(token: String): String? {
        try {
            // Decode base64
            val combined = Base64.getUrlDecoder().decode(token)
            
            // Extract IV (first 16 bytes) and encrypted data (rest)
            val iv = combined.sliceArray(0..15)
            val encrypted = combined.sliceArray(16 until combined.size)
            
            val key = SecretKeySpec(getSecretKey(), keyAlgorithm)
            val ivSpec = IvParameterSpec(iv)
            
            val cipher = Cipher.getInstance(algorithm)
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
            
            val decrypted = cipher.doFinal(encrypted)
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            // Invalid token
            return null
        }
    }
    
    /**
     * Encode challenge ID and share code into a secure token
     * Format: challengeId|shareCode -> encrypted -> base64
     */
    fun encodeChallengeShare(challengeId: Long, shareCode: String): String {
        return encode("$challengeId|$shareCode")
    }
    
    /**
     * Decode challenge share token back to challenge ID and share code
     * Returns Pair<challengeId, shareCode> or null if invalid
     */
    fun decodeChallengeShare(token: String): Pair<Long, String>? {
        val decoded = decode(token) ?: return null
        
        val parts = decoded.split("|")
        if (parts.size != 2) {
            return null
        }
        
        val challengeId = parts[0].toLongOrNull() ?: return null
        val shareCode = parts[1]
        
        return Pair(challengeId, shareCode)
    }
    
    /**
     * Encode referral code into a secure token
     */
    fun encodeReferral(referralCode: String): String {
        return encode(referralCode)
    }
    
    /**
     * Decode referral token back to referral code
     */
    fun decodeReferral(token: String): String? {
        return decode(token)
    }
    
    /**
     * Encode clan ID and share code into a secure token
     * Format: clanId|shareCode -> encrypted -> base64
     */
    fun encodeClanShare(clanId: Long, shareCode: String): String {
        return encode("$clanId|$shareCode")
    }
    
    /**
     * Decode clan share token back to clan ID and share code
     * Returns Pair<clanId, shareCode> or null if invalid
     */
    fun decodeClanShare(token: String): Pair<Long, String>? {
        val decoded = decode(token) ?: return null
        
        val parts = decoded.split("|")
        if (parts.size != 2) {
            return null
        }
        
        val clanId = parts[0].toLongOrNull() ?: return null
        val shareCode = parts[1]
        
        return Pair(clanId, shareCode)
    }
    
    /**
     * Validate token format (quick check without full decryption)
     */
    fun isValidToken(token: String): Boolean {
        return try {
            Base64.getUrlDecoder().decode(token)
            true
        } catch (e: Exception) {
            false
        }
    }
}

