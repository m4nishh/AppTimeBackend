# Redis Integration Guide

This document describes the Redis integration implemented across the AppTimeBackend project.

## Overview

Redis has been integrated throughout the project to provide:
- **Caching**: Reduce database load by caching frequently accessed data
- **Session Management**: Store admin and TOTP verification sessions
- **Rate Limiting**: Track API request rates per IP/user
- **Performance**: Improve response times for read-heavy operations

## Configuration

### Environment Variables

Add the following to your `.env` file:

```env
# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=          # Optional, leave empty if no password
REDIS_DATABASE=0         # Optional, defaults to 0
```

### Application Configuration

Redis configuration is also available in `application.conf`:

```hocon
redis {
    host = ${?REDIS_HOST}
    port = ${?REDIS_PORT}
    password = ${?REDIS_PASSWORD}
    database = ${?REDIS_DATABASE}
}
```

## Redis Service

The `RedisService` object provides a high-level API for Redis operations:

### Key Features

- **Automatic JSON Serialization**: Objects are automatically serialized/deserialized
- **TTL Management**: Built-in time-to-live support
- **Cache Key Prefixes**: Organized key naming with prefixes
- **Graceful Degradation**: Falls back gracefully if Redis is unavailable

### Usage Examples

```kotlin
// Get from cache
val cached = RedisService.get<MyData>("my:key")

// Set in cache with TTL
RedisService.set("my:key", myData, RedisService.TTL.MEDIUM)

// Delete from cache
RedisService.delete("my:key")

// Increment counter (for rate limiting)
val count = RedisService.increment("counter:key", 60)

// Get or compute pattern
val data = RedisService.getOrCompute("my:key", RedisService.TTL.MEDIUM) {
    // Compute expensive operation
    computeData()
}
```

## Integration Points

### 1. Translation Service

**Location**: `src/main/kotlin/common/TranslationService.kt`

- Caches individual translation messages
- TTL: 24 hours (translations rarely change)
- Cache key format: `translation:{language}:{key}`

### 2. Leaderboard Service

**Location**: `src/main/kotlin/leaderboard/Service.kt`

- Caches daily, weekly, and monthly leaderboards
- TTL: 5 minutes (leaderboards update frequently)
- Cache key format: `leaderboard:{period}:{date}:{userId?}`

**Cache Invalidation**: Leaderboards are automatically invalidated when stats are updated.

### 3. Challenge Service

**Location**: `src/main/kotlin/challenges/Service.kt`

- Caches active challenges list
- Caches challenge details
- Caches challenge rankings
- TTL: 10 minutes
- Cache key formats:
  - `challenge:active:{userId?}`
  - `challenge:detail:{challengeId}:{userId?}`
  - `challenge:rankings:{challengeId}:{userId?}`

**Cache Invalidation**: Caches are invalidated when:
- User joins a challenge
- Challenge stats are submitted
- Challenge details are updated

### 4. Admin Session Management

**Location**: `src/main/kotlin/admin/Routes.kt`

- Stores admin login sessions in Redis
- TTL: 1 hour
- Cache key format: `admin:session:{token}`

**Session Flow**:
1. Admin logs in → Session stored in Redis
2. Admin verifies token → Session TTL refreshed
3. Session expires → Automatically removed after 1 hour

### 5. TOTP Verification Sessions

**Location**: `src/main/kotlin/users/Repository.kt`

- Stores TOTP verification sessions for faster access
- TTL: 1 hour (configurable)
- Cache key format: `totp:session:{requestingUserId}:{targetUserId}`

**Dual Storage**: Sessions are stored in both:
- PostgreSQL (for persistence and audit)
- Redis (for fast access)

### 6. Feature Flags Service

**Location**: `src/main/kotlin/features/Service.kt`

- Caches feature flags evaluation results
- TTL: 30 minutes
- Cache key formats:
  - `feature:flags:{params}`
  - `feature:flag:{featureName}`

**Cache Invalidation**: Caches are invalidated when:
- Feature flags are created
- Feature flags are updated
- Feature flags are deleted

### 7. Focus Service

**Location**: `src/main/kotlin/focus/Service.kt`

- Caches focus stats for users
- TTL: 10 minutes
- Cache key format: `focus:stats:{userId}`

### 8. Rate Limiting

**Location**: `src/main/kotlin/common/RateLimiting.kt`

- Provides rate limiting utilities using Redis counters
- Can be used in route handlers to limit requests per IP/user
- Cache key format: `ratelimit:{key}`

**Usage**:
```kotlin
suspend fun ApplicationCall.rateLimit(
    key: String,
    maxRequests: Long = 100,
    windowSeconds: Long = 60
): Boolean
```

## Cache Key Naming Convention

All cache keys follow a consistent naming pattern:

```
{prefix}:{identifier}:{optional_params}
```

Examples:
- `translation:en:user.profile.retrieved`
- `leaderboard:daily:2024-01-15:user123`
- `challenge:active:all`
- `admin:session:abc123def456`
- `totp:session:user1:user2`
- `feature:flags:user123:v1.0:android`
- `focus:stats:user123`
- `ratelimit:192.168.1.1`

## TTL Values

Predefined TTL constants in `RedisService`:

- `SHORT`: 5 minutes (300 seconds)
- `MEDIUM`: 30 minutes (1800 seconds)
- `LONG`: 1 hour (3600 seconds)
- `VERY_LONG`: 24 hours (86400 seconds)
- `TRANSLATION`: 24 hours
- `LEADERBOARD`: 5 minutes
- `CHALLENGE`: 10 minutes
- `ADMIN_SESSION`: 1 hour
- `TOTP_SESSION`: 1 hour
- `FEATURE_FLAGS`: 30 minutes
- `FOCUS_STATS`: 10 minutes
- `USER_PROFILE`: 30 minutes

## Cache Invalidation

### Pattern-Based Invalidation

```kotlin
// Invalidate all keys matching a pattern
RedisService.invalidatePattern("challenge:active:*")
```

### Manual Invalidation

```kotlin
// Delete specific key
RedisService.delete("challenge:detail:123")
```

## Monitoring

### Check Redis Connection

```kotlin
if (RedisConfig.isAvailable()) {
    println("Redis is connected")
} else {
    println("Redis is not available")
}
```

### Redis Connection Status

The application logs Redis connection status on startup:
- ✅ `Redis connected successfully!` - Redis is available
- ⚠️ `Redis connection failed` - Redis is unavailable (app continues without Redis)

## Graceful Degradation

The application is designed to work without Redis:

- If Redis is unavailable, all cache operations return `null` or `false`
- The application falls back to database queries
- No errors are thrown; the app continues to function normally

## Performance Benefits

### Before Redis
- Every leaderboard request: Database query
- Every translation lookup: In-memory cache (single instance only)
- Every challenge detail: Database query

### After Redis
- Leaderboard requests: Redis cache hit (5-10ms) vs Database query (50-100ms)
- Translation lookups: Redis cache hit (1-2ms) vs In-memory lookup (0.1ms)
- Challenge details: Redis cache hit (5-10ms) vs Database query (50-100ms)

**Expected Performance Improvement**: 5-10x faster for cached operations

## Best Practices

1. **Cache Frequently Accessed Data**: Focus on read-heavy endpoints
2. **Set Appropriate TTLs**: Balance freshness vs performance
3. **Invalidate on Updates**: Clear cache when data changes
4. **Monitor Cache Hit Rates**: Track Redis performance
5. **Use Pattern-Based Keys**: Makes invalidation easier

## Troubleshooting

### Redis Connection Issues

1. Check if Redis is running:
   ```bash
   redis-cli ping
   # Should return: PONG
   ```

2. Verify connection settings in `.env`:
   ```env
   REDIS_HOST=localhost
   REDIS_PORT=6379
   ```

3. Check Redis logs:
   ```bash
   tail -f /var/log/redis/redis-server.log
   ```

### Cache Not Working

1. Verify Redis is connected:
   - Check application startup logs
   - Look for "Redis connected successfully!" message

2. Check cache keys:
   ```bash
   redis-cli
   > KEYS *
   ```

3. Verify TTL settings:
   ```bash
   redis-cli
   > TTL "translation:en:user.profile.retrieved"
   ```

## Future Enhancements

Potential improvements:
- Redis cluster support for high availability
- Cache warming on application startup
- Cache statistics and monitoring dashboard
- Distributed locking for critical operations
- Pub/Sub for cache invalidation across instances


