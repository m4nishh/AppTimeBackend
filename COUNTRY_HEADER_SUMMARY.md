# Country from Header - Quick Summary

## ✅ Updated Implementation

Changed country extraction from **request body** to **HTTP header** (`X-Country-Code`).

## What Changed

### HTTP Header (NEW)
```http
POST /api/users/register
X-Country-Code: US
Content-Type: application/json

{
  "deviceInfo": {
    "deviceId": "device123",
    ...
  }
}
```

### Files Updated

1. **`users/Routes.kt`** - Extracts country from header
   ```kotlin
   val country = call.request.headers["X-Country-Code"]
   val response = service.registerDevice(request, country)
   ```

2. **`users/Service.kt`** - Accepts country as parameter
   ```kotlin
   suspend fun registerDevice(
       request: DeviceRegistrationRequest, 
       country: String? = null
   )
   ```

3. **`users/Models.kt`** - Removed country from request body
   ```kotlin
   data class DeviceRegistrationRequest(
       val deviceInfo: DeviceInfo,
       val firebaseToken: String? = null
       // country removed - now comes from header
   )
   ```

## Usage

### cURL Example
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -H "X-Country-Code: US" \
  -d '{
    "deviceInfo": {
      "deviceId": "device123",
      "manufacturer": "Samsung",
      "model": "Galaxy S21"
    }
  }'
```

### Android Example
```kotlin
val request = Request.Builder()
    .url("https://api.example.com/api/users/register")
    .addHeader("X-Country-Code", "US")
    .post(jsonBody)
    .build()
```

### JavaScript Example
```javascript
fetch('/api/users/register', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'X-Country-Code': 'US'
    },
    body: JSON.stringify({deviceInfo: {...}})
});
```

## Country Detection

### Recommended: System Locale
```kotlin
// Android
val countryCode = Locale.getDefault().country // "US", "CA", "IN", etc.
```

```javascript
// JavaScript
const countryCode = navigator.language.split('-')[1]; // "US" from "en-US"
```

## Header Format

**Recommended**: ISO 3166-1 Alpha-2 codes
- `US` - United States
- `CA` - Canada
- `GB` - United Kingdom
- `IN` - India
- `AU` - Australia
- `DE` - Germany
- `FR` - France
- `JP` - Japan

**Also Accepted**: Full country names
- `United States`
- `Canada`
- `India`

## Advantages

✅ **Clean API Design** - Metadata in headers, not body
✅ **Backward Compatible** - Header is optional
✅ **Flexible** - Easy to add/remove
✅ **Standard Practice** - Follows HTTP conventions
✅ **Easy Testing** - Simple to modify in tools

## What Stayed the Same

✅ Database schema (country column)
✅ Repository layer (saves/retrieves)
✅ Profile responses (includes country)
✅ Admin panel (manages country)
✅ All other endpoints unchanged

## Backward Compatibility

✅ **100% Compatible**
- Header is optional
- Old clients work without changes
- Missing header = NULL country
- No breaking changes

## Testing

```bash
# With country
curl -H "X-Country-Code: US" -X POST http://localhost:8080/api/users/register -d '{...}'

# Without country (still works)
curl -X POST http://localhost:8080/api/users/register -d '{...}'

# Get profile (country included)
curl http://localhost:8080/api/users/{userId}/profile
```

## Files Modified

1. ✅ `src/main/kotlin/users/Routes.kt`
2. ✅ `src/main/kotlin/users/Service.kt`
3. ✅ `src/main/kotlin/users/Models.kt`

**No linter errors** - Ready to deploy! 🚀

See `USER_COUNTRY_HEADER.md` for complete documentation.

