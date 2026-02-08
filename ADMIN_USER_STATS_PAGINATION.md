# Admin User Stats with Pagination and Search

This document describes the new paginated user statistics endpoint with search functionality.

## Endpoint

```
GET /api/admin/users-stats
```

## Description

Retrieve paginated user statistics with optional username search. This endpoint allows administrators to:
- List all users with pagination
- Search users by username (case-insensitive partial match)
- Control page size and navigation

## Query Parameters

| Parameter | Type | Required | Default | Max | Description |
|-----------|------|----------|---------|-----|-------------|
| `username` | string | No | - | - | Search users by username (case-insensitive, partial match) |
| `page` | integer | No | 1 | - | Page number (starts from 1) |
| `pageSize` | integer | No | 20 | 100 | Number of users per page |

## Response Format

```json
{
  "success": true,
  "messageKey": "users.retrieved",
  "message": "Users retrieved successfully",
  "data": {
    "users": [
      {
        "userId": "user123",
        "username": "john_doe",
        "deviceId": "device456",
        "deviceModel": "Samsung Galaxy S21",
        "createdAt": "2024-01-15T10:30:00Z",
        "lastSyncTime": "2024-01-20T15:45:00Z"
      }
    ],
    "totalCount": 150,
    "page": 1,
    "pageSize": 20,
    "totalPages": 8
  }
}
```

## Response Fields

- `users`: Array of user summaries
  - `userId`: Unique user identifier
  - `username`: Username (null if not set)
  - `deviceId`: Device identifier
  - `deviceModel`: Device model name (null if not available)
  - `createdAt`: User registration timestamp
  - `lastSyncTime`: Last sync timestamp (null if never synced)
- `totalCount`: Total number of users matching the search criteria
- `page`: Current page number
- `pageSize`: Number of users per page
- `totalPages`: Total number of pages available

## Examples

### 1. Get first page of all users (default)

```bash
curl -X GET "http://localhost:8080/api/admin/users-stats"
```

### 2. Get first page with custom page size

```bash
curl -X GET "http://localhost:8080/api/admin/users-stats?pageSize=50"
```

### 3. Get second page

```bash
curl -X GET "http://localhost:8080/api/admin/users-stats?page=2&pageSize=20"
```

### 4. Search users by username

```bash
curl -X GET "http://localhost:8080/api/admin/users-stats?username=john"
```

This will return all users whose username contains "john" (case-insensitive).

### 5. Search with pagination

```bash
curl -X GET "http://localhost:8080/api/admin/users-stats?username=test&page=1&pageSize=10"
```

### 6. Search for users with exact username pattern

```bash
curl -X GET "http://localhost:8080/api/admin/users-stats?username=admin"
```

## Validation Rules

1. **Page Number**: Must be greater than 0. If invalid or not provided, defaults to 1.
2. **Page Size**: 
   - Must be between 1 and 100
   - If less than 1, defaults to 20
   - If greater than 100, capped at 100
3. **Username Search**: 
   - Case-insensitive
   - Partial match (uses SQL LIKE with wildcards)
   - Empty or null username returns all users

## Use Cases

### Administrator Dashboard
Display a paginated list of all registered users with search capability.

```javascript
// Frontend example
const fetchUsers = async (page = 1, searchTerm = '') => {
  const params = new URLSearchParams({
    page: page.toString(),
    pageSize: '20'
  });
  
  if (searchTerm) {
    params.append('username', searchTerm);
  }
  
  const response = await fetch(`/api/admin/users-stats?${params}`);
  const data = await response.json();
  return data.data;
};
```

### User Search
Quickly find users by username for moderation or support.

```javascript
const searchUsers = async (searchTerm) => {
  const response = await fetch(
    `/api/admin/users-stats?username=${encodeURIComponent(searchTerm)}&pageSize=50`
  );
  const data = await response.json();
  return data.data.users;
};
```

### Bulk Operations
Iterate through all users in batches for bulk operations.

```javascript
const processAllUsers = async () => {
  let page = 1;
  let hasMore = true;
  
  while (hasMore) {
    const response = await fetch(`/api/admin/users-stats?page=${page}&pageSize=100`);
    const data = await response.json();
    const { users, totalPages } = data.data;
    
    // Process users batch
    await processUserBatch(users);
    
    hasMore = page < totalPages;
    page++;
  }
};
```

## Performance Considerations

1. **Indexing**: The search uses the `username` field with LIKE query. Ensure proper database indexing for optimal performance.
2. **Page Size**: While the maximum page size is 100, smaller page sizes (20-50) are recommended for better performance and user experience.
3. **Search Optimization**: The username search uses lowercase conversion and wildcard matching. Consider using full-text search for very large datasets.

## Comparison with Existing Endpoints

### Original `/api/admin/stats` Endpoint
- Returns comprehensive admin statistics including user stats
- User data limited to recent 10 registrations
- No pagination or search
- Best for dashboard overview

### New `/api/admin/users-stats` Endpoint
- Focused on user listing and search
- Supports pagination for large user bases
- Allows username filtering
- Best for user management and search

### Existing `/api/admin/users` Endpoint
- Full CRUD operations on individual users
- Supports limit/offset pagination but no search
- More detailed user information
- Best for user administration

## Future Enhancements

Potential improvements for future versions:

1. Additional search filters:
   - Search by device ID
   - Filter by registration date range
   - Filter by last sync time
   - Filter by TOTP status
   
2. Sorting options:
   - Sort by username
   - Sort by registration date
   - Sort by last activity
   
3. Export functionality:
   - Export search results to CSV
   - Bulk export with filters
   
4. Advanced search:
   - Multiple field search
   - Regular expression support
   - Full-text search

## Error Handling

### Invalid Page Number
```json
{
  "success": true,
  "data": {
    "page": 1,
    "totalCount": 0,
    "users": [],
    "pageSize": 20,
    "totalPages": 0
  }
}
```
Note: Invalid page numbers are automatically corrected to valid values.

### No Results Found
```json
{
  "success": true,
  "data": {
    "users": [],
    "totalCount": 0,
    "page": 1,
    "pageSize": 20,
    "totalPages": 0
  }
}
```

### Server Error
```json
{
  "success": false,
  "messageKey": "users.failed",
  "message": "Failed to retrieve paginated user stats: [error details]"
}
```

