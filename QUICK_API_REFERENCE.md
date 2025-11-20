# Quick API Reference

## Test User ID
```
61a7126f-e1d9-58dd-b79e-333928e83f03
```

Use this as the Bearer token in all authenticated requests.

---

## Focus API

### Submit Focus Sessions (Array)

```bash
curl -X POST 'http://localhost:8080/api/focus/submit' \
-H 'Content-Type: application/json' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03' \
-d '{
  "sessions": [
    {
      "focusDuration": 1800000,
      "startTime": "2024-01-15T10:00:00Z",
      "endTime": "2024-01-15T10:30:00Z",
      "sessionType": "work"
    }
  ]
}'
```

### Get All Focus Sessions

```bash
curl -X GET 'http://localhost:8080/api/focus/history' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

### Get Focus Sessions by Time Range

```bash
curl -X GET 'http://localhost:8080/api/focus/history?startDate=2024-01-15T00:00:00Z&endDate=2024-01-15T23:59:59Z' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

### Get Focus Stats

```bash
curl -X GET 'http://localhost:8080/api/focus/stats' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

---

## Consents API

### Get All Consent Templates

```bash
curl -X GET 'http://localhost:8080/api/consents'
```

### Submit Consents

```bash
curl -X POST 'http://localhost:8080/api/consents' \
-H 'Content-Type: application/json' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03' \
-d '{
  "consents": [
    {
      "id": 1,
      "value": "accepted"
    }
  ]
}'
```

### Get User Consents

```bash
curl -X GET 'http://localhost:8080/api/consents/user' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

---

## User API

### Register Device

```bash
curl -X POST 'http://localhost:8080/api/users/register' \
-H 'Content-Type: application/json' \
-d '{
  "deviceInfo": {
    "deviceId": "unique-device-id-12345",
    "manufacturer": "Samsung",
    "model": "Galaxy S21"
  }
}'
```

### Search Users (by name or username)

```bash
curl -X GET 'http://localhost:8080/api/users/search?q=john'
```

### Generate TOTP Code for User (by username)

```bash
curl -X GET 'http://localhost:8080/api/users/john_doe/totp/generate'
```

### Verify TOTP Code for User (by username)

```bash
curl -X POST 'http://localhost:8080/api/users/john_doe/totp/verify' \
-H 'Content-Type: application/json' \
-d '{"code":"123456"}'
```

### Get User Profile with TOTP (by username)

```bash
curl -X POST 'http://localhost:8080/api/users/john_doe/profile' \
-H 'Content-Type: application/json' \
-d '{"code":"123456"}'
```

### Get User Profile

```bash
curl -X GET 'http://localhost:8080/api/v1/user/profile' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03'
```

### Update User Profile (Username Only)

```bash
curl -X PUT 'http://localhost:8080/api/v1/user/profile' \
-H 'Content-Type: application/json' \
-H 'Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03' \
-d '{
  "username": "my_new_username"
}'
```

---

## Base URL
```
http://localhost:8080
```

## Authentication
All protected endpoints require:
```
Authorization: Bearer 61a7126f-e1d9-58dd-b79e-333928e83f03
```

