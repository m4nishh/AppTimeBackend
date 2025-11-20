# Challenges Feature - Deployment Guide

## Overview

The Challenges feature has been successfully implemented and is ready for deployment. The system includes:

- **8 Pre-seeded Challenges** (4 LESS_SCREENTIME, 4 MORE_SCREENTIME)
- **5 Main API Endpoints** for challenge management
- **Ranking System** with challenge-type-based calculations
- **Participant Stats Tracking** for app usage during challenges

## What's Included

### Database Tables
1. **challenges** - Stores challenge information
2. **challenge_participants** - Tracks user participation
3. **challenge_participant_stats** - Stores app usage stats for ranking

### Pre-seeded Challenges

The following challenges are automatically created when the database initializes:

#### LESS_SCREENTIME Challenges (Lower screen time = better rank)

1. **Digital Detox Challenge** (2 weeks)
   - Reduce daily screen time by 30%
   - Reward: Digital Wellness Badge + Premium Features (1 month)

2. **Weekend Warrior Challenge** (1 week)
   - Keep screen time under 2 hours per day on weekends
   - Reward: Weekend Warrior Badge

3. **Evening Unplug Challenge** (1 week)
   - Keep phone usage under 30 minutes after 8 PM
   - Reward: Sleep Well Badge + Meditation App Subscription (1 month)

4. **Social Media Detox Week** (1 week)
   - Limit social media apps to less than 30 minutes per day
   - Reward: Social Detox Champion Badge

#### MORE_SCREENTIME Challenges (Higher screen time = better rank)

5. **Focus Mode Marathon** (1 month)
   - Complete 100 hours of focus mode sessions
   - Reward: Focus Master Badge + Exclusive Profile Badge

6. **Productivity Power Hour** (2 weeks)
   - Accumulate 50 hours of productive app usage
   - Reward: Productivity Pro Badge + Learning Resources Pack

7. **30-Day Learning Streak** (1 month)
   - Spend at least 1 hour daily on educational apps
   - Reward: Lifelong Learner Badge + Course Discount (50% off)

8. **Deep Work Challenge** (2 weeks)
   - Complete 40 hours of uninterrupted focus sessions
   - Reward: Deep Work Master Badge + Productivity Tools Bundle

## Deployment Steps

### 1. Build the Application

```bash
./gradlew build
```

### 2. Database Setup

The challenges will be automatically seeded when the application starts and connects to the database. The seed data checks if challenges already exist, so it's safe to restart the application.

**Note:** If you need to reset challenges, you can:
- Delete the challenges manually from the database
- Or modify `ChallengeSeedData.kt` to force re-seeding

### 3. Start the Server

```bash
./gradlew run
```

Or use the built JAR:
```bash
java -jar build/libs/AppTimeBackend-1.0-SNAPSHOT.jar
```

### 4. Verify Deployment

Test the active challenges endpoint:
```bash
curl -X GET "http://localhost:8080/api/challenges/active"
```

You should see all 8 challenges in the response.

## API Endpoints

All endpoints are documented in `CHALLENGES_API_CURL.md`. Quick reference:

1. **GET** `/api/challenges/active` - Get all active challenges (public)
2. **POST** `/api/challenges/join` - Join a challenge (auth required)
3. **GET** `/api/challenges/user` - Get user's challenges (auth required)
4. **GET** `/api/challenges/{challengeId}` - Get challenge details (auth required)
5. **GET** `/api/challenges/{challengeId}/rankings` - Get top 10 rankings (auth required)
6. **POST** `/api/challenges/stats` - Submit challenge stats (auth required)
7. **POST** `/api/challenges/stats/batch` - Submit batch stats (auth required)

## Ranking System

### LESS_SCREENTIME Challenges
- Participants ranked by **total duration ascending**
- Lower total screen time = better rank (rank 1 = lowest screen time)

### MORE_SCREENTIME Challenges
- Participants ranked by **total duration descending**
- Higher total screen time = better rank (rank 1 = highest screen time)

## Testing the Feature

### 1. View Active Challenges
```bash
curl -X GET "http://localhost:8080/api/challenges/active"
```

### 2. Join a Challenge
```bash
curl -X POST "http://localhost:8080/api/challenges/join" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  -d '{"challengeId": 1}'
```

### 3. Submit Challenge Stats
```bash
curl -X POST "http://localhost:8080/api/challenges/stats" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  -d '{
    "challengeId": 1,
    "appName": "Instagram",
    "packageName": "com.instagram.android",
    "startSyncTime": "2024-01-20T10:00:00Z",
    "endSyncTime": "2024-01-20T10:30:00Z",
    "duration": 1800000
  }'
```

### 4. View Rankings
```bash
curl -X GET "http://localhost:8080/api/challenges/1/rankings" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN"
```

## Database Schema

### challenges table
- `id` (bigint, primary key)
- `title` (varchar)
- `description` (text)
- `reward` (varchar)
- `start_time` (timestamp)
- `end_time` (timestamp)
- `thumbnail` (varchar)
- `challenge_type` (varchar) - "LESS_SCREENTIME" or "MORE_SCREENTIME"
- `is_active` (boolean)
- `created_at` (timestamp)

### challenge_participants table
- `id` (bigint, primary key)
- `challenge_id` (bigint, foreign key to challenges)
- `user_id` (varchar)
- `joined_at` (timestamp)
- Unique constraint on (challenge_id, user_id)

### challenge_participant_stats table
- `id` (bigint, primary key)
- `challenge_id` (bigint, foreign key to challenges)
- `user_id` (varchar)
- `app_name` (varchar)
- `package_name` (varchar)
- `start_sync_time` (timestamp)
- `end_sync_time` (timestamp)
- `duration` (bigint) - milliseconds

## Notes

- Challenges are automatically seeded on first database initialization
- Seed data checks for existing challenges to prevent duplicates
- All timestamps use ISO 8601 format
- Durations are stored in milliseconds
- Ranking calculations are done on-the-fly based on challenge type
- Top 10 rankings are returned, but user's actual rank is also included if they're participating

## Troubleshooting

### Challenges not appearing
- Check database connection
- Verify tables were created: `SELECT * FROM challenges;`
- Check application logs for seed data messages

### Ranking not working
- Ensure participants have submitted stats via `/api/challenges/stats`
- Verify challenge type is set correctly (LESS_SCREENTIME or MORE_SCREENTIME)
- Check that stats are within the challenge time period

### Cannot join challenge
- Verify challenge exists and is active
- Check that challenge hasn't ended (endTime > now)
- Ensure user hasn't already joined (unique constraint)

## Next Steps

1. **Customize Challenges**: Edit `ChallengeSeedData.kt` to add/modify challenges
2. **Add Thumbnails**: Update thumbnail URLs to point to actual images
3. **Configure Rewards**: Implement reward distribution system
4. **Add Notifications**: Notify users about challenge updates, rankings, etc.
5. **Analytics**: Track challenge participation and engagement metrics

