# Admin User Stats - Quick Start Guide

## ✅ Implementation Complete

Pagination and username search have been successfully added to the Admin User Stats feature!

## What Was Added

### 1. **New Endpoint**: `GET /api/admin/users-stats`

This dedicated endpoint provides:
- ✅ Pagination support (page-based navigation)
- ✅ Username search (case-insensitive, partial match)
- ✅ Configurable page size (1-100, default: 20)
- ✅ Total count and pagination metadata

### 2. **Modified Files**

- `src/main/kotlin/admin/Models.kt` - Added `PaginatedUserStats` and `UserSearchQuery` models
- `src/main/kotlin/admin/Repository.kt` - Added `getUsersWithPagination()` method
- `src/main/kotlin/admin/Service.kt` - Added service layer with validation
- `src/main/kotlin/admin/Routes.kt` - Added new endpoint route

### 3. **Documentation Created**

- `ADMIN_USER_STATS_PAGINATION.md` - Complete API documentation
- `ADMIN_USER_STATS_CURL.md` - cURL examples and testing guide
- `ADMIN_USER_STATS_SUMMARY.md` - Technical implementation details
- `ADMIN_USER_STATS_QUICKSTART.md` - This file

## Quick Test

### Start the Server

```bash
# In the project directory
./gradlew run
```

Or if there's a port conflict, find and kill the process:

```bash
# Find what's using port 8080
lsof -i :8080

# Kill the process (replace PID with actual process ID)
kill -9 <PID>

# Then start the server
./gradlew run
```

### Test the New Endpoint

Once the server is running, try these commands:

#### 1. Get All Users (First Page)
```bash
curl http://localhost:8080/api/admin/users-stats | jq
```

#### 2. Search for a User
```bash
curl "http://localhost:8080/api/admin/users-stats?username=test" | jq
```

#### 3. Navigate Pages
```bash
curl "http://localhost:8080/api/admin/users-stats?page=2&pageSize=10" | jq
```

#### 4. Combined Search and Pagination
```bash
curl "http://localhost:8080/api/admin/users-stats?username=john&page=1&pageSize=20" | jq
```

### Expected Response Format

```json
{
  "success": true,
  "messageKey": "users.retrieved",
  "message": "Users retrieved successfully",
  "data": {
    "users": [
      {
        "userId": "550e8400-e29b-41d4-a716-446655440000",
        "username": "john_doe",
        "deviceId": "device_12345",
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

## API Reference

### Query Parameters

| Parameter | Type | Required | Default | Max | Description |
|-----------|------|----------|---------|-----|-------------|
| username | string | No | - | - | Search term (case-insensitive, partial match) |
| page | integer | No | 1 | - | Page number (minimum 1) |
| pageSize | integer | No | 20 | 100 | Results per page |

### Features

- ✅ **Pagination**: Navigate through large user lists
- ✅ **Search**: Find users by username (partial, case-insensitive)
- ✅ **Validation**: Automatic parameter validation and correction
- ✅ **Metadata**: Total count and page information included
- ✅ **Sorting**: Results ordered by creation date (newest first)

### Validation

- Page < 1 → defaults to 1
- PageSize < 1 → defaults to 20
- PageSize > 100 → capped at 100
- Empty username → returns all users

## Integration Examples

### JavaScript/TypeScript

```javascript
async function getUserStats(options = {}) {
  const { username, page = 1, pageSize = 20 } = options;
  
  const params = new URLSearchParams({ page, pageSize });
  if (username) params.append('username', username);
  
  const response = await fetch(`/api/admin/users-stats?${params}`);
  const data = await response.json();
  
  return data.data; // Returns { users, totalCount, page, pageSize, totalPages }
}

// Usage examples
const allUsers = await getUserStats();
const searchResults = await getUserStats({ username: 'john' });
const page2 = await getUserStats({ page: 2, pageSize: 50 });
```

### React Component Example

```javascript
function UserList() {
  const [users, setUsers] = React.useState([]);
  const [page, setPage] = React.useState(1);
  const [totalPages, setTotalPages] = React.useState(0);
  const [search, setSearch] = React.useState('');
  
  React.useEffect(() => {
    fetchUsers();
  }, [page, search]);
  
  async function fetchUsers() {
    const params = new URLSearchParams({ page, pageSize: 20 });
    if (search) params.append('username', search);
    
    const response = await fetch(`/api/admin/users-stats?${params}`);
    const data = await response.json();
    
    setUsers(data.data.users);
    setTotalPages(data.data.totalPages);
  }
  
  return (
    <div>
      <input 
        type="text" 
        placeholder="Search by username..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />
      
      <ul>
        {users.map(user => (
          <li key={user.userId}>
            {user.username || 'No username'} - {user.deviceModel}
          </li>
        ))}
      </ul>
      
      <div>
        <button onClick={() => setPage(p => Math.max(1, p - 1))}>
          Previous
        </button>
        <span>Page {page} of {totalPages}</span>
        <button onClick={() => setPage(p => Math.min(totalPages, p + 1))}>
          Next
        </button>
      </div>
    </div>
  );
}
```

### Python Example

```python
import requests

def get_user_stats(username=None, page=1, page_size=20):
    url = "http://localhost:8080/api/admin/users-stats"
    params = {"page": page, "pageSize": page_size}
    
    if username:
        params["username"] = username
    
    response = requests.get(url, params=params)
    return response.json()["data"]

# Usage
all_users = get_user_stats()
search_results = get_user_stats(username="john")
page_2 = get_user_stats(page=2, page_size=50)
```

## Testing Checklist

- [ ] Server starts without errors
- [ ] GET `/api/admin/users-stats` returns paginated results
- [ ] Search by username works (case-insensitive)
- [ ] Pagination navigation works (page parameter)
- [ ] Page size can be customized
- [ ] Total count is correct
- [ ] Total pages is calculated correctly
- [ ] Invalid parameters are auto-corrected
- [ ] Empty results return proper structure
- [ ] Results are ordered by creation date (newest first)

## Performance Optimization (Optional)

For optimal performance with large user databases, create these database indexes:

```sql
-- Index on username for search
CREATE INDEX IF NOT EXISTS idx_users_username_lower ON users(LOWER(username));

-- Index on created_at for ordering
CREATE INDEX IF NOT EXISTS idx_users_created_at_desc ON users(created_at DESC);

-- Composite index (optional, for combined operations)
CREATE INDEX IF NOT EXISTS idx_users_search_order ON users(LOWER(username), created_at DESC);
```

## Troubleshooting

### Issue: Port 8080 already in use

**Solution:**
```bash
# Find the process
lsof -i :8080

# Kill it
kill -9 <PID>

# Or use the pkill command
pkill -9 -f gradle
```

### Issue: No results returned

**Solution:**
- Check if users exist in the database
- Verify the search term is correct
- Try without the username parameter to get all users

### Issue: Pagination not working

**Solution:**
- Check the totalPages value in the response
- Ensure page number is within valid range (1 to totalPages)
- Verify pageSize is between 1 and 100

## Next Steps

1. **Test the endpoint** with various parameters
2. **Integrate with your admin frontend** (if applicable)
3. **Add database indexes** for better performance (optional)
4. **Monitor performance** with your actual data size

## Additional Resources

- **Full Documentation**: `ADMIN_USER_STATS_PAGINATION.md`
- **cURL Examples**: `ADMIN_USER_STATS_CURL.md`
- **Technical Details**: `ADMIN_USER_STATS_SUMMARY.md`

## Summary

✅ **Complete** - Pagination and search functionality successfully added to UserStats!

The new endpoint provides:
- Efficient pagination for large user lists
- Fast username search with partial matching
- Comprehensive metadata (total count, pages, etc.)
- Robust validation and error handling
- Clean, consistent API design

Start the server and test it out! 🚀

