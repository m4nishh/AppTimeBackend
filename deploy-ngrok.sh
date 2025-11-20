#!/bin/bash

# Deploy AppTimeBackend to Ngrok
# Make sure you have ngrok installed and configured

echo "🚀 Starting AppTimeBackend deployment to Ngrok..."

# Check if ngrok is installed
if ! command -v ngrok &> /dev/null; then
    echo "❌ Error: ngrok is not installed"
    echo "Install it from: https://ngrok.com/download"
    exit 1
fi

# Check if NGROK_AUTHTOKEN is set
if [ -z "$NGROK_AUTHTOKEN" ]; then
    echo "⚠️  Warning: NGROK_AUTHTOKEN environment variable is not set"
    echo "You can get your authtoken from: https://dashboard.ngrok.com/get-started/your-authtoken"
    echo "Set it with: export NGROK_AUTHTOKEN=your_token_here"
    read -p "Do you want to continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    ngrok config add-authtoken "$NGROK_AUTHTOKEN"
fi

# Check if port 8080 is already in use
if lsof -ti:8080 > /dev/null 2>&1; then
    echo "⚠️  Port 8080 is already in use"
    echo "Killing existing processes on port 8080..."
    lsof -ti:8080 | xargs kill -9 2>/dev/null || true
    sleep 2
fi

# Build the project
echo "📦 Building project..."
./gradlew build --no-daemon

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

# Start the Ktor server in background
echo "🔧 Starting Ktor server on port 8080..."
./gradlew run --no-daemon &
SERVER_PID=$!

# Wait for server to start
echo "⏳ Waiting for server to start..."
sleep 10

# Check if server is running
if ! curl -s http://localhost:8080/health > /dev/null; then
    echo "❌ Server failed to start!"
    kill $SERVER_PID 2>/dev/null
    exit 1
fi

echo "✅ Server is running!"

# Start ngrok tunnel
echo "🌐 Starting ngrok tunnel..."
ngrok http 8080 --log=stdout > ngrok.log &
NGROK_PID=$!

# Wait for ngrok to start
sleep 5

# Get the public URL from ngrok API
NGROK_URL=$(curl -s http://localhost:4040/api/tunnels | grep -o '"public_url":"https://[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$NGROK_URL" ]; then
    echo "❌ Failed to get ngrok URL"
    kill $SERVER_PID $NGROK_PID 2>/dev/null
    exit 1
fi

echo ""
echo "=========================================="
echo "🎉 Deployment Successful!"
echo "=========================================="
echo ""
echo "🌐 Ngrok Public URL: $NGROK_URL"
echo ""
echo "📋 Test your endpoints:"
echo "   Health: $NGROK_URL/health"
echo "   Active Challenges: $NGROK_URL/api/challenges/active"
echo "   Register: $NGROK_URL/api/users/register"
echo "   Usage Stats: $NGROK_URL/api/usage/stats/daily?date=2024-01-15"
echo ""
echo "💡 Note: This tunnel will be active until you stop it (Ctrl+C)"
echo "   Free ngrok tunnels expire after 2 hours"
echo ""
echo "Press Ctrl+C to stop the server and ngrok tunnel"
echo "=========================================="
echo ""

# Function to cleanup on exit
cleanup() {
    echo ""
    echo "🛑 Stopping server and ngrok..."
    kill $SERVER_PID $NGROK_PID 2>/dev/null
    echo "✅ Stopped!"
    exit 0
}

# Trap Ctrl+C
trap cleanup SIGINT SIGTERM

# Keep the script running
wait

