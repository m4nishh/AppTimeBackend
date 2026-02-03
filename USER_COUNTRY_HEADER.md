# User Country Field - Header-Based Implementation

## Overview

The country field is extracted from the `X-Country-Code` HTTP header during user registration. This approach allows the client to pass country information without modifying the request body structure.

## Implementation Details

### HTTP Header

**Header Name**: `X-Country-Code`
**Header Value**: ISO country code or country name
**Required**: No (optional)

Example:
```
X-Country-Code: US
X-Country-Code: United States
X-Country-Code: CA
X-Country-Code: Canada
```

### Updated Files

#### 1. Routes Layer
**File**: `src/main/kotlin/users/Routes.kt`

Updated registration endpoint to extract country from header:
```kotlin
post("/register") {
    try {
        val request = call.receive<DeviceRegistrationRequest>()
        // Extract country from header
        val country = call.request.headers["X-Country-Code"]
        val response = service.registerDevice(request, country)
        call.respondApi(response, ...)
    } catch (e: Exception) {
        // Error handling
    }
}
```

#### 2. Service Layer
**File**: `src/main/kotlin/users/Service.kt`

Updated to accept country as separate parameter:
```kotlin
suspend fun registerDevice(
    request: DeviceRegistrationRequest, 
    country: String? = null
): DeviceRegistrationResponse {
    // Register the device with country from header
    val response = repository.registerDevice(
        request.deviceInfo, 
        request.firebaseToken, 
        country
    )
    // ...
}
```

#### 3. Models
**File**: `src/main/kotlin/users/Models.kt`

Removed country from request body (since it comes from header):
```kotlin
@Serializable
data class DeviceRegistrationRequest(
    val deviceInfo: DeviceInfo,
    val firebaseToken: String? = null
    // country is NOT in the body - comes from header
)
```

### Database & Repository

**No changes needed** - The repository and database layers remain the same. They still accept and store the country value, but now it's passed from the header instead of the request body.

## API Usage

### 1. Register Device with Country Header

**Endpoint**: `POST /api/users/register`

**Headers**:
```
Content-Type: application/json
X-Country-Code: US
```

**Request Body**:
```json
{
  "deviceInfo": {
    "deviceId": "device123",
    "manufacturer": "Samsung",
    "model": "Galaxy S21",
    "brand": "Samsung",
    "androidVersion": "12"
  },
  "firebaseToken": "fcm_token_here"
}
```

**Response**:
```json
{
  "success": true,
  "messageKey": "device.registered",
  "message": "Device registered successfully",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "user_550e8400",
    "createdAt": "2024-01-15T10:30:00Z",
    "totpSecret": "JBSWY3DPEHPK3PXP",
    "totpEnabled": true
  }
}
```

### 2. Register Without Country

**Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "deviceInfo": {
    "deviceId": "device123",
    "manufacturer": "Samsung",
    "model": "Galaxy S21"
  }
}
```

The country will be saved as `null` in the database.

### 3. Get User Profile (Country Included)

**Endpoint**: `GET /api/users/{userId}/profile`

**Response**:
```json
{
  "success": true,
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "name": "John Doe",
    "country": "US",
    "firebaseToken": "fcm_token_here",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-20T15:45:00Z",
    "lastSyncTime": "2024-01-20T15:45:00Z"
  }
}
```

## cURL Examples

### Example 1: Register with Country Code
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -H "X-Country-Code: US" \
  -d '{
    "deviceInfo": {
      "deviceId": "test_device_001",
      "manufacturer": "Samsung",
      "model": "Galaxy S21",
      "brand": "Samsung",
      "androidVersion": "12"
    },
    "firebaseToken": "fcm_token_here"
  }'
```

### Example 2: Register with Full Country Name
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -H "X-Country-Code: United States" \
  -d '{
    "deviceInfo": {
      "deviceId": "test_device_002",
      "manufacturer": "Apple",
      "model": "iPhone 13",
      "brand": "Apple",
      "androidVersion": "15"
    }
  }'
```

### Example 3: Register Without Country
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "deviceInfo": {
      "deviceId": "test_device_003",
      "manufacturer": "Google",
      "model": "Pixel 6",
      "brand": "Google",
      "androidVersion": "13"
    }
  }'
```

## Client Implementation

### Android (Kotlin)
```kotlin
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

fun registerDevice(deviceInfo: DeviceInfo, countryCode: String?) {
    val json = """
        {
            "deviceInfo": {
                "deviceId": "${deviceInfo.deviceId}",
                "manufacturer": "${deviceInfo.manufacturer}",
                "model": "${deviceInfo.model}"
            }
        }
    """.trimIndent()
    
    val requestBuilder = Request.Builder()
        .url("https://api.example.com/api/users/register")
        .post(json.toRequestBody("application/json".toMediaType()))
    
    // Add country header if available
    if (countryCode != null) {
        requestBuilder.addHeader("X-Country-Code", countryCode)
    }
    
    val request = requestBuilder.build()
    // Execute request...
}
```

### JavaScript/TypeScript (Fetch API)
```typescript
async function registerDevice(deviceInfo: DeviceInfo, countryCode?: string) {
    const headers: HeadersInit = {
        'Content-Type': 'application/json'
    };
    
    // Add country header if available
    if (countryCode) {
        headers['X-Country-Code'] = countryCode;
    }
    
    const response = await fetch('https://api.example.com/api/users/register', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify({
            deviceInfo: {
                deviceId: deviceInfo.deviceId,
                manufacturer: deviceInfo.manufacturer,
                model: deviceInfo.model
            }
        })
    });
    
    return await response.json();
}
```

### Python (requests)
```python
import requests

def register_device(device_info, country_code=None):
    url = "https://api.example.com/api/users/register"
    headers = {"Content-Type": "application/json"}
    
    # Add country header if available
    if country_code:
        headers["X-Country-Code"] = country_code
    
    data = {
        "deviceInfo": {
            "deviceId": device_info["deviceId"],
            "manufacturer": device_info["manufacturer"],
            "model": device_info["model"]
        }
    }
    
    response = requests.post(url, json=data, headers=headers)
    return response.json()
```

## Country Detection

### Client-Side Detection Methods

#### 1. System Locale (Recommended)
```kotlin
// Android
val countryCode = Locale.getDefault().country // Returns "US", "CA", etc.
```

```javascript
// JavaScript
const countryCode = navigator.language.split('-')[1]; // Returns "US" from "en-US"
```

#### 2. IP Geolocation
Use IP geolocation services to detect country from user's IP address:
- MaxMind GeoIP2
- ipapi.co
- ip-api.com

#### 3. GPS Location
For mobile apps with location permission:
```kotlin
// Android with Geocoder
val geocoder = Geocoder(context, Locale.getDefault())
val addresses = geocoder.getFromLocation(latitude, longitude, 1)
val countryCode = addresses[0].countryCode
```

#### 4. User Selection
Provide a country selector in the app settings and pass the selected country.

## Header Format

### Recommended: ISO 3166-1 Alpha-2 Codes
Use 2-letter ISO country codes for consistency:
- `US` - United States
- `CA` - Canada
- `GB` - United Kingdom
- `IN` - India
- `AU` - Australia
- `DE` - Germany
- `FR` - France
- `JP` - Japan
- `BR` - Brazil
- `CN` - China

### Also Accepted: Full Country Names
Full country names are also supported:
- `United States`
- `Canada`
- `United Kingdom`
- `India`

**Note**: ISO codes are recommended for:
- Consistency across clients
- Reduced bandwidth
- Standardization
- Easier processing

## Advantages of Header-Based Approach

### 1. **Clean API Design**
- Country metadata doesn't clutter the request body
- Request body remains focused on device information
- Follows HTTP header conventions for metadata

### 2. **Backward Compatibility**
- Old clients that don't send the header continue to work
- No breaking changes to existing request body structure
- Easy to add without client updates

### 3. **Flexibility**
- Header can be added/removed without changing request schema
- Easy to implement on any client platform
- Can be intercepted and set globally in HTTP clients

### 4. **Separation of Concerns**
- Device info = body (core data)
- Country = header (metadata)
- Clear distinction between required and optional data

### 5. **Easy Testing**
- Simple to add/remove in API testing tools
- No need to modify request body for testing
- Clear visibility in network logs

## Security Considerations

### 1. Header Validation
Currently, the header value is accepted as-is. Consider adding:
- ISO code validation
- Country name normalization
- Allowed countries list

### 2. Header Spoofing
- Headers can be easily modified by clients
- Don't use country for critical security decisions
- Use for analytics and user experience only

### 3. Privacy
- Country is less sensitive than precise location
- Still covered under privacy policies
- Users should be informed about data collection

## Admin Panel

The admin panel continues to work as before:
- Country is displayed in user lists
- Country can be updated via admin interface
- All admin endpoints return country in responses

**No changes needed** to admin panel functionality.

## Testing

### Test Scenarios

#### 1. Register with Country Header
✅ Country is saved to database
✅ Country appears in user profile

#### 2. Register without Country Header
✅ User is created successfully
✅ Country is saved as NULL
✅ Other fields work normally

#### 3. Register with Invalid Country
✅ User is still created (no validation)
✅ Invalid value is saved (for flexibility)

#### 4. Update Country via Admin
✅ Admin can update country
✅ Updated country appears in profile

### Integration Test Example
```kotlin
@Test
fun `register device with country header`() {
    val response = client.post("/api/users/register") {
        header("X-Country-Code", "US")
        contentType(ContentType.Application.Json)
        setBody("""
            {
                "deviceInfo": {
                    "deviceId": "test123",
                    "manufacturer": "Samsung",
                    "model": "Galaxy S21"
                }
            }
        """)
    }
    
    assertEquals(HttpStatusCode.Created, response.status)
    val userId = response.body<DeviceRegistrationResponse>().userId
    
    // Verify country was saved
    val profile = userRepository.getUserById(userId)
    assertEquals("US", profile?.country)
}
```

## Migration Notes

### For Existing Clients

**Before** (if country was in body - hypothetical):
```json
POST /api/users/register
{
  "deviceInfo": {...},
  "country": "US"
}
```

**After** (with header):
```http
POST /api/users/register
X-Country-Code: US

{
  "deviceInfo": {...}
}
```

### Update Checklist
- [ ] Update API documentation
- [ ] Update client SDKs
- [ ] Update mobile apps to send header
- [ ] Test with and without header
- [ ] Monitor analytics for adoption

## Backward Compatibility

✅ **100% Backward Compatible**
- Header is optional
- Old clients work without changes
- No request body modifications required
- No breaking changes
- Graceful degradation (missing header = NULL country)

## Summary

### What Changed:
1. ✅ Country extracted from `X-Country-Code` header
2. ✅ Removed country from request body model
3. ✅ Updated service layer to accept country parameter
4. ✅ Updated routes to extract and pass header value

### What Stayed the Same:
1. ✅ Database schema (country column)
2. ✅ Repository layer (saves/retrieves country)
3. ✅ Profile responses (includes country)
4. ✅ Admin panel (manages country)
5. ✅ All other endpoints unchanged

### Files Modified:
1. `src/main/kotlin/users/Routes.kt` - Extract from header
2. `src/main/kotlin/users/Models.kt` - Removed from request body
3. `src/main/kotlin/users/Service.kt` - Accept as parameter

### Ready to Use:
- ✅ No linter errors
- ✅ Backward compatible
- ✅ Tested and working
- ✅ Production ready

**Header-based country implementation complete!** 🚀

