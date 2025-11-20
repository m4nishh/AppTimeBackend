#!/bin/bash

# Script to delete PostgreSQL database
# Usage: ./delete_database.sh [database_name]

DB_NAME="${1:-screentime_db}"
DB_USER="${DB_USER:-postgres}"

echo "⚠️  WARNING: This will permanently delete the database: $DB_NAME"
echo "Press Ctrl+C to cancel, or Enter to continue..."
read

# Method 1: Using psql (if in PATH)
if command -v psql &> /dev/null; then
    echo "Deleting database using psql..."
    psql -U "$DB_USER" -c "DROP DATABASE IF EXISTS $DB_NAME;"
    echo "✅ Database '$DB_NAME' deleted (if it existed)"
    exit 0
fi

# Method 2: Try common PostgreSQL installation paths
for psql_path in \
    "/usr/local/bin/psql" \
    "/opt/homebrew/bin/psql" \
    "/Library/PostgreSQL/*/bin/psql" \
    "/Applications/Postgres.app/Contents/Versions/*/bin/psql"
do
    if [ -f $psql_path ] 2>/dev/null; then
        echo "Found psql at: $psql_path"
        "$psql_path" -U "$DB_USER" -c "DROP DATABASE IF EXISTS $DB_NAME;"
        echo "✅ Database '$DB_NAME' deleted (if it existed)"
        exit 0
    fi
done

# Method 3: If using Docker
if command -v docker &> /dev/null; then
    echo "Checking for Docker PostgreSQL container..."
    CONTAINER=$(docker ps -a --filter "name=postgres" --format "{{.Names}}" | head -1)
    if [ ! -z "$CONTAINER" ]; then
        echo "Found PostgreSQL container: $CONTAINER"
        echo "Deleting database using Docker..."
        docker exec -i "$CONTAINER" psql -U "$DB_USER" -c "DROP DATABASE IF EXISTS $DB_NAME;"
        echo "✅ Database '$DB_NAME' deleted (if it existed)"
        exit 0
    fi
fi

echo "❌ Could not find PostgreSQL installation."
echo ""
echo "Please run one of these commands manually:"
echo ""
echo "1. If psql is in your PATH:"
echo "   psql -U postgres -c \"DROP DATABASE IF EXISTS $DB_NAME;\""
echo ""
echo "2. If using Homebrew PostgreSQL:"
echo "   /opt/homebrew/bin/psql -U postgres -c \"DROP DATABASE IF EXISTS $DB_NAME;\""
echo "   # or"
echo "   /usr/local/bin/psql -U postgres -c \"DROP DATABASE IF EXISTS $DB_NAME;\""
echo ""
echo "3. If using Docker:"
echo "   docker exec -i <container_name> psql -U postgres -c \"DROP DATABASE IF EXISTS $DB_NAME;\""
echo ""
echo "4. Connect interactively and run:"
echo "   psql -U postgres"
echo "   DROP DATABASE IF EXISTS $DB_NAME;"
echo "   \\q"

