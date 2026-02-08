# Notification Image Support & Error Handling Fix

## Issues Fixed

### 1. ✅ Images Not Being Sent in Notifications
**Problem:** The notification service was storing image URLs in the database but not sending them via Firebase Cloud Messaging.

**Solution:** Updated `FirebaseNotificationService` to support images:
- Added `image: String?` parameter to `sendNotification()` method
- Added `image: String?` parameter to `sendBulkNotifications()` method
- Used `Notification.builder().setImage(image)` to include images in FCM payload
- Updated `NotificationService` to pass the image parameter when sending push notifications
- Added image to the FCM data payload for client-side access

### 2. ✅ Better Error Handling for UNREGISTERED Tokens
**Problem:** The app was throwing stack traces for UNREGISTERED tokens (expired/invalid Firebase tokens).

**Error:**
```
FirebaseMessagingException: Requested entity was not found.
Error Code: NOT_FOUND
Details: UNREGISTERED
```

**Solution:** Enhanced error handling in `FirebaseNotificationService`:
- Added specific handling for `ErrorCode.NOT_FOUND` (UNREGISTERED tokens)
- Now logs a warning instead of severe error with stack trace
- Logs only the first 20 characters of the invalid token for security
- Returns `false` gracefully without crashing

---

## Changes Made

### File: `FirebaseNotificationService.kt`

#### 1. Updated `sendNotification()` Method Signature
**Before:**
```kotlin
fun sendNotification(
    firebaseToken: String,
    title: String,
    body: String,
    data: Map<String, String> = emptyMap()
): Boolean
```

**After:**
```kotlin
fun sendNotification(
    firebaseToken: String,
    title: String,
    body: String,
    image: String? = null,  // NEW: Optional image parameter
    data: Map<String, String> = emptyMap()
): Boolean
```

#### 2. Added Image Support in Notification Builder
```kotlin
val notificationBuilder = Notification.builder()
    .setTitle(title)
    .setBody(body)

// Add image if provided
if (!image.isNullOrBlank()) {
    notificationBuilder.setImage(image)
    logger.info("📸 Adding image to notification: $image")
}

val notification = notificationBuilder.build()
```

#### 3. Enhanced Error Handling for UNREGISTERED Tokens
**Before:**
```kotlin
when (e.errorCode) {
    ErrorCode.UNKNOWN, ErrorCode.INVALID_ARGUMENT -> {
        // Token is invalid, expired, or app was uninstalled
        logger.warning("⚠️  Invalid or expired FCM token...")
        false
    }
    // ... other cases
    else -> {
        logger.severe("❌ Failed to send notification: ${e.message} (Error Code: ${e.errorCode})")
        e.printStackTrace()  // STACK TRACE FOR ALL UNKNOWN ERRORS
        false
    }
}
```

**After:**
```kotlin
when (e.errorCode) {
    ErrorCode.NOT_FOUND -> {
        // NEW: Specific handling for UNREGISTERED tokens
        logger.warning("⚠️  FCM token UNREGISTERED. The app may have been uninstalled or token expired. Token: ${firebaseToken.take(20)}...")
        // TODO: Consider marking this token as invalid in the database
        false
    }
    ErrorCode.UNKNOWN, ErrorCode.INVALID_ARGUMENT -> {
        // Token is invalid or malformed
        logger.warning("⚠️  Invalid or malformed FCM token. Token may need to be refreshed. Error: ${e.message}")
        false
    }
    // ... other cases
    else -> {
        logger.severe("❌ Failed to send notification: ${e.message} (Error Code: ${e.errorCode})")
        e.printStackTrace()
        false
    }
}
```

#### 4. Updated `sendBulkNotifications()` Method
```kotlin
fun sendBulkNotifications(
    firebaseTokens: List<String>,
    title: String,
    body: String,
    image: String? = null,  // NEW: Optional image parameter
    data: Map<String, String> = emptyMap()
): Int {
    // ...
    if (sendNotification(token, title, body, image, data)) {
        successCount++
    }
    // ...
}
```

### File: `NotificationService.kt`

#### Updated to Pass Image to Firebase
**Before:**
```kotlin
FirebaseNotificationService.sendNotification(
    firebaseToken = firebaseToken,
    title = title,
    body = text,
    data = data
)
```

**After:**
```kotlin
if (image != null) {
    data["image"] = image  // Add to data payload
}

FirebaseNotificationService.sendNotification(
    firebaseToken = firebaseToken,
    title = title,
    body = text,
    image = image,  // NEW: Pass image parameter
    data = data
)
```

---

## How Images Work in FCM Notifications

### Server-Side (Backend)
1. Store image URL in database (already working)
2. Pass image URL to `FirebaseNotificationService.sendNotification()`
3. Firebase sets the image using `Notification.builder().setImage(imageUrl)`
4. Image is sent in both:
   - **Notification payload**: For native display
   - **Data payload**: For custom handling in the app

### Client-Side (Android App)
The Android app receives notifications with images in two ways:

#### 1. Foreground Notifications
```kotlin
// In MyFirebaseMessagingService.kt
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    val imageUrl = remoteMessage.data["image"]
    
    if (imageUrl != null) {
        // Load image and display in notification
        val bitmap = Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .submit()
            .get()
        
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setLargeIcon(bitmap)
            .setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(bitmap))
            // ... other notification settings
    }
}
```

#### 2. Background Notifications
When app is in background, FCM automatically displays the notification with the image using the `notification.image` field.

---

## Firebase Cloud Messaging Payload

### Complete Payload Structure (With Image)
```json
{
  "token": "device_firebase_token",
  "notification": {
    "title": "New Reward! 🎁",
    "body": "You've unlocked a special wallpaper pack!",
    "image": "https://example.com/images/reward-wallpaper.jpg"
  },
  "data": {
    "type": "reward_claimed",
    "notificationId": "12345",
    "deeplink": "apptime://screen/reward_transaction/TXN-789",
    "image": "https://example.com/images/reward-wallpaper.jpg"
  }
}
```

### Image URL Requirements
- Must be a publicly accessible HTTPS URL
- Supported formats: JPEG, PNG, GIF, WebP
- Recommended size: 1200x600px (2:1 aspect ratio)
- Maximum file size: 1MB
- Should load quickly (< 2 seconds)

---

## Testing Images in Notifications

### 1. Test from Admin Panel
```bash
curl -X POST "http://localhost:8080/api/admin/notifications/send" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "title": "New Wallpaper Available! 🎨",
    "text": "Check out our latest collection",
    "type": "new_wallpaper",
    "image": "https://example.com/images/wallpaper-preview.jpg",
    "deeplink": "wallpaper"
  }'
```

### 2. Test Programmatically
```kotlin
// In your Kotlin code
notificationService.sendNewWallpaperNotification(
    userId = "user123",
    collectionName = "Nature Pack",
    wallpaperCount = 10,
    imageUrl = "https://example.com/images/nature-pack-preview.jpg"
)
```

### 3. Expected Results
- ✅ Image appears in the notification (if device supports it)
- ✅ Image URL is stored in database
- ✅ Image URL is in the FCM data payload
- ✅ No errors for UNREGISTERED tokens

---

## Error Code Reference

| Error Code | Meaning | Action Taken |
|------------|---------|--------------|
| `NOT_FOUND` | Token is UNREGISTERED (app uninstalled or token expired) | Log warning, return false |
| `INVALID_ARGUMENT` | Token is malformed or invalid | Log warning, return false |
| `DEADLINE_EXCEEDED` | FCM quota exceeded | Log warning, return false |
| `UNAVAILABLE` | FCM service temporarily down | Log warning, return false |
| Other | Unknown error | Log severe with stack trace |

---

## Future Improvements

### 1. Token Invalidation
Currently, when we detect an UNREGISTERED token, we log it but don't update the database. Consider adding:

```kotlin
// In FirebaseNotificationService.kt
ErrorCode.NOT_FOUND -> {
    logger.warning("⚠️  FCM token UNREGISTERED. Token: ${firebaseToken.take(20)}...")
    
    // TODO: Implement this in UserRepository
    // userRepository.invalidateFirebaseToken(firebaseToken)
    
    false
}
```

### 2. Image Caching
For frequently used images (like wallpaper collections), consider:
- Pre-uploading to a CDN
- Using consistent URLs for caching
- Optimizing image sizes for notifications

### 3. Retry Logic
For `UNAVAILABLE` errors, consider implementing:
- Exponential backoff retry
- Queue failed notifications
- Retry with NotificationQueueService

### 4. Analytics
Track notification metrics:
- Delivery success rate
- UNREGISTERED token frequency
- Image load success/failure
- Click-through rates by notification type

---

## Notes for Android Developers

1. **Image Loading**: Ensure your Android app has proper image loading (Glide/Coil/Picasso)
2. **Notification Permissions**: Android 13+ requires notification permissions
3. **Large Images**: May not display on all devices/Android versions
4. **Network**: Images only load if device has internet connection
5. **Fallback**: Always have a good title/body text for devices that don't show images

---

## Backward Compatibility

✅ **All existing code continues to work** because:
- `image` parameter is optional with default `null`
- Existing calls in `users/Routes.kt` don't need changes
- Notifications without images work exactly as before
- Only enhancement is that images are now supported when provided

---

*Last Updated: January 29, 2026*
*Fixed By: AI Assistant*

