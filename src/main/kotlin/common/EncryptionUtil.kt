package com.apptime.code.common

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Encryption utility for encrypting API responses
 * Uses user-specific encryption keys derived from userId
 * Frontend can derive the same key from the userId (Bearer token) they already have
 */
object EncryptionUtil {
    
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding" // Explicit transformation with padding
    
    /**
     * Get base encryption key from environment variable or use default (for development)
     * This is combined with userId to create user-specific keys
     */
    private fun getBaseKey(): String {
        return System.getenv("ENCRYPTION_KEY") 
            ?: "default-encryption-key-base"
    }
    
    /**
     * Derive a user-specific encryption key from userId
     * Uses SHA-256 hash of (baseKey + userId) to ensure consistent 32-byte key
     * Then takes first 16 bytes for AES-128
     */
    private fun deriveUserKey(userId: String): ByteArray {
        val baseKey = getBaseKey()
        val combined = "$baseKey:$userId"
        
        // Use SHA-256 to create a consistent 32-byte hash
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(combined.toByteArray(Charsets.UTF_8))
        
        // Take first 16 bytes for AES-128
        return hashBytes.sliceArray(0..15)
    }
    
    /**
     * Encrypt a string using user-specific key and return Base64 encoded encrypted data
     * @param data The data to encrypt
     * @param userId The user ID (Bearer token) to derive the encryption key
     */
    fun encrypt(data: String, userId: String): String {
        try {
            val keyBytes = deriveUserKey(userId)
            val key = SecretKeySpec(keyBytes, ALGORITHM)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(encryptedBytes)
        } catch (e: Exception) {
            throw RuntimeException("Encryption failed: ${e.message}", e)
        }
    }
    
    /**
     * Decrypt a Base64 encoded encrypted string using user-specific key
     * @param encryptedData The Base64 encoded encrypted data
     * @param userId The user ID (Bearer token) to derive the decryption key
     */
    fun decrypt(encryptedData: String, userId: String): String {
        try {
            val keyBytes = deriveUserKey(userId)
            val key = SecretKeySpec(keyBytes, ALGORITHM)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key)
            val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData))
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Decryption failed: ${e.message}. Make sure you're using the correct userId (Bearer token).", e)
        }
    }
    
    /**
     * Legacy method for backward compatibility - uses global key
     * @deprecated Use encrypt(data, userId) instead for user-specific encryption
     */
    @Deprecated("Use encrypt(data, userId) for user-specific encryption")
    fun encrypt(data: String): String {
        val defaultUserId = "default-user"
        return encrypt(data, defaultUserId)
    }
    
    /**
     * Legacy method for backward compatibility - uses global key
     * @deprecated Use decrypt(encryptedData, userId) instead for user-specific decryption
     */
    @Deprecated("Use decrypt(encryptedData, userId) for user-specific decryption")
    fun decrypt(encryptedData: String): String {
        val defaultUserId = "default-user"
        return decrypt(encryptedData, defaultUserId)
    }
}

