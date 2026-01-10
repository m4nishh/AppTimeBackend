package users

/**
 * User service layer - handles business logic
 */
class UserService(private val repository: UserRepository) {
    
    /**
     * Register a new device
     */
    suspend fun registerDevice(request: DeviceRegistrationRequest): DeviceRegistrationResponse {
        // Validate device info
        if (request.deviceInfo.deviceId.isBlank()) {
            throw IllegalArgumentException("Device ID is required")
        }
        
        return repository.registerDevice(request.deviceInfo, request.firebaseToken)
    }
    
    /**
     * Get user profile (always includes syncTime)
     */
    suspend fun getUserProfile(userId: String): UserProfile? {
        if (userId.isBlank()) {
            throw IllegalArgumentException("User ID is required")
        }
        
        return repository.getUserById(userId)
    }
    
    /**
     * Update user profile - username and firebaseToken can be updated
     */
    suspend fun updateUserProfile(userId: String, username: String?, firebaseToken: String? = null): UserProfile? {
        if (userId.isBlank()) {
            throw IllegalArgumentException("User ID is required")
        }

        // Check if user exists
        if (!repository.userExists(userId)) {
            throw IllegalArgumentException("User not found")
        }

        // Update username if provided
        if (username != null && username.isNotBlank()) {
            repository.updateUsername(userId, username)
        }
        
        // Update firebaseToken if provided
        if (firebaseToken != null) {
            repository.updateFirebaseToken(userId, firebaseToken)
        }

        // Return updated profile
        return repository.getUserById(userId)
    }
    
    /**
     * Search users by name
     * Returns up to 10 matching users
     */
    suspend fun searchUsers(query: String, limit: Int = 10): List<UserSearchResult> {
        if (query.isBlank()) {
            throw IllegalArgumentException("Search query cannot be empty")
        }
        
        if (query.length < 2) {
            throw IllegalArgumentException("Search query must be at least 2 characters")
        }
        
        return repository.searchUsersByName(query, limit)
    }
    
    /**
     * Generate TOTP code for a user by username
     * Returns the 6-digit code (secret is never exposed)
     */
    suspend fun generateTOTPCodeByUsername(username: String): TOTPGenerateResponse {
        if (username.isBlank()) {
            throw IllegalArgumentException("Username is required")
        }
        
        // Check if user exists and has TOTP enabled
        if (!repository.isTOTPEnabledForUserByUsername(username)) {
            throw IllegalArgumentException("TOTP is not enabled for this user")
        }
        
        // Get secret from database
        val secret = repository.getTOTPSecretByUsername(username)
            ?: throw IllegalArgumentException("TOTP secret not found for user")
        
        // Generate TOTP code
        val code = TOTPUtil.generateTOTP(secret)
        val remainingSeconds = TOTPUtil.getRemainingSeconds()
        
        // Calculate expiration time
        val expiresAt = kotlinx.datetime.Instant.fromEpochMilliseconds(
            System.currentTimeMillis() + (remainingSeconds * 1000)
        )
        
        return TOTPGenerateResponse(
            code = code,
            remainingSeconds = remainingSeconds,
            expiresAt = expiresAt.toString()
        )
    }
    
    /**
     * Generate TOTP code for authenticated user by userId
     * Returns the 6-digit code (secret is never exposed)
     */
    suspend fun generateTOTPCodeByUserId(userId: String): TOTPGenerateResponse {
        if (userId.isBlank()) {
            throw IllegalArgumentException("User ID is required")
        }
        
        // Check if user exists and has TOTP enabled
        if (!repository.isTOTPEnabledForUser(userId)) {
            throw IllegalArgumentException("TOTP is not enabled for this user")
        }
        
        // Get secret from database
        val secret = repository.getTOTPSecret(userId)
            ?: throw IllegalArgumentException("TOTP secret not found for user")
        
        // Generate TOTP code
        val code = TOTPUtil.generateTOTP(secret)
        val remainingSeconds = TOTPUtil.getRemainingSeconds()
        
        // Calculate expiration time
        val expiresAt = kotlinx.datetime.Instant.fromEpochMilliseconds(
            System.currentTimeMillis() + (remainingSeconds * 1000)
        )
        
        return TOTPGenerateResponse(
            code = code,
            remainingSeconds = remainingSeconds,
            expiresAt = expiresAt.toString()
        )
    }
    
    /**
     * Verify TOTP code and get user profile by username
     * User A needs User B's TOTP code to see User B's details
     */
    suspend fun verifyTOTPAndGetProfile(username: String, code: String): PublicUserProfile {
        if (username.isBlank()) {
            throw IllegalArgumentException("Username is required")
        }
        
        if (code.isBlank()) {
            throw IllegalArgumentException("TOTP code is required")
        }
        
        // Check if user exists and has TOTP enabled
        if (!repository.isTOTPEnabledForUserByUsername(username)) {
            throw IllegalArgumentException("TOTP is not enabled for this user")
        }
        
        // Get secret from database
        val secret = repository.getTOTPSecretByUsername(username)
            ?: throw IllegalArgumentException("TOTP secret not found for user")
        
        // Validate code
        val isValid = TOTPUtil.validateCode(secret, code)
        
        if (!isValid) {
            throw IllegalArgumentException("Invalid TOTP code")
        }
        
        // Get user profile (without userId)
        val profile = repository.getUserProfileByUsername(username)
            ?: throw IllegalArgumentException("User not found")
        
        // Return public profile (without userId)
        return PublicUserProfile(
            username = profile.username,
            email = profile.email,
            name = profile.name,
            createdAt = profile.createdAt,
            updatedAt = profile.updatedAt,
            lastSyncTime = profile.lastSyncTime
        )
    }
    
    /**
     * Verify TOTP code for a user by username and create verification session
     * @param username Target user's username (User B)
     * @param code TOTP code to verify
     * @param requestingUserId User A's ID (who is requesting access) - required
     * @param durationSeconds Optional duration in seconds for access (default: 3600 = 1 hour, min: 60, max: 86400 = 24 hours)
     */
    suspend fun verifyTOTPCodeByUsername(
        username: String,
        code: String,
        requestingUserId: String?,
        durationSeconds: Int? = null
    ): TOTPVerifyResponse {
        if (username.isBlank()) {
            throw IllegalArgumentException("Username is required")
        }
        
        if (code.isBlank()) {
            throw IllegalArgumentException("TOTP code is required")
        }
        
        if (requestingUserId.isNullOrBlank()) {
            return TOTPVerifyResponse(
                valid = false,
                message = "Requesting user ID is required"
            )
        }
        
        // Get target user ID
        val targetUserId = repository.getUserIdByUsername(username)
            ?: return TOTPVerifyResponse(
                valid = false,
                message = "User not found"
            )
        
        // Prevent users from verifying their own TOTP
        if (requestingUserId == targetUserId) {
            return TOTPVerifyResponse(
                valid = false,
                message = "Cannot verify your own TOTP code"
            )
        }
        
        // Check if user exists and has TOTP enabled
        if (!repository.isTOTPEnabledForUserByUsername(username)) {
            return TOTPVerifyResponse(
                valid = false,
                message = "TOTP is not enabled for this user"
            )
        }
        
        // Get secret from database
        val secret = repository.getTOTPSecretByUsername(username)
            ?: return TOTPVerifyResponse(
                valid = false,
                message = "TOTP secret not found for user"
            )
        
        // Validate code
        val isValid = TOTPUtil.validateCode(secret, code)
        
        return if (isValid) {
            // Validate and set duration (default: 1 hour, min: 60 seconds, max: 24 hours)
            val defaultDuration = 3600 // 1 hour
            val minDuration = 60 // 1 minute
            val maxDuration = 86400 // 24 hours
            
            val sessionDuration = when {
                durationSeconds == null -> defaultDuration
                durationSeconds < minDuration -> {
                    return TOTPVerifyResponse(
                        valid = false,
                        message = "Duration must be at least $minDuration seconds (1 minute)",
                        validitySeconds = 60
                    )
                }
                durationSeconds > maxDuration -> {
                    return TOTPVerifyResponse(
                        valid = false,
                        message = "Duration cannot exceed $maxDuration seconds (24 hours)",
                        validitySeconds = 60
                    )
                }
                else -> durationSeconds
            }
            
            // Create verification session with custom duration
            repository.createTOTPVerificationSession(
                requestingUserId = requestingUserId,
                targetUserId = targetUserId,
                targetUsername = username,
                durationSeconds = sessionDuration
            )
            
            // Calculate session expiration
            val sessionExpiresAt = kotlinx.datetime.Instant.fromEpochMilliseconds(
                System.currentTimeMillis() + (sessionDuration * 1000)
            )
            
            // Format duration message
            val durationMessage = when {
                sessionDuration < 3600 -> "${sessionDuration / 60} minute(s)"
                sessionDuration < 86400 -> "${sessionDuration / 3600} hour(s)"
                else -> "${sessionDuration / 86400} day(s)"
            }
            
            TOTPVerifyResponse(
                valid = true,
                message = "TOTP code verified successfully. Access granted for $durationMessage.",
                validitySeconds = sessionDuration,
                remainingSeconds = sessionDuration,
                expiresAt = sessionExpiresAt.toString()
            )
        } else {
            TOTPVerifyResponse(
                valid = false,
                message = "Invalid TOTP code",
                validitySeconds = 60
            )
        }
    }
    
    /**
     * Check if requesting user has valid access to target user's data
     */
    suspend fun hasAccessToUserData(
        requestingUserId: String,
        targetUserId: String
    ): Boolean {
        return repository.hasValidTOTPVerificationSession(requestingUserId, targetUserId)
    }
    
    /**
     * Get TOTP verification session details with remaining time
     */
    suspend fun getTOTPVerificationSessionDetails(
        requestingUserId: String,
        targetUserId: String
    ): Map<String, Any>? {
        return repository.getTOTPVerificationSessionDetails(requestingUserId, targetUserId)
    }
    
    /**
     * Check if TOTP is verified and access is still valid for a user by username
     * Returns access status with remaining time if valid
     */
    suspend fun checkTOTPAccessStatus(
        username: String,
        requestingUserId: String
    ): TOTPAccessStatusResponse {
        if (username.isBlank()) {
            return TOTPAccessStatusResponse(
                hasAccess = false,
                message = "Username is required"
            )
        }
        
        if (requestingUserId.isBlank()) {
            return TOTPAccessStatusResponse(
                hasAccess = false,
                message = "Requesting user ID is required"
            )
        }
        
        // Get target user ID
        val targetUserId = repository.getUserIdByUsername(username)
            ?: return TOTPAccessStatusResponse(
                hasAccess = false,
                message = "User not found"
            )
        
        // Get session details
        val sessionDetails = repository.getTOTPVerificationSessionDetails(requestingUserId, targetUserId)
        
        return if (sessionDetails != null) {
            val expiresAtStr = sessionDetails["expiresAt"] as? String
            val verifiedAtStr = sessionDetails["verifiedAt"] as? String
            val targetUsername = sessionDetails["targetUsername"] as? String
            val remainingSeconds = sessionDetails["remainingSeconds"] as? Int
            
            TOTPAccessStatusResponse(
                hasAccess = remainingSeconds != null && remainingSeconds > 0,
                message = if (remainingSeconds != null && remainingSeconds > 0) {
                    "Access is valid. ${remainingSeconds} second(s) remaining."
                } else {
                    "Access has expired."
                },
                verifiedAt = verifiedAtStr,
                expiresAt = expiresAtStr,
                remainingSeconds = remainingSeconds,
                targetUsername = targetUsername
            )
        } else {
            TOTPAccessStatusResponse(
                hasAccess = false,
                message = "No valid TOTP verification session found. Please verify TOTP code first."
            )
        }
    }
    
    /**
     * Get control panel overview - all users who have access to current user's data
     */
    suspend fun getControlPanelOverview(targetUserId: String): TOTPControlPanelResponse {
        if (targetUserId.isBlank()) {
            throw IllegalArgumentException("User ID is required")
        }
        
        // Get active TOTP sessions
        val activeSessions = repository.getActiveSessionsForTargetUser(targetUserId)
        val totpAccessors = activeSessions.map { session ->
            TOTPAccessorInfo(
                requestingUsername = session["requestingUsername"] as? String,
                verifiedAt = session["verifiedAt"] as String,
                expiresAt = session["expiresAt"] as? String,
                remainingSeconds = session["remainingSeconds"] as? Int,
                accessType = session["accessType"] as String
            )
        }
        
        return TOTPControlPanelResponse(
            activeSessions = totpAccessors
        )
    }
    
    /**
     * Revoke access for a user
     */
    suspend fun revokeAccess(requestingUserId: String,targetUserId: String): Boolean {
        if (targetUserId.isBlank()) {
            throw IllegalArgumentException("Target user ID is required")
        }
        if (requestingUserId.isBlank()) {
            throw IllegalArgumentException("Requesting user ID is required")
        }
        
        return repository.revokeAccess(requestingUserId,targetUserId)
    }
    
    /**
     * Grant access to a user without requiring TOTP verification
     */
    suspend fun grantAccessWithoutTOTP(
        targetUserId: String,
        username: String,
        durationSeconds: Int? = null
    ): ExtendAccessResponse {
        if (targetUserId.isBlank()) {
            throw IllegalArgumentException("Target user ID is required")
        }
        if (username.isBlank()) {
            throw IllegalArgumentException("Username is required")
        }
        
        // Get user ID by username
        val requestingUserId = repository.getUserIdByUsername(username)
            ?: throw IllegalArgumentException("User not found")
        
        // Prevent self-granting
        if (targetUserId == requestingUserId) {
            throw IllegalArgumentException("Cannot grant access to yourself")
        }

        val targetUserName = repository.getUserById(targetUserId)?.username;


        // Validate and set duration (default: 1 day, max: 1 year)
        val defaultDuration = 86400 // 1 day
        val maxDuration = 31536000 // 1 year
        
        val sessionDuration = when {
            durationSeconds == null -> defaultDuration
            durationSeconds > maxDuration -> maxDuration
            durationSeconds <= 0 -> throw IllegalArgumentException("Duration must be greater than 0")
            else -> durationSeconds
        }
        
        // Create session without TOTP verification
        repository.createTOTPVerificationSession(
            requestingUserId = requestingUserId,
            targetUserId = targetUserId,
            targetUsername = targetUserName,
            durationSeconds = sessionDuration
        )
        
        // Calculate session expiration
        val now = kotlinx.datetime.Clock.System.now()
        val sessionExpiresAt = kotlinx.datetime.Instant.fromEpochMilliseconds(
            now.toEpochMilliseconds() + (sessionDuration * 1000)
        )
        
        val remainingSeconds = sessionDuration
        
        // Format duration message
        val durationMessage = when {
            sessionDuration < 3600 -> "${sessionDuration / 60} minute(s)"
            sessionDuration < 86400 -> "${sessionDuration / 3600} hour(s)"
            sessionDuration < 31536000 -> "${sessionDuration / 86400} day(s)"
            else -> "${sessionDuration / 31536000} year(s)"
        }
        
        return ExtendAccessResponse(
            success = true,
            message = "Access granted without TOTP verification for $durationMessage.",
            expiresAt = sessionExpiresAt.toString(),
            remainingSeconds = remainingSeconds
        )
    }
    
    /**
     * Extend access time for a session
     */
    suspend fun extendAccessTime(
        targetUserId: String,
        requestingUserId: String,
        additionalSeconds: Int
    ): ExtendAccessResponse {
        if (targetUserId.isBlank()) {
            throw IllegalArgumentException("Target user ID is required")
        }
        if (requestingUserId.isBlank()) {
            throw IllegalArgumentException("Requesting user ID is required")
        }
        if (additionalSeconds <= 0) {
            throw IllegalArgumentException("Additional seconds must be greater than 0")
        }
        
        // Maximum extension: 7 days (604800 seconds)
        val maxAdditionalSeconds = 604800
        val actualAdditionalSeconds = if (additionalSeconds > maxAdditionalSeconds) {
            maxAdditionalSeconds
        } else {
            additionalSeconds
        }
        
        val result = repository.extendAccessTime(targetUserId, requestingUserId, actualAdditionalSeconds)
        
        return if (result != null) {
            ExtendAccessResponse(
                success = true,
                message = "Access time extended successfully",
                expiresAt = result["newExpiresAt"] as? String,
                remainingSeconds = result["remainingSeconds"] as? Int
            )
        } else {
            ExtendAccessResponse(
                success = false,
                message = "No active session found to extend",
                expiresAt = null,
                remainingSeconds = null
            )
        }
    }
}

