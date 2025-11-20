# User Search API

## Endpoint
```
GET /api/users/search
```

## Description
Search for users by name or username. Returns up to 10 matching users. Case-insensitive partial match.

**Public endpoint** - No authentication required.

---

## Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `q` | String | Yes | Search query (minimum 2 characters) |

---

## Request Examples

### Basic Search
```bash
curl -X GET 'http://localhost:8080/api/users/search?q=john'
```

### Search with Multiple Words
```bash
curl -X GET 'http://localhost:8080/api/users/search?q=john%20doe'
```

### Search by Username
```bash
curl -X GET 'http://localhost:8080/api/users/search?q=user_abc'
```

---

## Response Format

### Success Response (200 OK)
```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "userId": "61a7126f-e1d9-58dd-b79e-333928e83f03",
      "username": "john_doe",
      "email": "john@example.com",
      "name": "John Doe",
      "createdAt": "2024-01-15T10:00:00Z",
      "isActive": true
    },
    {
      "userId": "abc123-def456-ghi789-jkl012-mno345",
      "username": "john_smith",
      "email": null,
      "name": "John Smith",
      "createdAt": "2024-01-14T08:30:00Z",
      "isActive": false
    }
  ],
  "message": "Users found: 2",
  "timestamp": "2024-01-15T16:00:00Z"
}
```

### Empty Results
```json
{
  "success": true,
  "status": 200,
  "data": [],
  "message": "Users found: 0",
  "timestamp": "2024-01-15T16:00:00Z"
}
```

### Error Response (400 Bad Request)
```json
{
  "success": false,
  "status": 400,
  "data": null,
  "message": "Search query parameter 'q' is required",
  "error": {
    "code": "BAD_REQUEST",
    "message": "Search query parameter 'q' is required"
  },
  "timestamp": "2024-01-15T16:00:00Z"
}
```

---

## Search Behavior

1. **Case-Insensitive**: Searches are case-insensitive
   - `q=john` matches "John", "JOHN", "john", "Johnny"

2. **Partial Match**: Matches partial strings
   - `q=john` matches "John Doe", "Johnny", "john_smith"

3. **Multiple Fields**: Searches in both `name` and `username` fields
   - If query matches either name or username, user is included

4. **Limit**: Returns maximum 10 results

5. **Minimum Length**: Query must be at least 2 characters

---

## Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `userId` | String | Unique user identifier |
| `username` | String? | User's username (nullable) |
| `email` | String? | User's email (nullable) |
| `name` | String? | User's display name (nullable) |
| `createdAt` | String | Account creation timestamp |
| `isActive` | Boolean? | Whether user has synced data (has lastSyncTime) |

---

## Examples

### Example 1: Search by Name
**Request:**
```bash
curl -X GET 'http://localhost:8080/api/users/search?q=alice'
```

**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "username": "alice_wonder",
      "email": "alice@example.com",
      "name": "Alice Wonder",
      "createdAt": "2024-01-10T12:00:00Z",
      "isActive": true
    }
  ],
  "message": "Users found: 1"
}
```

### Example 2: Search by Username
**Request:**
```bash
curl -X GET 'http://localhost:8080/api/users/search?q=user_abc'
```

**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "userId": "61a7126f-e1d9-58dd-b79e-333928e83f03",
      "username": "user_abc123",
      "email": null,
      "name": null,
      "createdAt": "2024-01-15T10:00:00Z",
      "isActive": false
    }
  ],
  "message": "Users found: 1"
}
```

### Example 3: No Results
**Request:**
```bash
curl -X GET 'http://localhost:8080/api/users/search?q=xyz12345'
```

**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": [],
  "message": "Users found: 0"
}
```

---

## Error Cases

### Missing Query Parameter
**Request:**
```bash
curl -X GET 'http://localhost:8080/api/users/search'
```

**Response:**
```json
{
  "success": false,
  "status": 400,
  "message": "Search query parameter 'q' is required"
}
```

### Query Too Short
**Request:**
```bash
curl -X GET 'http://localhost:8080/api/users/search?q=a'
```

**Response:**
```json
{
  "success": false,
  "status": 400,
  "message": "Search query must be at least 2 characters"
}
```

---

## Notes

1. **No Authentication Required**: This is a public endpoint
2. **Performance**: Search is optimized with database indexes
3. **Results Limit**: Maximum 10 users returned per search
4. **Search Fields**: Searches in both `name` and `username` fields
5. **Case-Insensitive**: All searches are case-insensitive
6. **Partial Matching**: Supports partial string matching

---

## Quick Reference

```bash
# Search for users
curl -X GET 'http://localhost:8080/api/users/search?q=search_term'
```

