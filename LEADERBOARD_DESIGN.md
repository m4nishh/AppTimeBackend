# Leaderboard & Ranking Design

## 🎯 Goal: Fast Leaderboard Queries

Make it easy to calculate:
- Daily leaderboard (top users by screen time today)
- Weekly leaderboard (top users this week)
- Monthly leaderboard (top users this month)
- User's own rank in each period

---

## 📊 Strategy: Pre-Aggregated Summary Table

### Table: `usage_summary` (Pre-aggregated daily stats)

| id | user_id | date | total_screen_time | app_count | last_updated |
|----|---------|------|-------------------|-----------|---------------|
| 1 | user1 | 2024-01-15 | 1800000 | 5 | 2024-01-15 11:00:00 |
| 2 | user2 | 2024-01-15 | 2100000 | 8 | 2024-01-15 11:00:00 |
| 3 | user3 | 2024-01-15 | 1500000 | 3 | 2024-01-15 11:00:00 |

**Benefits:**
- Fast queries (no need to sum thousands of events)
- Easy to calculate rankings
- Can update incrementally as new events arrive

---

## 🗄️ Database Schema Design

### Option 1: Daily Summary Table (Recommended)

```sql
CREATE TABLE usage_summary (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    date DATE NOT NULL,  -- YYYY-MM-DD
    total_screen_time BIGINT DEFAULT 0,  -- milliseconds
    app_count INT DEFAULT 0,
    session_count INT DEFAULT 0,
    last_updated TIMESTAMP DEFAULT NOW(),
    
    UNIQUE(user_id, date),
    INDEX idx_date_time (date, total_screen_time DESC),
    INDEX idx_user_date (user_id, date)
);
```

**How it works:**
- Every time an app CLOSED event is processed, update the summary for that user/date
- Increment `total_screen_time` by the session duration
- Increment `session_count`
- Update `app_count` if new app

---

### Option 2: Materialized View (PostgreSQL)

```sql
CREATE MATERIALIZED VIEW daily_usage_summary AS
SELECT 
    user_id,
    DATE(timestamp) as date,
    SUM(duration_ms) as total_screen_time,
    COUNT(DISTINCT package_name) as app_count,
    COUNT(*) as session_count
FROM app_events
WHERE event_type = 'CLOSED'
GROUP BY user_id, DATE(timestamp);

-- Refresh periodically
REFRESH MATERIALIZED VIEW CONCURRENTLY daily_usage_summary;
```

---

## 📈 Leaderboard Queries

### Query 1: Daily Leaderboard (Top 10)

```sql
SELECT 
    u.user_id,
    u.username,
    us.total_screen_time,
    ROW_NUMBER() OVER (ORDER BY us.total_screen_time DESC) as rank
FROM usage_summary us
JOIN users u ON u.user_id = us.user_id
WHERE us.date = CURRENT_DATE
ORDER BY us.total_screen_time DESC
LIMIT 10;
```

**Result:**
| rank | user_id | username | total_screen_time |
|------|---------|----------|-------------------|
| 1 | user2 | user_abc123 | 2100000 |
| 2 | user1 | user_def456 | 1800000 |
| 3 | user3 | user_ghi789 | 1500000 |

---

### Query 2: Weekly Leaderboard

```sql
SELECT 
    u.user_id,
    u.username,
    SUM(us.total_screen_time) as weekly_total,
    ROW_NUMBER() OVER (ORDER BY SUM(us.total_screen_time) DESC) as rank
FROM usage_summary us
JOIN users u ON u.user_id = us.user_id
WHERE us.date >= DATE_TRUNC('week', CURRENT_DATE)
  AND us.date < DATE_TRUNC('week', CURRENT_DATE) + INTERVAL '7 days'
GROUP BY u.user_id, u.username
ORDER BY weekly_total DESC
LIMIT 10;
```

---

### Query 3: Monthly Leaderboard

```sql
SELECT 
    u.user_id,
    u.username,
    SUM(us.total_screen_time) as monthly_total,
    ROW_NUMBER() OVER (ORDER BY SUM(us.total_screen_time) DESC) as rank
FROM usage_summary us
JOIN users u ON u.user_id = us.user_id
WHERE us.date >= DATE_TRUNC('month', CURRENT_DATE)
  AND us.date < DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month'
GROUP BY u.user_id, u.username
ORDER BY monthly_total DESC
LIMIT 10;
```

---

### Query 4: Get User's Rank

```sql
WITH ranked_users AS (
    SELECT 
        user_id,
        total_screen_time,
        ROW_NUMBER() OVER (ORDER BY total_screen_time DESC) as rank
    FROM usage_summary
    WHERE date = CURRENT_DATE
)
SELECT 
    rank,
    total_screen_time,
    (SELECT COUNT(*) FROM ranked_users) as total_users
FROM ranked_users
WHERE user_id = 'abc123';
```

**Result:**
```json
{
  "rank": 5,
  "totalScreenTime": 1800000,
  "totalUsers": 100
}
```

---

## 🔄 Update Strategy

### When App Event is Processed:

```kotlin
// Pseudo-code
fun processAppEvent(userId: String, event: AppEvent) {
    if (event.type == "CLOSED") {
        val date = event.timestamp.toDate()
        
        // Update summary table
        usageSummary.updateOrInsert(
            userId = userId,
            date = date,
            incrementTime = event.duration,
            incrementSessions = 1
        )
    }
}
```

### SQL Update:

```sql
INSERT INTO usage_summary (user_id, date, total_screen_time, session_count)
VALUES ('user123', '2024-01-15', 622000, 1)
ON CONFLICT (user_id, date)
DO UPDATE SET
    total_screen_time = usage_summary.total_screen_time + EXCLUDED.total_screen_time,
    session_count = usage_summary.session_count + EXCLUDED.session_count,
    last_updated = NOW();
```

---

## 📊 Enhanced Summary Table (More Metrics)

```sql
CREATE TABLE usage_summary (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    
    -- Time metrics
    total_screen_time BIGINT DEFAULT 0,
    average_session_duration BIGINT DEFAULT 0,
    longest_session_duration BIGINT DEFAULT 0,
    
    -- App metrics
    app_count INT DEFAULT 0,
    session_count INT DEFAULT 0,
    most_used_app VARCHAR(255),
    most_used_app_time BIGINT DEFAULT 0,
    
    -- Time distribution
    peak_hour INT,  -- Hour with most usage (0-23)
    
    -- Timestamps
    first_activity TIMESTAMP,
    last_activity TIMESTAMP,
    last_updated TIMESTAMP DEFAULT NOW(),
    
    UNIQUE(user_id, date),
    INDEX idx_date_time (date, total_screen_time DESC),
    INDEX idx_user_date (user_id, date)
);
```

---

## 🚀 Performance Optimizations

### 1. Indexes for Fast Queries

```sql
-- For daily leaderboard
CREATE INDEX idx_daily_leaderboard ON usage_summary(date, total_screen_time DESC);

-- For weekly/monthly (date range queries)
CREATE INDEX idx_date_range ON usage_summary(date);

-- For user's own stats
CREATE INDEX idx_user_stats ON usage_summary(user_id, date DESC);
```

### 2. Partitioning (For Large Scale)

```sql
-- Partition by month for better performance
CREATE TABLE usage_summary (
    ...
) PARTITION BY RANGE (date);

CREATE TABLE usage_summary_2024_01 PARTITION OF usage_summary
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
```

### 3. Caching Strategy

- Cache top 100 users for each period (daily/weekly/monthly)
- Refresh cache every 5-10 minutes
- Store in Redis or in-memory cache

---

## 📱 API Response Examples

### Daily Leaderboard Response

```json
{
  "success": true,
  "data": {
    "period": "daily",
    "date": "2024-01-15",
    "entries": [
      {
        "rank": 1,
        "userId": "user2",
        "username": "user_abc123",
        "totalScreenTime": 2100000,
        "appCount": 8
      },
      {
        "rank": 2,
        "userId": "user1",
        "username": "user_def456",
        "totalScreenTime": 1800000,
        "appCount": 5
      }
    ],
    "userRank": {
      "rank": 5,
      "totalScreenTime": 1500000,
      "totalUsers": 100
    },
    "totalUsers": 100
  }
}
```

---

## 🎯 Recommended Implementation

### Step 1: Store Events (Detailed)
- Keep `app_events` table for complete history
- Every OPEN/CLOSE event stored

### Step 2: Update Summary (Real-time)
- When CLOSED event processed
- Immediately update `usage_summary` table
- Use `ON CONFLICT` for upsert

### Step 3: Query Leaderboard (Fast)
- Query `usage_summary` table (not `app_events`)
- Simple aggregation queries
- Fast response times

### Step 4: Cache Results (Optional)
- Cache top 100 for each period
- Refresh periodically
- Serve from cache for even faster responses

---

## 📋 Example: Complete Flow

### 1. App Sends Event
```json
{
  "fromTime": "2024-01-15T10:05:23Z",
  "toTime": "2024-01-15T10:15:45Z",
  "usage": [{
    "packageName": "com.android.chrome",
    "appName": "Chrome",
    "events": [{
      "type": "CLOSED",
      "timestamp": "2024-01-15T10:15:45Z",
      "sessionId": "session_001"
    }]
  }],
  "totalTime": 622000
}
```

### 2. Backend Processes
- Insert CLOSED event into `app_events`
- Calculate duration: 622,000ms
- Update `usage_summary`:
  ```sql
  UPDATE usage_summary
  SET total_screen_time = total_screen_time + 622000,
      session_count = session_count + 1,
      last_updated = NOW()
  WHERE user_id = 'abc123' AND date = '2024-01-15';
  ```

### 3. Leaderboard Query (Instant)
```sql
SELECT * FROM usage_summary
WHERE date = '2024-01-15'
ORDER BY total_screen_time DESC
LIMIT 10;
```
**Result:** Fast! No need to sum thousands of events.

---

## ✅ Benefits

1. **Fast Queries:** Pre-aggregated data = instant leaderboards
2. **Scalable:** Works with millions of events
3. **Accurate:** Real-time updates as events arrive
4. **Flexible:** Can still query raw events for detailed analysis
5. **Efficient:** Minimal database load for leaderboard queries

---

## 🤔 Questions

1. **Update Frequency:** Update summary in real-time or batch every minute?
2. **Retention:** Keep summary forever or archive old data?
3. **Ranking Tie-Breaker:** If two users have same time, how to rank? (First to reach? Alphabetical?)
4. **Privacy:** Show usernames or anonymize in leaderboard?

What do you think? This design makes leaderboard queries very fast! 🚀

