#!/bin/bash
# Value Investor Bot - Taiwan Edition
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
SHIOAJI_PID_FILE="$SCRIPT_DIR/.shioaji_api.pid"
DECRYPT_KEY="${1:-}"

stop_services() {
    echo -e "${BLUE}Stopping services...${NC}"
    [ -f "$SHIOAJI_PID_FILE" ] && kill -9 $(cat "$SHIOAJI_PID_FILE") 2>/dev/null; rm -f "$SHIOAJI_PID_FILE"
    pkill -f "shioaji_api.py" 2>/dev/null || true
    pkill -f "value-investor-bot" 2>/dev/null || true
    echo -e "${GREEN}Stopped${NC}"
}

show_status() {
    echo -e "${BLUE}Status:${NC}"
    curl -s http://localhost:11434/api/tags >/dev/null 2>&1 && echo -e "Ollama: ${GREEN}Running${NC}" || echo -e "Ollama: ${RED}Not running${NC}"
    curl -s http://127.0.0.1:8888/health >/dev/null 2>&1 && echo -e "Shioaji API: ${GREEN}Running${NC}" || echo -e "Shioaji API: ${RED}Not running${NC}"
    curl -s http://localhost:8080/api/health >/dev/null 2>&1 && echo -e "Backend: ${GREEN}Running${NC}" || echo -e "Backend: ${RED}Not running${NC}"
}

encrypt_env() {
    if [ -z "$DECRYPT_KEY" ]; then
        echo -e "${RED}Error: Key required${NC}"
        echo "Usage: ./run.sh <key> encrypt"
        exit 1
    fi
    cd shioaji_bridge && source venv/bin/activate 2>/dev/null || python3 -m venv venv && source venv/bin/activate
    pip install -q cryptography 2>/dev/null
    python3 config.py encrypt "$DECRYPT_KEY"
    deactivate
    cd "$SCRIPT_DIR"
}

case "${2:-$1}" in
    stop) stop_services; exit 0 ;;
    status) show_status; exit 0 ;;
    encrypt) encrypt_env; exit 0 ;;
    help|--help|-h) echo "Usage: ./run.sh <key> [start|stop|status|encrypt]"; exit 0 ;;
esac

case "$1" in stop|status|help|--help|-h) "$0" "" "$1"; exit 0 ;; esac

echo -e "${BLUE}========================================"
echo "  Value Investor Bot - Taiwan Edition"
echo -e "========================================${NC}"

echo -e "${BLUE}[1/4] Checking Ollama...${NC}"
curl -s http://localhost:11434/api/tags >/dev/null 2>&1 || echo -e "${YELLOW}Warning: Ollama not running${NC}"

echo -e "${BLUE}[2/4] Checking frontend...${NC}"
STATIC="backend/src/main/resources/static"
if [ ! -f "$STATIC/index.html" ]; then
    echo "Building frontend..."
    cd frontend && npm install --silent && npm run build --silent && cd ..
    mkdir -p "$STATIC" && rm -rf "$STATIC"/* && cp -r frontend/dist/* "$STATIC/"
fi

echo -e "${BLUE}[3/4] Starting Shioaji API...${NC}"
if [ -d "shioaji_bridge" ]; then
    cd shioaji_bridge
    [ ! -d "venv" ] && python3 -m venv venv
    source venv/bin/activate
    pip install -q -r requirements.txt cryptography 2>/dev/null
    DECRYPT_KEY="$DECRYPT_KEY" nohup python3 shioaji_api.py > shioaji_api.log 2>&1 & echo $! > "$SHIOAJI_PID_FILE"
    deactivate
    cd "$SCRIPT_DIR"
    sleep 2
fi

echo -e "${BLUE}[4/4] Starting backend...${NC}"
echo -e "${GREEN}========================================"
echo "  http://localhost:8080"
echo "  Press Ctrl+C to stop"
echo -e "========================================${NC}"

cd backend
JAR="target/value-investor-bot-0.0.1-SNAPSHOT.jar"
[ ! -f "$JAR" ] && ./mvnw clean package -DskipTests -q
java -jar "$JAR"
