# Notification Queue System

## Overview

The notification queue system decouples challenge reward processing from notification sending, making the system more resilient and scalable. When challenges are won, notifications are enqueued instead of being sent immediately, allowing for asynchronous processing.

## Architecture

### Components

1. **NotificationQueueService** (`src/main/kotlin/notifications/NotificationQueueService.kt`)
   - Manages the message queue
   - Handles enqueueing and processing of notification messages
   - Uses in-memory queue with coroutines for async processing
   - Supports multiple concurrent workers for parallel processing

2. **QueueModels** (`src/main/kotlin/notifications/QueueModels.kt`)
   - Defines message types for different notification scenarios
   - `ChallengeRewardNotificationMessage`: Notification to challenge winner
   - `ChallengeWinnerNotificationMessage`: Notification to other participants

3. **RewardService** (Updated)
   - Now enqueues notifications instead of sending directly
   - Uses `NotificationQueueService.enqueueChallengeRewardNotification()` and `enqueueChallengeWinnerNotification()`

4. **Queue Consumer**
   - Started automatically on application startup
   - Runs multiple workers (default: 5) for concurrent processing
   - Processes messages asynchronously from the queue

## How It Works

### Flow

1. **Challenge Reward Processing**
   ```
   RewardService.awardChallengeRewards()
   ↓
   Process rewards and add coins
   ↓
   Enqueue notifications (non-blocking)
   ↓
   Return response immediately
   ```

2. **Notification Processing**
   ```
   Queue Consumer (Worker)
   ↓
   Dequeue notification message
   ↓
   Send notification via NotificationService
   ↓
   Log success/failure
   ```

### Benefits

- **Non-blocking**: Reward processing doesn't wait for notifications
- **Resilient**: Failed notifications don't block reward processing
- **Scalable**: Multiple workers process notifications concurrently
- **Smooth**: System remains responsive even under high load

## Configuration

### Worker Configuration

The number of concurrent workers can be configured in `Application.kt`:

```kotlin
NotificationQueueService.startConsumer(
    notificationService, 
    appScope, 
    maxConcurrentWorkers = 5  // Adjust based on load
)
```

### Queue Statistics

Monitor queue performance:

```kotlin
val stats = NotificationQueueService.getStatistics()
// Returns: QueueStatistics(queueSize, totalEnqueued, totalProcessed, totalFailed)
```

## Message Types

### ChallengeRewardNotificationMessage

Sent to challenge winners when they win a challenge.

```kotlin
data class ChallengeRewardNotificationMessage(
    val messageId: String,
    val timestamp: Long,
    val userId: String,
    val challengeTitle: String,
    val rank: Int,
    val coins: Long,
    val challengeId: Long
)
```

### ChallengeWinnerNotificationMessage

Sent to other participants when someone wins a challenge.

```kotlin
data class ChallengeWinnerNotificationMessage(
    val messageId: String,
    val timestamp: Long,
    val winnerUserId: String,
    val winnerUsername: String,
    val challengeTitle: String,
    val coins: Long,
    val challengeId: Long,
    val otherUserIds: List<String>
)
```

## Usage

### Enqueueing Notifications

The queue service is used automatically in `RewardService`. To manually enqueue:

```kotlin
// Challenge reward notification
NotificationQueueService.enqueueChallengeRewardNotification(
    userId = "user123",
    challengeTitle = "Daily Focus Challenge",
    rank = 1,
    coins = 1000,
    challengeId = 42
)

// Challenge winner notification
NotificationQueueService.enqueueChallengeWinnerNotification(
    winnerUserId = "user123",
    winnerUsername = "John Doe",
    challengeTitle = "Daily Focus Challenge",
    coins = 1000,
    challengeId = 42,
    otherUserIds = listOf("user456", "user789")
)
```

## Monitoring

### Queue Statistics

Check queue health:

```kotlin
val stats = NotificationQueueService.getStatistics()
println("Queue size: ${stats.queueSize}")
println("Total enqueued: ${stats.totalEnqueued}")
println("Total processed: ${stats.totalProcessed}")
println("Total failed: ${stats.totalFailed}")
```

### Logs

The queue service logs important events:
- Message enqueued
- Worker started/processing
- Message processed successfully
- Message processing failed

## Future Enhancements

### Redis Integration

The current implementation uses an in-memory queue. For production scalability, consider:

1. **Redis Lists**: Use Redis LPUSH/RPOP for distributed queue
2. **Redis Streams**: Use Redis Streams for more advanced features
3. **Dead Letter Queue**: Store failed messages for retry
4. **Retry Logic**: Automatic retry with exponential backoff

### Example Redis Integration

```kotlin
// Enqueue to Redis
redis.lpush("notification:queue", json.encodeToString(message))

// Dequeue from Redis
val messageJson = redis.rpop("notification:queue")
val message = json.decodeFromString<NotificationMessage>(messageJson)
```

## Troubleshooting

### Queue Not Processing

1. Check if queue consumer is started (check application logs)
2. Verify workers are running (check logs for "Worker X started")
3. Check for exceptions in worker logs

### High Queue Size

1. Increase number of workers
2. Check notification service performance
3. Verify Firebase/notification service connectivity

### Failed Notifications

1. Check notification service logs
2. Verify user Firebase tokens are valid
3. Check network connectivity to Firebase

## Performance

- **Enqueue Time**: < 1ms (in-memory)
- **Processing Time**: Depends on notification service (typically 50-200ms per notification)
- **Throughput**: ~50-100 notifications/second with 5 workers
- **Scalability**: Can handle thousands of notifications per minute

## Best Practices

1. **Monitor Queue Size**: Alert if queue size exceeds threshold
2. **Monitor Failure Rate**: Investigate if failure rate is high
3. **Adjust Workers**: Scale workers based on load
4. **Graceful Shutdown**: Ensure queue is drained on shutdown (future enhancement)

