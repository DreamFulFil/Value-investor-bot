#!/bin/bash
# Value Investor Bot - Taiwan Edition
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
SHIOAJI_PID_FILE="$SCRIPT_DIR/.shioaji_api.pid"
BACKEND_PID_FILE="$SCRIPT_DIR/.backend.pid"

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
    
    # Stop Shioaji API
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
    
    # Stop Backend
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
    
    # Release ports
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
    if curl -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo -e "  Backend:     ${GREEN}✓ Running${NC}"
    else
        echo -e "  Backend:     ${RED}✗ Not running${NC}"
    fi
}

clean_env() {
    echo -e "${BLUE}Cleaning environment...${NC}"
    stop_services
    rm -f backend/database.db
    rm -f backend/*.db
    rm -rf backend/target
    rm -rf backend/src/main/resources/static/*
    rm -rf frontend/node_modules frontend/dist
    rm -f shioaji_bridge/*.log
    echo -e "${GREEN}Environment cleaned. Run './run.sh <key>' to start fresh.${NC}"
}

reset_portfolio() {
    echo -e "${BLUE}Resetting portfolio data (keeping stock universe)...${NC}"
    rm -f backend/database.db
    rm -f backend/*.db
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

echo -e "${BLUE}========================================"
echo "  Value Investor Bot - Taiwan Edition"
echo -e "========================================${NC}"

# Step 1: Check Ollama
echo -e "${BLUE}[1/4] Checking Ollama...${NC}"
if curl -s http://localhost:11434/api/tags >/dev/null 2>&1; then
    echo -e "  ${GREEN}✓ Ollama is running${NC}"
else
    echo -e "  ${YELLOW}○ Ollama not running (AI insights disabled)${NC}"
fi

# Step 2: Build frontend if needed
echo -e "${BLUE}[2/4] Checking frontend...${NC}"
STATIC="backend/src/main/resources/static"
if [ ! -f "$STATIC/index.html" ] || [ ! -f "$STATIC/assets/"*.js ]; then
    echo "  Building frontend..."
    cd frontend
    npm install --silent 2>/dev/null
    npm run build --silent 2>/dev/null
    cd "$SCRIPT_DIR"
    mkdir -p "$STATIC"
    rm -rf "$STATIC"/*
    cp -r frontend/dist/* "$STATIC/"
    echo -e "  ${GREEN}✓ Frontend built${NC}"
else
    echo -e "  ${GREEN}✓ Frontend ready${NC}"
fi

# Step 3: Start Shioaji API
echo -e "${BLUE}[3/4] Starting Shioaji API...${NC}"
if [ -d "shioaji_bridge" ]; then
    cd shioaji_bridge
    [ ! -d "venv" ] && python3 -m venv venv
    source venv/bin/activate
    pip install -q -r requirements.txt cryptography yfinance 2>/dev/null
    
    # Start in background with proper signal handling
    DECRYPT_KEY="$DECRYPT_KEY" python3 shioaji_api.py > shioaji_api.log 2>&1 &
    SHIOAJI_PID=$!
    echo $SHIOAJI_PID > "$SHIOAJI_PID_FILE"
    
    deactivate
    cd "$SCRIPT_DIR"
    
    # Wait for Shioaji to be ready
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
        echo -e "\n  ${YELLOW}○ Shioaji API slow to start (using Yahoo Finance fallback)${NC}"
    fi
fi

# Step 4: Build and start backend
echo -e "${BLUE}[4/4] Starting backend...${NC}"
cd backend
JAR="target/value-investor-bot-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR" ]; then
    echo "  Building backend..."
    mvn clean package -DskipTests -q
    echo -e "  ${GREEN}✓ Backend built${NC}"
fi

echo -e "${GREEN}========================================"
echo "  Value Investor Bot Started"
echo "  Dashboard: http://localhost:8080"
echo "  Press Ctrl+C to stop"
echo -e "========================================${NC}"

# Trap to cleanup on exit
cleanup() {
    echo ""
    echo -e "${BLUE}Shutting down...${NC}"
    cd "$SCRIPT_DIR"
    stop_services
}
trap cleanup EXIT INT TERM

# Start backend in foreground
java -jar "$JAR"
