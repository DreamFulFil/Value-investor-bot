#!/bin/bash
set -euo pipefail

# Ensure we are in the correct directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Starting Python FastAPI bridge..."

# Activate virtual environment
source venv/bin/activate

# Start uvicorn in the background, redirecting output to a log file
# and store PID
UVICORN_LOG="uvicorn.log"
UVICORN_PID_FILE="uvicorn.pid"

uvicorn shioaji_api:app --host 0.0.0.0 --port 8888 > "$UVICORN_LOG" 2>&1 &
UVICORN_PID=$!
echo "$UVICORN_PID" > "$UVICORN_PID_FILE"

echo "Python FastAPI bridge started with PID $UVICORN_PID. Log: $UVICORN_LOG"

# Deactivate venv to avoid interfering with other scripts
deactivate

# Wait for the service to be ready
echo "Waiting for Python FastAPI bridge to be ready on port 8888..."
TIMEOUT=60
ELAPSED=0
while [[ $ELAPSED -lt $TIMEOUT ]]; do
    if curl -sf http://localhost:8888/health >/dev/null 2>&1; then
        echo "Python FastAPI bridge is ready!"
        exit 0
    fi
    sleep 2
    ELAPSED=$((ELAPSED + 2))
done

echo "Error: Python FastAPI bridge did not start within $TIMEOUT seconds."
exit 1
