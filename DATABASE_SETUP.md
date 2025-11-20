# PostgreSQL Database Setup Guide

## Why PostgreSQL?

PostgreSQL is the best choice for your ScreenTime backend because:

✅ **Handles both relational and time-series data** - Perfect for user profiles + app usage tracking  
✅ **Excellent query performance** - Fast aggregations for leaderboards and stats  
✅ **JSON support** - Flexible schema for preferences and settings  
✅ **ACID compliance** - Data integrity for user data  
✅ **Great ecosystem** - Works seamlessly with Ktor and Exposed ORM  
✅ **Scalable** - Can handle millions of records efficiently  

## Quick Setup

### Option 1: Local PostgreSQL (Development)

1. **Install PostgreSQL:**
   ```bash
   # macOS
   brew install postgresql@15
   brew services start postgresql@15
   
   # Ubuntu/Debian
   sudo apt-get install postgresql postgresql-contrib
   sudo systemctl start postgresql
   
   # Windows
   # Download from https://www.postgresql.org/download/windows/
   ```

2. **Create Database:**
   ```bash
   psql -U postgres
   CREATE DATABASE screentime_db;
   CREATE USER screentime_user WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE screentime_db TO screentime_user;
   \q
   ```

3. **Set Environment Variables:**
   ```bash
   export DATABASE_URL="jdbc:postgresql://localhost:5432/screentime_db"
   export DB_USER="screentime_user"
   export DB_PASSWORD="your_password"
   ```

### Option 2: Docker (Recommended for Development)

```bash
docker run --name screentime-postgres \
  -e POSTGRES_DB=screentime_db \
  -e POSTGRES_USER=screentime_user \
  -e POSTGRES_PASSWORD=your_password \
  -p 5432:5432 \
  -d postgres:15-alpine
```

### Option 3: Cloud PostgreSQL (Production)

**Recommended Providers:**
- **Supabase** (Free tier: 500MB, great for startups)
- **Neon** (Serverless PostgreSQL, free tier available)
- **AWS RDS** (Production-ready, pay-as-you-go)
- **Railway** (Simple deployment, free tier)
- **Render** (Free tier available)

## Database Schema Recommendations

Based on your API structure, here are the main tables you'll need:

### Core Tables:
1. **users** - User accounts and device info
2. **app_usage** - Time-series app usage data
3. **focus_sessions** - Focus mode tracking
4. **url_searches** - Web browsing history
5. **blocked_domains** - Domain blocking rules
6. **notifications** - User notifications
7. **leaderboard_stats** - Pre-aggregated leaderboard data

### Indexing Strategy:
- Index on `user_id` + `timestamp` for app_usage queries
- Index on `package_name` for app-specific queries
- Index on `date` for daily/weekly aggregations
- Consider partitioning `app_usage` by date for large datasets

## Testing Connection

Run your Ktor server:
```bash
./gradlew run
```

Check the logs - you should see:
```
✅ Database connected successfully!
```

## Alternative Databases (If Needed)

### TimescaleDB (PostgreSQL Extension)
- **Best for:** Heavy time-series workloads
- **When to use:** If you're storing millions of usage records per day
- **Setup:** Install as PostgreSQL extension

### MongoDB
- **Best for:** Very flexible schema requirements
- **When to use:** If data structure changes frequently
- **Trade-off:** Less efficient for complex queries

### InfluxDB
- **Best for:** Pure time-series metrics
- **When to use:** Only if you ONLY need time-series data
- **Trade-off:** Not suitable for relational data (users, profiles)

## Next Steps

1. Create your database tables using Exposed ORM
2. Set up database migrations
3. Implement your API endpoints with database operations
4. Add connection pooling configuration
5. Set up database backups (for production)

