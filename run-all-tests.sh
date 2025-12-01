#!/bin/bash
set -euo pipefail

# ═══════════════════════════════════════════════════════════════════════════════
# Universal Test Runner for Value Investor Bot
# ═══════════════════════════════════════════════════════════════════════════════
# Auto-detects environment, starts/stops services, runs complete test suite
#
# Usage:
#   ./run-all-tests.sh --quick    Fast mode: unit + integration only, no services
#   ./run-all-tests.sh            Full mode: auto-start services + all E2E tests
#
# Requirements: JDK 21, Node.js 24, Python 3.11
# ═══════════════════════════════════════════════════════════════════════════════

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# State
QUICK_MODE=false
SERVICES_STARTED=false
CLEANUP_REGISTERED=false

# Parse args
if [[ "${1:-}" == "--quick" ]]; then
    QUICK_MODE=true
fi

# Cleanup handler
cleanup() {
    if [[ "$SERVICES_STARTED" == "true" ]]; then
        echo -e "\n${YELLOW}Stopping services...${NC}"
        ./run.sh stop || true
    fi
}

register_cleanup() {
    if [[ "$CLEANUP_REGISTERED" == "false" ]]; then
        trap cleanup EXIT INT TERM
        CLEANUP_REGISTERED=true
    fi
}

# Helpers
print_header() {
    echo -e "\n${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC} ${BOLD}$1${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_step() {
    echo -e "${YELLOW}▸${NC} $1"
}

# Check port
is_port_open() {
    nc -z localhost "$1" 2>/dev/null || curl -s "http://localhost:$1" >/dev/null 2>&1
}

# Wait for service
wait_for_service() {
    local url="$1"
    local timeout="$2"
    local elapsed=0
    
    while [[ $elapsed -lt $timeout ]]; do
        if curl -sf "$url" >/dev/null 2>&1; then
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done
    return 1
}

# ═══════════════════════════════════════════════════════════════════════════════
# QUICK MODE: Unit + Integration tests only (no services needed)
# ═══════════════════════════════════════════════════════════════════════════════

if [[ "$QUICK_MODE" == "true" ]]; then
    print_header "🚀 QUICK MODE: Unit + Integration Tests"
    
    # Backend tests
    print_step "Running backend tests (Java)"
    cd "$SCRIPT_DIR/backend"
    jenv local 21.0 2>/dev/null || true
    if ! jenv exec mvn test -q; then
        print_error "Backend tests failed"
        exit 1
    fi
    print_success "Backend tests passed"
    
    # Python tests
    print_step "Running Python bridge tests"
    cd "$SCRIPT_DIR/shioaji_bridge"
    if [[ ! -d "venv" ]]; then
        python3 -m venv venv
    fi
    source venv/bin/activate
    pip install -q pytest pytest-mock requests httpx 2>/dev/null || pip install pytest pytest-mock requests httpx
    if ! python -m pytest tests/ --ignore=tests/test_api_e2e.py -q; then
        print_error "Python tests failed"
        deactivate
        exit 1
    fi
    deactivate
    print_success "Python tests passed"
    
    print_header "✅ ALL QUICK TESTS PASSED"
    exit 0
fi

# ═══════════════════════════════════════════════════════════════════════════════
# FULL MODE: Auto-start services + Complete test suite
# ═══════════════════════════════════════════════════════════════════════════════

print_header "🎯 FULL MODE: Complete Test Suite with E2E"

# Check if services are running
if is_port_open 8080; then
    print_success "Services already running on port 8080"
else
    print_step "Services not detected, starting them now..."
    register_cleanup
    
    # Determine password source
    if [[ -n "${JASYPT_PASSWORD:-}" ]]; then
        print_step "Using JASYPT_PASSWORD from environment (CI mode)"
        ./run.sh --detached
    elif [[ -n "${1:-}" ]]; then
        print_step "Using password from command line"
        ./run.sh "$1" --detached
    else
        print_step "Attempting to start with default password"
        ./run.sh dreamfulfil --detached
    fi
    
    SERVICES_STARTED=true
    
    # Wait for backend
    print_step "Waiting for backend to be ready (up to 90s)..."
    if ! wait_for_service "http://localhost:8080/api/health" 90; then
        print_error "Backend failed to start within 90 seconds"
        exit 1
    fi
    print_success "Backend is ready"
    
    # Brief stabilization
    sleep 3
fi

# Install Playwright browsers if needed
cd "$SCRIPT_DIR/frontend"
if ! npx playwright --version >/dev/null 2>&1 || ! ls ~/.cache/ms-playwright/chromium-* >/dev/null 2>&1; then
    print_step "Installing Playwright browsers..."
    npx playwright install --with-deps chromium
fi

# Run backend tests
print_header "🧪 Backend Tests (Java)"
cd "$SCRIPT_DIR/backend"
jenv local 21.0 2>/dev/null || true
if ! jenv exec mvn test -q; then
    print_error "Backend tests failed"
    exit 1
fi
print_success "Backend tests passed"

# Run Python tests
print_header "🐍 Python Bridge Tests"
cd "$SCRIPT_DIR/shioaji_bridge"
if [[ ! -d "venv" ]]; then
    python3 -m venv venv
fi
source venv/bin/activate
pip install -q pytest pytest-mock requests httpx 2>/dev/null || pip install pytest pytest-mock requests httpx
if ! python -m pytest tests/ --ignore=tests/test_api_e2e.py -q; then
    print_error "Python tests failed"
    deactivate
    exit 1
fi
deactivate
print_success "Python tests passed"

# Run E2E tests
print_header "🌐 E2E Tests (Playwright)"
cd "$SCRIPT_DIR/frontend"
if ! npx playwright test --reporter=list; then
    print_error "E2E tests failed"
    exit 1
fi
print_success "E2E tests passed"

print_header "✅ ALL TESTS PASSED (UNIT + INTEGRATION + E2E)"
exit 0
