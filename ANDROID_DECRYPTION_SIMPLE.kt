/**
 * SIMPLE ANDROID DECRYPTION EXAMPLE
 * 
 * Copy this code to your Android project
 */

package com.yourpackage.util

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object DecryptionHelper {
    
    // IMPORTANT: This must match your server's ENCRYPTION_KEY or default
    // Default on server: "default-encryption-key-base"
    private const val BASE_KEY = "default-encryption-key-base"
    
    /**
     * Decrypt encrypted data from API
     * 
     * @param encryptedData Base64 string from API response (e.g., "QEGvoj27RuTMbMBzRf3VuA==")
     * @param userId Your Bearer token / userId (e.g., "61a7126f-e1d9-58dd-b79e-333928e83f03")
     * @return Decrypted JSON string
     */
    fun decrypt(encryptedData: String, userId: String): String {
        // Step 1: Combine base key with userId
        val combined = "$BASE_KEY:$userId"
        
        // Step 2: Create SHA-256 hash
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(combined.toByteArray(Charsets.UTF_8))
        
        // Step 3: Take first 16 bytes for AES-128 key
        val keyBytes = hashBytes.sliceArray(0..15)
        
        // Step 4: Create cipher and decrypt
        val key = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key)
        
        // Step 5: Decode Base64 and decrypt
        val encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        
        // Step 6: Convert to string
        return String(decryptedBytes, Charsets.UTF_8)
    }
}

/**
 * USAGE EXAMPLE:
 * 
 * // 1. Get encrypted data from API response
 * val encryptedData = apiResponse.data.encryptedData
 * 
 * // 2. Get your userId (Bearer token)
 * val userId = "61a7126f-e1d9-58dd-b79e-333928e83f03"
 * 
 * // 3. Decrypt
 * val decryptedJson = DecryptionHelper.decrypt(encryptedData, userId)
 * 
 * // 4. Parse JSON (using Gson, kotlinx.serialization, etc.)
 * val stats = gson.fromJson(decryptedJson, Array<DailyUsageStatsItem>::class.java).toList()
 * 
 * // 5. Use the data
 * stats.forEach { item ->
 *     println("App: ${item.appName}, Usage: ${item.usageTime}ms")
 * }
 */

