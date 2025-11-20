# Three-Table Design for Usage Tracking

## 🎯 Three Separate Tables

### 1. **Usage Events Table** - Track every OPEN/CLOSE event
### 2. **App Usage Summary Table** - Total screen time per app per user
### 3. **App Timeline Table** - Sequence of app opens (Instagram → Twitter → Chrome)

---

## 📊 Table 1: `app_events` (Detailed Events)

**Purpose:** Store every OPEN/CLOSED event for complete history

```sql
CREATE TABLE app_events (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    package_name VARCHAR(255) NOT NULL,
    app_name VARCHAR(255),
    event_type VARCHAR(10) NOT NULL,  -- 'OPENED' or 'CLOSED'
    timestamp TIMESTAMP NOT NULL,
    session_id VARCHAR(255) NOT NULL,
    duration_ms BIGINT,  -- Calculated when CLOSED
    created_at TIMESTAMP DEFAULT NOW(),
    
    INDEX idx_user_time (user_id, timestamp),
    INDEX idx_user_package (user_id, package_name),
    INDEX idx_session (session_id)
);
```

**Example Data:**
| id | user_id | package_name | app_name | event_type | timestamp | session_id | duration_ms |
|----|---------|--------------|----------|------------|-----------|------------|-------------|
| 1 | user1 | com.instagram | Instagram | OPENED | 2024-01-15 10:00:00 | s1 | NULL |
| 2 | user1 | com.instagram | Instagram | CLOSED | 2024-01-15 10:05:00 | s1 | 300000 |
| 3 | user1 | com.twitter | Twitter | OPENED | 2024-01-15 10:05:30 | s2 | NULL |
| 4 | user1 | com.twitter | Twitter | CLOSED | 2024-01-15 10:10:00 | s2 | 270000 |

---

## 📊 Table 2: `app_usage_summary` (Total Time Per App)

**Purpose:** Maintain total screen time per app per user (aggregated)

```sql
CREATE TABLE app_usage_summary (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    package_name VARCHAR(255) NOT NULL,
    app_name VARCHAR(255),
    total_time_ms BIGINT DEFAULT 0,  -- Total time for this app
    session_count INT DEFAULT 0,      -- Number of sessions
    last_used TIMESTAMP,             -- Last time app was used
    date DATE,                        -- For daily tracking (optional)
    
    UNIQUE(user_id, package_name, date),  -- One row per user/app/date
    INDEX idx_user_time (user_id, total_time_ms DESC),
    INDEX idx_user_date (user_id, date)
);
```

**Example Data:**
| id | user_id | package_name | app_name | total_time_ms | session_count | last_used | date |
|----|---------|--------------|----------|---------------|---------------|-----------|------|
| 1 | user1 | com.instagram | Instagram | 300000 | 1 | 2024-01-15 10:05:00 | 2024-01-15 |
| 2 | user1 | com.twitter | Twitter | 270000 | 1 | 2024-01-15 10:10:00 | 2024-01-15 |
| 3 | user1 | com.android.chrome | Chrome | 500000 | 2 | 2024-01-15 10:20:00 | 2024-01-15 |

**Benefits:**
- Fast queries: "What apps did I use today?"
- Easy leaderboard: "Top apps by usage time"
- Quick stats: "How much time on Instagram?"

---

## 📊 Table 3: `app_timeline` (Sequence of App Opens)

**Purpose:** Track the sequence/order of app opens (like activity feed)

```sql
CREATE TABLE app_timeline (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    package_name VARCHAR(255) NOT NULL,
    app_name VARCHAR(255),
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    duration_ms BIGINT,
    sequence_number INT,  -- Order in which apps were opened (1, 2, 3...)
    date DATE NOT NULL,
    
    INDEX idx_user_date_seq (user_id, date, sequence_number),
    INDEX idx_user_time (user_id, opened_at)
);
```

**Example Data:**
| id | user_id | package_name | app_name | opened_at | closed_at | duration_ms | sequence | date |
|----|---------|--------------|----------|-----------|-----------|-------------|----------|------|
| 1 | user1 | com.instagram | Instagram | 10:00:00 | 10:05:00 | 300000 | 1 | 2024-01-15 |
| 2 | user1 | com.twitter | Twitter | 10:05:30 | 10:10:00 | 270000 | 2 | 2024-01-15 |
| 3 | user1 | com.android.chrome | Chrome | 10:10:30 | 10:15:00 | 270000 | 3 | 2024-01-15 |

**Benefits:**
- See app usage sequence: "Opened Instagram, then Twitter, then Chrome"
- Timeline view: "What did I do today in order?"
- Pattern analysis: "What apps do I open after Instagram?"

---

## 🔄 How Data Flows

### When App Sends Event:

```json
{
  "fromTime": "2024-01-15T10:00:00Z",
  "toTime": "2024-01-15T10:05:00Z",
  "usage": [{
    "packageName": "com.instagram",
    "appName": "Instagram",
    "events": [
      {"type": "OPENED", "timestamp": "2024-01-15T10:00:00Z", "sessionId": "s1"},
      {"type": "CLOSED", "timestamp": "2024-01-15T10:05:00Z", "sessionId": "s1"}
    ]
  }],
  "totalTime": 300000
}
```

### Backend Processing:

1. **Insert into `app_events`:**
   ```sql
   INSERT INTO app_events (user_id, package_name, app_name, event_type, timestamp, session_id)
   VALUES ('user1', 'com.instagram', 'Instagram', 'OPENED', '2024-01-15 10:00:00', 's1');
   
   INSERT INTO app_events (user_id, package_name, app_name, event_type, timestamp, session_id, duration_ms)
   VALUES ('user1', 'com.instagram', 'Instagram', 'CLOSED', '2024-01-15 10:05:00', 's1', 300000);
   ```

2. **Update `app_usage_summary`:**
   ```sql
   INSERT INTO app_usage_summary (user_id, package_name, app_name, total_time_ms, session_count, last_used, date)
   VALUES ('user1', 'com.instagram', 'Instagram', 300000, 1, '2024-01-15 10:05:00', '2024-01-15')
   ON CONFLICT (user_id, package_name, date)
   DO UPDATE SET
       total_time_ms = app_usage_summary.total_time_ms + 300000,
       session_count = app_usage_summary.session_count + 1,
       last_used = '2024-01-15 10:05:00';
   ```

3. **Insert into `app_timeline`:**
   ```sql
   INSERT INTO app_timeline (user_id, package_name, app_name, opened_at, closed_at, duration_ms, sequence_number, date)
   VALUES (
       'user1', 
       'com.instagram', 
       'Instagram', 
       '2024-01-15 10:00:00',
       '2024-01-15 10:05:00',
       300000,
       (SELECT COALESCE(MAX(sequence_number), 0) + 1 FROM app_timeline WHERE user_id = 'user1' AND date = '2024-01-15'),
       '2024-01-15'
   );
   ```

---

## 📈 Query Examples

### Query 1: Get Today's Timeline (Sequence of Apps)

```sql
SELECT 
    app_name,
    opened_at,
    closed_at,
    duration_ms,
    sequence_number
FROM app_timeline
WHERE user_id = 'user1' AND date = '2024-01-15'
ORDER BY sequence_number;
```

**Result:**
| app_name | opened_at | closed_at | duration_ms | sequence |
|----------|-----------|-----------|-------------|----------|
| Instagram | 10:00:00 | 10:05:00 | 300000 | 1 |
| Twitter | 10:05:30 | 10:10:00 | 270000 | 2 |
| Chrome | 10:10:30 | 10:15:00 | 270000 | 3 |

**Response:**
```json
{
  "timeline": [
    {"app": "Instagram", "opened": "10:00:00", "closed": "10:05:00", "duration": 300000},
    {"app": "Twitter", "opened": "10:05:30", "closed": "10:10:00", "duration": 270000},
    {"app": "Chrome", "opened": "10:10:30", "closed": "10:15:00", "duration": 270000}
  ]
}
```

---

### Query 2: Get Top Apps by Usage Time

```sql
SELECT 
    app_name,
    total_time_ms,
    session_count,
    last_used
FROM app_usage_summary
WHERE user_id = 'user1' AND date = '2024-01-15'
ORDER BY total_time_ms DESC;
```

**Result:**
| app_name | total_time_ms | session_count | last_used |
|----------|---------------|---------------|-----------|
| Chrome | 500000 | 2 | 10:20:00 |
| Instagram | 300000 | 1 | 10:05:00 |
| Twitter | 270000 | 1 | 10:10:00 |

---

### Query 3: Leaderboard (Total Screen Time Per User)

```sql
SELECT 
    u.username,
    SUM(aus.total_time_ms) as total_screen_time,
    ROW_NUMBER() OVER (ORDER BY SUM(aus.total_time_ms) DESC) as rank
FROM app_usage_summary aus
JOIN users u ON u.user_id = aus.user_id
WHERE aus.date = '2024-01-15'
GROUP BY u.user_id, u.username
ORDER BY total_screen_time DESC
LIMIT 10;
```

**Result:**
| rank | username | total_screen_time |
|------|----------|-------------------|
| 1 | user_abc | 5000000 |
| 2 | user_def | 3000000 |
| 3 | user_ghi | 2000000 |

---

### Query 4: Get App Usage History

```sql
SELECT 
    date,
    total_time_ms,
    session_count
FROM app_usage_summary
WHERE user_id = 'user1' AND package_name = 'com.instagram'
ORDER BY date DESC
LIMIT 30;
```

**Result:** Last 30 days of Instagram usage

---

## 🎯 Benefits of Three-Table Design

### 1. **Separation of Concerns**
- `app_events`: Raw data, complete history
- `app_usage_summary`: Aggregated stats, fast queries
- `app_timeline`: Sequential view, activity feed

### 2. **Performance**
- Leaderboard: Query `app_usage_summary` (fast aggregation)
- Timeline: Query `app_timeline` (ordered sequence)
- History: Query `app_events` (detailed events)

### 3. **Easy Queries**
- "Top apps today" → `app_usage_summary`
- "What did I do today?" → `app_timeline`
- "Show me all Chrome events" → `app_events`

### 4. **Scalability**
- Each table optimized for its purpose
- Indexes tailored to query patterns
- Can partition by date if needed

---

## 📋 Table Relationships

```
users (1) ──┐
            ├──> app_events (many) - Every event
            ├──> app_usage_summary (many) - Per app per day
            └──> app_timeline (many) - Sequential sessions
```

---

## 🔄 Update Strategy

### When CLOSED Event Received:

1. **Insert into `app_events`** ✅
2. **Update `app_usage_summary`** (increment total_time_ms) ✅
3. **Update `app_timeline`** (set closed_at, duration_ms) ✅

All in one transaction for data consistency!

---

## 📊 Example: Complete Flow

### App Sends:
```json
{
  "fromTime": "2024-01-15T10:00:00Z",
  "toTime": "2024-01-15T10:05:00Z",
  "usage": [{
    "packageName": "com.instagram",
    "appName": "Instagram",
    "events": [
      {"type": "OPENED", "timestamp": "2024-01-15T10:00:00Z", "sessionId": "s1"},
      {"type": "CLOSED", "timestamp": "2024-01-15T10:05:00Z", "sessionId": "s1"}
    ]
  }],
  "totalTime": 300000
}
```

### Backend:
1. Store events → `app_events` table
2. Update summary → `app_usage_summary` (Instagram: +300000ms)
3. Add to timeline → `app_timeline` (sequence: 1)

### Queries:
- **Timeline:** "Opened Instagram at 10:00, closed at 10:05"
- **Summary:** "Instagram: 300000ms today, 1 session"
- **Events:** "Instagram OPENED at 10:00, CLOSED at 10:05"

---

## ✅ Summary

**Three Tables:**
1. `app_events` - Every OPEN/CLOSE event (detailed)
2. `app_usage_summary` - Total time per app (aggregated)
3. `app_timeline` - Sequence of app opens (ordered)

**Benefits:**
- ✅ Fast leaderboard queries
- ✅ Easy timeline view
- ✅ Complete event history
- ✅ Optimized for each use case

This design gives you the best of all worlds! 🚀

