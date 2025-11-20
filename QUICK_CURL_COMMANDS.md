# Quick cURL Commands

## 1. Register User/Device

```bash
curl -X POST "http://localhost:8080/api/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceInfo": {
      "deviceId": "android_device_unique_id_12345",
      "manufacturer": "Samsung",
      "model": "SM-G950F",
      "brand": "samsung",
      "product": "dreamlte",
      "device": "dreamlte",
      "hardware": "samsungexynos8895",
      "androidVersion": "13",
      "sdkVersion": 33
    }
  }'
```

**Save the `userId` from response - it's your Bearer token!**

---

## 2. Get User Profile

```bash
curl -X GET "http://localhost:8080/api/v1/user/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ENCRYPTED_USER_ID"
```

**Example with actual token:**
```bash
curl -X GET "http://localhost:8080/api/v1/user/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03"
```

---

## 3. Update Username

```bash
curl -X PUT "http://localhost:8080/api/v1/user/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ENCRYPTED_USER_ID" \
  -d '{
    "username": "my_new_username"
  }'
```

**Example with actual token:**
```bash
curl -X PUT "http://localhost:8080/api/v1/user/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03" \
  -d '{
    "username": "my_new_username"
  }'
```

---

## Health Check

```bash
curl -X GET "http://localhost:8080/health"
```

