package com.yourpackage.util

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Android Encryption Utility for decrypting API responses
 * Matches the server-side encryption implementation
 */
object EncryptionUtil {
    
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"
    
    // Base key - should match server's ENCRYPTION_KEY or default
    // You can put this in BuildConfig or strings.xml for production
    private const val BASE_KEY = "default-encryption-key-base"
    
    /**
     * Derive user-specific encryption key from userId
     * Uses SHA-256 hash of (baseKey + userId) to ensure consistent 32-byte key
     * Then takes first 16 bytes for AES-128
     */
    private fun deriveUserKey(userId: String): ByteArray {
        val combined = "$BASE_KEY:$userId"
        
        // Use SHA-256 to create a consistent 32-byte hash
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(combined.toByteArray(Charsets.UTF_8))
        
        // Take first 16 bytes for AES-128
        return hashBytes.sliceArray(0..15)
    }
    
    /**
     * Decrypt a Base64 encoded encrypted string using user-specific key
     * 
     * @param encryptedData The Base64 encoded encrypted data from API
     * @param userId The user ID (Bearer token) to derive the decryption key
     * @return Decrypted JSON string
     */
    fun decrypt(encryptedData: String, userId: String): String {
        try {
            val keyBytes = deriveUserKey(userId)
            val key = SecretKeySpec(keyBytes, ALGORITHM)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key)
            
            // Decode Base64
            val encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
            
            // Decrypt
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Decryption failed: ${e.message}. Make sure you're using the correct userId (Bearer token).", e)
        }
    }
}

/**
 * Usage Example in your Android Activity/Fragment/ViewModel
 */
class UsageStatsExample {
    
    // Your Bearer token (userId) - get this from your auth storage
    private val userId = "61a7126f-e1d9-58dd-b79e-333928e83f03"
    
    /**
     * Example: Decrypt daily usage stats from API response
     */
    fun decryptUsageStats(encryptedData: String): List<DailyUsageStatsItem> {
        try {
            // Decrypt the encrypted data
            val decryptedJson = EncryptionUtil.decrypt(encryptedData, userId)
            
            // Parse JSON (using kotlinx.serialization or Gson)
            // Example with kotlinx.serialization:
            val json = kotlinx.serialization.json.Json { 
                ignoreUnknownKeys = true 
            }
            val stats = json.decodeFromString<List<DailyUsageStatsItem>>(decryptedJson)
            
            return stats
        } catch (e: Exception) {
            Log.e("Decryption", "Failed to decrypt usage stats", e)
            throw e
        }
    }
    
    /**
     * Example: Complete API call and decryption flow
     */
    suspend fun fetchDailyUsageStats(date: String): List<DailyUsageStatsItem> {
        // 1. Make API call
        val response = apiService.getDailyUsageStats(
            date = date,
            bearerToken = userId // Your Bearer token
        )
        
        // 2. Extract encrypted data from response
        val encryptedData = response.data.encryptedData
        
        // 3. Decrypt using your userId
        val decryptedJson = EncryptionUtil.decrypt(encryptedData, userId)
        
        // 4. Parse JSON
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        return json.decodeFromString<List<DailyUsageStatsItem>>(decryptedJson)
    }
}

/**
 * Data class matching server response
 */
@kotlinx.serialization.Serializable
data class DailyUsageStatsItem(
    val packageName: String,
    val appName: String? = null,
    val usageTime: Long, // milliseconds
    val duration: Long, // milliseconds
    val isSystemApp: Boolean = false,
    val category: String? = null,
    val totalScreenTime: Long, // milliseconds
    val lastUsed: String? = null, // ISO 8601 format
    val openedAt: String? = null // ISO 8601 format
)

/**
 * API Response wrapper
 */
@kotlinx.serialization.Serializable
data class DailyUsageStatsResponse(
    val encryptedData: String,
    val date: String
)

