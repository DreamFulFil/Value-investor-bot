#!/bin/bash
# Value Investor Bot - Taiwan Edition
# Bulletproof startup script - perfect dashboard every time
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; CYAN='\033[0;36m'; NC='\033[0m'
SHIOAJI_PID_FILE="$SCRIPT_DIR/.shioaji_api.pid"
BACKEND_PID_FILE="$SCRIPT_DIR/.backend.pid"
BUILD_TIMESTAMP_FILE="$SCRIPT_DIR/.frontend_build_timestamp"

# Parse arguments
DECRYPT_KEY=""
COMMAND=""
for arg in "$@"; do
    case "$arg" in
        stop|status|encrypt|clean|reset|help|--help|-h) COMMAND="$arg" ;;
        *) DECRYPT_KEY="$arg" ;;
    esac
done

stop_services() {
    echo -e "${BLUE}Stopping services...${NC}"
    
    # Stop Shioaji API gracefully
    if [ -f "$SHIOAJI_PID_FILE" ]; then
        PID=$(cat "$SHIOAJI_PID_FILE")
        if kill -0 "$PID" 2>/dev/null; then
            kill "$PID" 2>/dev/null
            sleep 1
            kill -9 "$PID" 2>/dev/null || true
        fi
        rm -f "$SHIOAJI_PID_FILE"
    fi
    pkill -f "shioaji_api.py" 2>/dev/null || true
    
    # Stop Backend gracefully
    if [ -f "$BACKEND_PID_FILE" ]; then
        PID=$(cat "$BACKEND_PID_FILE")
        if kill -0 "$PID" 2>/dev/null; then
            kill "$PID" 2>/dev/null
            sleep 1
            kill -9 "$PID" 2>/dev/null || true
        fi
        rm -f "$BACKEND_PID_FILE"
    fi
    pkill -f "value-investor-bot" 2>/dev/null || true
    
    # Release ports forcefully
    lsof -ti:8888 | xargs kill -9 2>/dev/null || true
    lsof -ti:8080 | xargs kill -9 2>/dev/null || true
    
    sleep 1
    echo -e "${GREEN}All services stopped${NC}"
}

show_status() {
    echo -e "${BLUE}Service Status:${NC}"
    if curl -s http://localhost:11434/api/tags >/dev/null 2>&1; then
        echo -e "  Ollama:      ${GREEN}✓ Running${NC}"
    else
        echo -e "  Ollama:      ${YELLOW}○ Not running (optional)${NC}"
    fi
    if curl -s http://127.0.0.1:8888/health >/dev/null 2>&1; then
        echo -e "  Shioaji API: ${GREEN}✓ Running${NC}"
    else
        echo -e "  Shioaji API: ${RED}✗ Not running${NC}"
    fi
    if curl -s http://localhost:8080/api/health >/dev/null 2>&1; then
        echo -e "  Backend:     ${GREEN}✓ Running${NC}"
    else
        echo -e "  Backend:     ${RED}✗ Not running${NC}"
    fi
}

clean_env() {
    echo -e "${BLUE}Cleaning environment...${NC}"
    stop_services
    rm -f backend/database.db backend/*.db
    rm -rf backend/target
    rm -rf backend/src/main/resources/static/*
    rm -rf frontend/node_modules frontend/dist
    rm -f shioaji_bridge/*.log
    rm -f "$BUILD_TIMESTAMP_FILE"
    echo -e "${GREEN}Environment cleaned. Run './run.sh <key>' to start fresh.${NC}"
}

reset_portfolio() {
    echo -e "${BLUE}Resetting portfolio data (keeping stock universe)...${NC}"
    rm -f backend/database.db backend/*.db
    echo -e "${GREEN}Portfolio reset. Stocks will be re-fetched on next start.${NC}"
}

encrypt_env() {
    if [ -z "$DECRYPT_KEY" ]; then
        echo -e "${RED}Error: Encryption key required${NC}"
        echo "Usage: ./run.sh <key> encrypt"
        exit 1
    fi
    cd shioaji_bridge
    [ ! -d "venv" ] && python3 -m venv venv
    source venv/bin/activate
    pip install -q cryptography 2>/dev/null
    python3 config.py encrypt "$DECRYPT_KEY"
    deactivate
    cd "$SCRIPT_DIR"
    echo -e "${GREEN}Encryption complete${NC}"
}

show_help() {
    echo "Value Investor Bot - Taiwan Edition"
    echo ""
    echo "Usage: ./run.sh <decrypt_key> [command]"
    echo ""
    echo "Commands:"
    echo "  (default)   Start all services"
    echo "  stop        Stop all services"
    echo "  status      Show service status"
    echo "  clean       Stop services and reset everything"
    echo "  reset       Reset portfolio data only (fresh start)"
    echo "  encrypt     Encrypt .env credentials"
    echo "  help        Show this help"
    echo ""
    echo "Examples:"
    echo "  ./run.sh mykey          # Start with decrypt key"
    echo "  ./run.sh stop           # Stop all services"
    echo "  ./run.sh reset          # Reset portfolio for fresh start"
    echo "  ./run.sh mykey encrypt  # Encrypt credentials"
}

# Check if frontend needs rebuild
frontend_needs_rebuild() {
    STATIC="$SCRIPT_DIR/backend/src/main/resources/static"
    
    # Must rebuild if no index.html or no JS bundle
    if [ ! -f "$STATIC/index.html" ]; then
        return 0
    fi
    
    # Check if any JS file exists in assets
    if ! ls "$STATIC/assets/"*.js 1>/dev/null 2>&1; then
        return 0
    fi
    
    # Check if source files are newer than build
    if [ -f "$BUILD_TIMESTAMP_FILE" ]; then
        LAST_BUILD=$(cat "$BUILD_TIMESTAMP_FILE")
        NEWEST_SRC=$(find frontend/src -type f -name "*.tsx" -o -name "*.ts" -o -name "*.css" 2>/dev/null | xargs stat -f "%m" 2>/dev/null | sort -rn | head -1)
        if [ -n "$NEWEST_SRC" ] && [ "$NEWEST_SRC" -gt "$LAST_BUILD" ]; then
            return 0
        fi
    else
        return 0
    fi
    
    return 1
}

build_frontend() {
    echo "  Building frontend..."
    cd "$SCRIPT_DIR/frontend"
    
    # Install deps if needed
    if [ ! -d "node_modules" ]; then
        npm install --silent 2>/dev/null
    fi
    
    # Build
    npm run build 2>/dev/null
    
    cd "$SCRIPT_DIR"
    
    # Copy to static resources (Vite already outputs to backend/src/main/resources/static via config)
    # But let's ensure it's there
    STATIC="$SCRIPT_DIR/backend/src/main/resources/static"
    if [ -d "frontend/dist" ] && [ ! -f "$STATIC/index.html" ]; then
        mkdir -p "$STATIC"
        cp -r frontend/dist/* "$STATIC/"
    fi
    
    # Record build timestamp
    date +%s > "$BUILD_TIMESTAMP_FILE"
    
    echo -e "  ${GREEN}✓ Frontend built${NC}"
}

open_browser() {
    URL="http://localhost:8080?t=$(date +%s)"
    
    # macOS
    if command -v open &>/dev/null; then
        open "$URL" 2>/dev/null &
    # Linux
    elif command -v xdg-open &>/dev/null; then
        xdg-open "$URL" 2>/dev/null &
    # WSL
    elif command -v wslview &>/dev/null; then
        wslview "$URL" 2>/dev/null &
    fi
}

wait_for_backend() {
    echo -n "  Waiting for backend"
    for i in {1..30}; do
        if curl -s http://localhost:8080/api/health >/dev/null 2>&1; then
            echo -e "\n  ${GREEN}✓ Backend ready${NC}"
            return 0
        fi
        echo -n "."
        sleep 0.5
    done
    echo -e "\n  ${YELLOW}○ Backend slow to start${NC}"
    return 1
}

# Handle commands
case "$COMMAND" in
    stop) stop_services; exit 0 ;;
    status) show_status; exit 0 ;;
    clean) clean_env; exit 0 ;;
    reset) reset_portfolio; exit 0 ;;
    encrypt) encrypt_env; exit 0 ;;
    help|--help|-h) show_help; exit 0 ;;
esac

# Require decrypt key to start services
if [ -z "$DECRYPT_KEY" ]; then
    echo -e "${RED}Error: Decrypt key required to start services${NC}"
    echo "Usage: ./run.sh <decrypt_key>"
    echo "Run './run.sh help' for more options"
    exit 1
fi

# Cleanup before starting
stop_services 2>/dev/null

echo ""
echo -e "${CYAN}╔════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  ${GREEN}Value Investor Bot - Taiwan Edition${CYAN}  ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════╝${NC}"
echo ""

# Step 1: Check Ollama
echo -e "${BLUE}[1/5] Checking Ollama...${NC}"
if curl -s http://localhost:11434/api/tags >/dev/null 2>&1; then
    echo -e "  ${GREEN}✓ Ollama is running${NC}"
else
    echo -e "  ${YELLOW}○ Ollama not running (AI insights disabled)${NC}"
fi

# Step 2: Build frontend if needed (smart rebuild)
echo -e "${BLUE}[2/5] Checking frontend...${NC}"
if frontend_needs_rebuild; then
    build_frontend
else
    echo -e "  ${GREEN}✓ Frontend up-to-date${NC}"
fi

# Step 3: Start Shioaji API
echo -e "${BLUE}[3/5] Starting Shioaji API...${NC}"
if [ -d "shioaji_bridge" ]; then
    cd shioaji_bridge
    [ ! -d "venv" ] && python3 -m venv venv
    source venv/bin/activate
    pip install -q -r requirements.txt cryptography yfinance 2>/dev/null
    
    DECRYPT_KEY="$DECRYPT_KEY" python3 shioaji_api.py > shioaji_api.log 2>&1 &
    SHIOAJI_PID=$!
    echo $SHIOAJI_PID > "$SHIOAJI_PID_FILE"
    
    deactivate
    cd "$SCRIPT_DIR"
    
    echo -n "  Waiting for Shioaji API"
    for i in {1..10}; do
        if curl -s http://127.0.0.1:8888/health >/dev/null 2>&1; then
            echo -e "\n  ${GREEN}✓ Shioaji API started${NC}"
            break
        fi
        echo -n "."
        sleep 1
    done
    
    if ! curl -s http://127.0.0.1:8888/health >/dev/null 2>&1; then
        echo -e "\n  ${YELLOW}○ Shioaji API slow (Yahoo Finance fallback active)${NC}"
    fi
fi

# Step 4: Build backend JAR if needed
echo -e "${BLUE}[4/5] Checking backend...${NC}"
cd "$SCRIPT_DIR/backend"
JAR="target/value-investor-bot-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR" ]; then
    echo "  Building backend JAR..."
    mvn clean package -DskipTests -q
    echo -e "  ${GREEN}✓ Backend built${NC}"
else
    echo -e "  ${GREEN}✓ Backend JAR ready${NC}"
fi

# Step 5: Start backend and open browser
echo -e "${BLUE}[5/5] Starting backend...${NC}"

# Start backend in background first
java -jar "$JAR" > "$SCRIPT_DIR/.backend.log" 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > "$BACKEND_PID_FILE"

# Wait for backend to be ready
if wait_for_backend; then
    # Open browser with cache-busting timestamp
    open_browser
fi

echo ""
echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║  🚀 Value Investor Bot is LIVE!        ║${NC}"
echo -e "${GREEN}║  Dashboard: http://localhost:8080      ║${NC}"
echo -e "${GREEN}║  Press Ctrl+C to stop                  ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
echo ""

# Trap to cleanup on exit
cleanup() {
    echo ""
    echo -e "${BLUE}Shutting down gracefully...${NC}"
    cd "$SCRIPT_DIR"
    stop_services
}
trap cleanup EXIT INT TERM

# Tail backend logs in foreground (keeps script running)
tail -f "$SCRIPT_DIR/.backend.log"
