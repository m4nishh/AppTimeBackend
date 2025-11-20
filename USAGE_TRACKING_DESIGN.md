# App Usage Tracking Design - Examples

## 📱 What Android App Sends

### Example 1: Hourly Batch Submission

The app sends data every hour with all app events from that hour:

```json
{
  "fromTime": "2024-01-15T10:00:00Z",
  "toTime": "2024-01-15T11:00:00Z",
  "usage": [
    {
      "packageName": "com.android.chrome",
      "appName": "Chrome",
      "events": [
        {
          "type": "OPENED",
          "timestamp": "2024-01-15T10:05:23Z",
          "sessionId": "session_001"
        },
        {
          "type": "CLOSED",
          "timestamp": "2024-01-15T10:15:45Z",
          "sessionId": "session_001"
        },
        {
          "type": "OPENED",
          "timestamp": "2024-01-15T10:20:10Z",
          "sessionId": "session_002"
        },
        {
          "type": "CLOSED",
          "timestamp": "2024-01-15T10:30:30Z",
          "sessionId": "session_002"
        }
      ]
    },
    {
      "packageName": "com.whatsapp",
      "appName": "WhatsApp",
      "events": [
        {
          "type": "OPENED",
          "timestamp": "2024-01-15T10:12:00Z",
          "sessionId": "session_003"
        },
        {
          "type": "CLOSED",
          "timestamp": "2024-01-15T10:18:00Z",
          "sessionId": "session_003"
        }
      ]
    }
  ],
  "totalTime": 1800000
}
```

**Calculation:**
- Chrome: (10:15:45 - 10:05:23) + (10:30:30 - 10:20:10) = 10min 22sec + 10min 20sec = 20min 42sec = 1,242,000ms
- WhatsApp: (10:18:00 - 10:12:00) = 6min = 360,000ms
- Total: 1,242,000 + 360,000 = 1,602,000ms (but app sends 1,800,000ms - includes other apps)

---

### Example 2: Real-time Event Submission

App sends events as they happen:

```json
{
  "fromTime": "2024-01-15T10:05:23Z",
  "toTime": "2024-01-15T10:05:23Z",
  "usage": [
    {
      "packageName": "com.android.chrome",
      "appName": "Chrome",
      "events": [
        {
          "type": "OPENED",
          "timestamp": "2024-01-15T10:05:23Z",
          "sessionId": "session_001"
        }
      ]
    }
  ],
  "totalTime": 0
}
```

Then later when app closes:

```json
{
  "fromTime": "2024-01-15T10:15:45Z",
  "toTime": "2024-01-15T10:15:45Z",
  "usage": [
    {
      "packageName": "com.android.chrome",
      "appName": "Chrome",
      "events": [
        {
          "type": "CLOSED",
          "timestamp": "2024-01-15T10:15:45Z",
          "sessionId": "session_001"
        }
      ]
    }
  ],
  "totalTime": 622000
}
```

---

## 🗄️ How Data is Stored in Database

### Table: `app_events`

| id | user_id | package_name | app_name | event_type | timestamp | session_id | duration_ms |
|----|---------|--------------|----------|------------|-----------|------------|-------------|
| 1 | abc123 | com.android.chrome | Chrome | OPENED | 2024-01-15 10:05:23 | session_001 | NULL |
| 2 | abc123 | com.android.chrome | Chrome | CLOSED | 2024-01-15 10:15:45 | session_001 | 622000 |
| 3 | abc123 | com.whatsapp | WhatsApp | OPENED | 2024-01-15 10:12:00 | session_003 | NULL |
| 4 | abc123 | com.whatsapp | WhatsApp | CLOSED | 2024-01-15 10:18:00 | session_003 | 360000 |
| 5 | abc123 | com.android.chrome | Chrome | OPENED | 2024-01-15 10:20:10 | session_002 | NULL |
| 6 | abc123 | com.android.chrome | Chrome | CLOSED | 2024-01-15 10:30:30 | session_002 | 620000 |

**Note:** `duration_ms` is calculated when CLOSED event is received by matching with OPENED event.

---

## 📊 Query Examples

### Query 1: Get Today's Total Screen Time

```sql
SELECT SUM(duration_ms) as total_time
FROM app_events
WHERE user_id = 'abc123'
  AND event_type = 'CLOSED'
  AND DATE(timestamp) = '2024-01-15';
```

**Result:** `1,602,000 ms` (20min 42sec + 6min)

---

### Query 2: Get App Usage for Today

```sql
SELECT 
  package_name,
  app_name,
  COUNT(*) as sessions,
  SUM(duration_ms) as total_time
FROM app_events
WHERE user_id = 'abc123'
  AND event_type = 'CLOSED'
  AND DATE(timestamp) = '2024-01-15'
GROUP BY package_name, app_name
ORDER BY total_time DESC;
```

**Result:**
| package_name | app_name | sessions | total_time |
|--------------|----------|----------|------------|
| com.android.chrome | Chrome | 2 | 1,242,000 |
| com.whatsapp | WhatsApp | 1 | 360,000 |

---

### Query 3: Get Hourly Breakdown

```sql
SELECT 
  EXTRACT(HOUR FROM timestamp) as hour,
  SUM(duration_ms) as total_time
FROM app_events
WHERE user_id = 'abc123'
  AND event_type = 'CLOSED'
  AND DATE(timestamp) = '2024-01-15'
GROUP BY hour
ORDER BY hour;
```

**Result:**
| hour | total_time |
|------|------------|
| 10 | 1,602,000 |

---

### Query 4: Get All Events for an App (Full History)

```sql
SELECT 
  event_type,
  timestamp,
  duration_ms
FROM app_events
WHERE user_id = 'abc123'
  AND package_name = 'com.android.chrome'
ORDER BY timestamp;
```

**Result:**
| event_type | timestamp | duration_ms |
|------------|-----------|-------------|
| OPENED | 2024-01-15 10:05:23 | NULL |
| CLOSED | 2024-01-15 10:15:45 | 622000 |
| OPENED | 2024-01-15 10:20:10 | NULL |
| CLOSED | 2024-01-15 10:30:30 | 620000 |

---

## 🔄 Processing Flow

### When App Sends Batch Data:

1. **Receive Request:**
   ```json
   {
     "fromTime": "2024-01-15T10:00:00Z",
     "toTime": "2024-01-15T11:00:00Z",
     "usage": [...],
     "totalTime": 1800000
   }
   ```

2. **Process Each App:**
   - For each app in `usage` array
   - For each event in `events` array
   - Store event in `app_events` table

3. **Calculate Duration:**
   - When CLOSED event received
   - Find matching OPENED event by `sessionId`
   - Calculate: `CLOSED.timestamp - OPENED.timestamp`
   - Update CLOSED event with `duration_ms`

4. **Validate:**
   - Check if `totalTime` matches sum of durations
   - Flag discrepancies for review

---

## 📈 Use Cases

### Use Case 1: Daily Report
**Request:** Get user's screen time for today
**Query:** Sum all CLOSED events for today
**Response:**
```json
{
  "date": "2024-01-15",
  "totalScreenTime": 1800000,
  "appCount": 5,
  "topApps": [
    {"packageName": "com.android.chrome", "time": 1242000},
    {"packageName": "com.whatsapp", "time": 360000}
  ]
}
```

### Use Case 2: App History
**Request:** Get all sessions for Chrome today
**Query:** Get all events for Chrome, group by sessionId
**Response:**
```json
{
  "appName": "Chrome",
  "sessions": [
    {
      "sessionId": "session_001",
      "openedAt": "2024-01-15T10:05:23Z",
      "closedAt": "2024-01-15T10:15:45Z",
      "duration": 622000
    },
    {
      "sessionId": "session_002",
      "openedAt": "2024-01-15T10:20:10Z",
      "closedAt": "2024-01-15T10:30:30Z",
      "duration": 620000
    }
  ],
  "totalTime": 1242000
}
```

### Use Case 3: Weekly Trends
**Request:** Get screen time for each day this week
**Query:** Group by date, sum durations
**Response:**
```json
{
  "week": "2024-01-15 to 2024-01-21",
  "dailyStats": [
    {"date": "2024-01-15", "totalTime": 1800000},
    {"date": "2024-01-16", "totalTime": 2100000},
    {"date": "2024-01-17", "totalTime": 1500000}
  ],
  "averageDaily": 1800000
}
```

---

## 🎯 Recommended Structure

### Request Model:
```kotlin
data class UsageBatchRequest(
    val fromTime: String,  // ISO 8601
    val toTime: String,    // ISO 8601
    val usage: List<AppUsageData>,
    val totalTime: Long   // milliseconds
)

data class AppUsageData(
    val packageName: String,
    val appName: String,
    val events: List<AppEvent>
)

data class AppEvent(
    val type: String,      // "OPENED" or "CLOSED"
    val timestamp: String,  // ISO 8601
    val sessionId: String  // Unique per open/close pair
)
```

### Database Table:
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

---

## ✅ Benefits of This Approach

1. **Complete History:** Every open/close is tracked
2. **Accurate Calculations:** Duration calculated from actual events
3. **Flexible Queries:** Can answer any question about usage
4. **Debugging:** Can trace exactly what happened
5. **Analytics:** Can detect patterns, anomalies
6. **Data Integrity:** Can validate app's totalTime against calculated sum

---

## 🤔 Questions to Consider

1. **Session Matching:** What if CLOSED event arrives before OPENED? (Shouldn't happen, but handle edge cases)
2. **Orphaned Events:** What if OPENED but no CLOSED? (App crashed, phone died)
3. **Duplicate Prevention:** How to handle same event sent twice?
4. **Time Validation:** What if `fromTime`/`toTime` don't match event timestamps?
5. **Batch Size:** How many events per batch? (Performance consideration)

What do you think? Does this structure work for your needs?

