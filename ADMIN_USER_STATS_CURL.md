# Admin User Stats API - cURL Examples

Quick reference for testing the paginated user stats endpoint.

## Base URL
```bash
BASE_URL="http://localhost:8080"
```

## Basic Examples

### 1. Get All Users (First Page, Default Settings)
```bash
curl -X GET "${BASE_URL}/api/admin/users-stats"
```

### 2. Get All Users with Custom Page Size
```bash
curl -X GET "${BASE_URL}/api/admin/users-stats?pageSize=50"
```

### 3. Navigate to Specific Page
```bash
# Page 2
curl -X GET "${BASE_URL}/api/admin/users-stats?page=2"

# Page 3 with 30 users per page
curl -X GET "${BASE_URL}/api/admin/users-stats?page=3&pageSize=30"
```

### 4. Search Users by Username
```bash
# Search for users with "john" in username
curl -X GET "${BASE_URL}/api/admin/users-stats?username=john"

# Search for users with "admin" in username
curl -X GET "${BASE_URL}/api/admin/users-stats?username=admin"

# Search with URL encoding for special characters
curl -X GET "${BASE_URL}/api/admin/users-stats?username=user%20test"
```

### 5. Combined Search and Pagination
```bash
# Search for "test" users, page 2, 10 per page
curl -X GET "${BASE_URL}/api/admin/users-stats?username=test&page=2&pageSize=10"

# Search for "admin" users, page 1, 5 per page
curl -X GET "${BASE_URL}/api/admin/users-stats?username=admin&page=1&pageSize=5"
```

## Pretty Printed JSON Output

### Using jq (if installed)
```bash
# Install jq: brew install jq (macOS) or apt-get install jq (Linux)

# Get users with pretty output
curl -s -X GET "${BASE_URL}/api/admin/users-stats" | jq '.'

# Get only the user list
curl -s -X GET "${BASE_URL}/api/admin/users-stats" | jq '.data.users'

# Get pagination info
curl -s -X GET "${BASE_URL}/api/admin/users-stats" | jq '.data | {totalCount, page, pageSize, totalPages}'

# Search and show only usernames
curl -s -X GET "${BASE_URL}/api/admin/users-stats?username=john" | jq '.data.users[].username'
```

### Using python -m json.tool
```bash
curl -s -X GET "${BASE_URL}/api/admin/users-stats" | python -m json.tool
```

## Advanced Examples

### 6. Get Maximum Users Per Page
```bash
# Maximum page size is 100
curl -X GET "${BASE_URL}/api/admin/users-stats?pageSize=100"
```

### 7. Case-Insensitive Search
```bash
# All of these will match "JohnDoe", "johndoe", "JOHNDOE", etc.
curl -X GET "${BASE_URL}/api/admin/users-stats?username=john"
curl -X GET "${BASE_URL}/api/admin/users-stats?username=JOHN"
curl -X GET "${BASE_URL}/api/admin/users-stats?username=John"
```

### 8. Partial Username Match
```bash
# Will match any username containing "test": test123, mytest, testing, etc.
curl -X GET "${BASE_URL}/api/admin/users-stats?username=test"

# Will match any username containing "user": user1, adminuser, usertest, etc.
curl -X GET "${BASE_URL}/api/admin/users-stats?username=user"
```

### 9. Empty Search (Returns All Users)
```bash
# Empty username parameter returns all users
curl -X GET "${BASE_URL}/api/admin/users-stats?username="
```

## Scripting Examples

### Bash Script: Iterate Through All Pages
```bash
#!/bin/bash

BASE_URL="http://localhost:8080"
PAGE=1
PAGE_SIZE=20

while true; do
    echo "Fetching page $PAGE..."
    
    RESPONSE=$(curl -s -X GET "${BASE_URL}/api/admin/users-stats?page=${PAGE}&pageSize=${PAGE_SIZE}")
    
    # Extract total pages using jq
    TOTAL_PAGES=$(echo "$RESPONSE" | jq -r '.data.totalPages')
    
    echo "Page $PAGE of $TOTAL_PAGES"
    echo "$RESPONSE" | jq '.data.users[] | {userId, username}'
    
    if [ "$PAGE" -ge "$TOTAL_PAGES" ]; then
        break
    fi
    
    PAGE=$((PAGE + 1))
    sleep 1  # Be nice to the server
done
```

### Bash Script: Search and Count Results
```bash
#!/bin/bash

BASE_URL="http://localhost:8080"
SEARCH_TERM="$1"

if [ -z "$SEARCH_TERM" ]; then
    echo "Usage: $0 <search_term>"
    exit 1
fi

RESPONSE=$(curl -s -X GET "${BASE_URL}/api/admin/users-stats?username=${SEARCH_TERM}")

TOTAL_COUNT=$(echo "$RESPONSE" | jq -r '.data.totalCount')
echo "Found $TOTAL_COUNT users matching '$SEARCH_TERM'"

echo "$RESPONSE" | jq -r '.data.users[] | "\(.username) (\(.userId))"'
```

### Bash Script: Get All Users (All Pages)
```bash
#!/bin/bash

BASE_URL="http://localhost:8080"
OUTPUT_FILE="all_users.json"

PAGE=1
PAGE_SIZE=100
ALL_USERS="[]"

echo "Fetching all users..."

while true; do
    RESPONSE=$(curl -s -X GET "${BASE_URL}/api/admin/users-stats?page=${PAGE}&pageSize=${PAGE_SIZE}")
    
    USERS=$(echo "$RESPONSE" | jq -r '.data.users')
    TOTAL_PAGES=$(echo "$RESPONSE" | jq -r '.data.totalPages')
    
    # Append users to array
    ALL_USERS=$(echo "$ALL_USERS" | jq --argjson new "$USERS" '. + $new')
    
    echo "Fetched page $PAGE of $TOTAL_PAGES"
    
    if [ "$PAGE" -ge "$TOTAL_PAGES" ]; then
        break
    fi
    
    PAGE=$((PAGE + 1))
done

echo "$ALL_USERS" | jq '.' > "$OUTPUT_FILE"
echo "Saved $(echo "$ALL_USERS" | jq 'length') users to $OUTPUT_FILE"
```

## Testing Edge Cases

### 10. Invalid Parameters (Auto-corrected)
```bash
# Page 0 -> corrected to page 1
curl -X GET "${BASE_URL}/api/admin/users-stats?page=0"

# Negative page -> corrected to page 1
curl -X GET "${BASE_URL}/api/admin/users-stats?page=-1"

# Page size 0 -> corrected to default (20)
curl -X GET "${BASE_URL}/api/admin/users-stats?pageSize=0"

# Page size > 100 -> capped at 100
curl -X GET "${BASE_URL}/api/admin/users-stats?pageSize=1000"
```

### 11. Non-existent Page
```bash
# Request page 9999 (likely beyond available data)
curl -X GET "${BASE_URL}/api/admin/users-stats?page=9999"
# Returns empty users array with correct pagination info
```

### 12. Special Characters in Search
```bash
# Search with special characters (URL encoded)
curl -X GET "${BASE_URL}/api/admin/users-stats?username=user%40test"  # user@test
curl -X GET "${BASE_URL}/api/admin/users-stats?username=user%2Btest"  # user+test
```

## Response Examples

### Successful Response
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
      },
      {
        "userId": "550e8400-e29b-41d4-a716-446655440001",
        "username": "jane_smith",
        "deviceId": "device_67890",
        "deviceModel": "iPhone 13 Pro",
        "createdAt": "2024-01-16T08:20:00Z",
        "lastSyncTime": "2024-01-21T09:15:00Z"
      }
    ],
    "totalCount": 42,
    "page": 1,
    "pageSize": 20,
    "totalPages": 3
  }
}
```

### Empty Results
```json
{
  "success": true,
  "messageKey": "users.retrieved",
  "message": "Users retrieved successfully",
  "data": {
    "users": [],
    "totalCount": 0,
    "page": 1,
    "pageSize": 20,
    "totalPages": 0
  }
}
```

## Integration with Other Admin Endpoints

### Compare with General Stats
```bash
# Get general admin stats (includes summary user stats)
curl -X GET "${BASE_URL}/api/admin/stats" | jq '.data.users'

# Get paginated user stats (detailed user list)
curl -X GET "${BASE_URL}/api/admin/users-stats" | jq '.data'
```

### Get User Details
```bash
# 1. Search for a user
USER_ID=$(curl -s -X GET "${BASE_URL}/api/admin/users-stats?username=john" | jq -r '.data.users[0].userId')

# 2. Get detailed user information
curl -X GET "${BASE_URL}/api/admin/users/${USER_ID}"
```

## Performance Testing

### Time a Request
```bash
time curl -X GET "${BASE_URL}/api/admin/users-stats?pageSize=100"
```

### Concurrent Requests
```bash
# Test with 10 concurrent requests
for i in {1..10}; do
    curl -X GET "${BASE_URL}/api/admin/users-stats?page=$i" &
done
wait
echo "All requests completed"
```

## Environment Variables for Easy Testing

### Setup
```bash
# Add to ~/.bashrc or ~/.zshrc
export APPTIME_BASE_URL="http://localhost:8080"
export APPTIME_ADMIN_TOKEN="your-admin-token-here"

# Alias for quick testing
alias apptime-users="curl -s -X GET \"\$APPTIME_BASE_URL/api/admin/users-stats\""
alias apptime-search="curl -s -X GET \"\$APPTIME_BASE_URL/api/admin/users-stats?username=\$1\""
```

### Usage
```bash
# List all users
apptime-users | jq '.'

# Search users
apptime-search "john" | jq '.data.users'
```

