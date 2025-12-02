#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

UVICORN_PID_FILE="uvicorn.pid"

if [[ -f "$UVICORN_PID_FILE" ]]; then
    UVICORN_PID=$(cat "$UVICORN_PID_FILE")
    echo "Stopping Python FastAPI bridge with PID $UVICORN_PID..."
    kill "$UVICORN_PID"
    rm -f "$UVICORN_PID_FILE"
    echo "Python FastAPI bridge stopped."
else
    echo "No PID file found, Python FastAPI bridge might not have been started by this script."
fi
