# Sync Worker Debug Guide

## Problem
The data sync worker is not making API calls even when phone time is changed by 1 hour ahead.

## Debug Endpoint

A new endpoint has been added to help debug sync issues:

### GET `/api/v1/user/sync/status`

**Authentication:** Required (Bearer token)

**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "userId": "user-id-here",
    "lastSyncTime": 1234567890000,
    "lastSyncTimeISO": "2025-11-14T10:30:00Z",
    "currentServerTime": 1234567895000,
    "currentServerTimeISO": "2025-11-14T10:30:05Z",
    "timeSinceLastSync": 5000,
    "timeSinceLastSyncSeconds": 5,
    "timeSinceLastSyncMinutes": 0,
    "hasSyncedBefore": true
  }
}
```

**cURL Example:**
```bash
curl -X GET 'https://your-ngrok-url.ngrok-free.app/api/v1/user/sync/status' \
  -H 'Authorization: Bearer YOUR_USER_ID'
```

## Common Issues and Solutions

### 1. **Time Comparison Issue**
If your Android worker compares client time with server `lastSyncTime`, there might be a timezone or clock skew issue.

**Check:**
- Compare `currentServerTime` from the endpoint with your device's `System.currentTimeMillis()`
- If there's a significant difference (> 5 minutes), there's a clock skew issue

**Solution:**
- Always use server time for comparison, not device time
- The endpoint provides `currentServerTime` which you can use as reference

### 2. **Sync Time Not Updated**
If `lastSyncTime` is `null` or very old, the worker might think it already synced.

**Check:**
- Call `/api/v1/user/sync/status` to see the current `lastSyncTime`
- Check if `hasSyncedBefore` is `false` (means never synced)

**Solution:**
- Make sure your sync API calls are actually reaching the server
- Check server logs for any errors during sync

### 3. **Sync Interval Logic**
If your worker checks "if (currentTime - lastSyncTime > 1 hour)", changing phone time by 1 hour might not trigger sync if:
- The worker uses device time instead of server time
- The comparison is done before the time change

**Solution:**
- Use server time for all time comparisons
- Call `/api/v1/user/sync/status` to get server time
- Compare `timeSinceLastSyncMinutes` from the endpoint

### 4. **Network/API Issues**
The worker might not be making API calls due to:
- Network connectivity issues
- API endpoint URL changes
- Authentication token expiration

**Check:**
- Verify the worker can reach the server
- Check if authentication token is valid
- Test the sync endpoint manually with cURL

## Recommended Sync Worker Logic

```kotlin
// Pseudo-code for Android worker
suspend fun shouldSync(): Boolean {
    // Get sync status from server
    val syncStatus = apiClient.getSyncStatus()
    
    // Use server time for comparison
    val timeSinceLastSyncMinutes = syncStatus.timeSinceLastSyncMinutes
    
    // Sync if:
    // 1. Never synced before, OR
    // 2. More than 1 hour (60 minutes) since last sync
    return !syncStatus.hasSyncedBefore || 
           (timeSinceLastSyncMinutes != null && timeSinceLastSyncMinutes >= 60)
}
```

## Testing Sync

1. **Check current sync status:**
   ```bash
   curl -X GET 'YOUR_URL/api/v1/user/sync/status' \
     -H 'Authorization: Bearer YOUR_USER_ID'
   ```

2. **Manually trigger sync:**
   ```bash
   curl -X POST 'YOUR_URL/api/usage/events/batch' \
     -H 'Authorization: Bearer YOUR_USER_ID' \
     -H 'Content-Type: application/json' \
     -d '{
       "syncTime": "2025-11-14T10:30:00Z",
       "events": [...]
     }'
   ```

3. **Check sync status again** to verify `lastSyncTime` was updated

## Notes

- `lastSyncTime` is updated automatically when you submit events via:
  - `POST /api/usage/events` (single event)
  - `POST /api/usage/events/batch` (batch events)
- The `syncTime` in the request body is what gets stored as `lastSyncTime`
- All times are in milliseconds (Unix timestamp)
- ISO 8601 format is used for human-readable times

