# Database Connection Info

## Database Created ✅

- **Database Name:** `screentime_db`
- **Owner:** `postgres` (or `amankumar` depending on your setup)
- **Port:** `5432`
- **Host:** `localhost`

## User Credentials

### Option 1: Use dedicated user (Recommended for production)
- **Username:** `screentime_user`
- **Password:** `screentime_pass`

### Option 2: Use postgres superuser (For development)
- **Username:** `postgres` (or `amankumar`)
- **Password:** (your PostgreSQL password, if set)

## Connection String

```bash
# Using dedicated user
DATABASE_URL=jdbc:postgresql://localhost:5432/screentime_db
DB_USER=screentime_user
DB_PASSWORD=screentime_pass

# Using postgres user (default in code)
DATABASE_URL=jdbc:postgresql://localhost:5432/screentime_db
DB_USER=postgres
DB_PASSWORD=
```

## Test Connection

```bash
# Test with psql
/opt/homebrew/opt/postgresql@15/bin/psql -U postgres -d screentime_db

# Or with the dedicated user
/opt/homebrew/opt/postgresql@15/bin/psql -U screentime_user -d screentime_db
```

## Next Steps

1. The database is ready to use
2. Your Ktor application will connect automatically when you run it
3. You can now create tables using Exposed ORM
4. Set environment variables if you want to use the dedicated user instead of defaults

