#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════════
# Universal Test Runner for Value Investor Bot
# ═══════════════════════════════════════════════════════════════════════════════
# Executes all test suites: Backend (Java), Frontend (TypeScript), Python Bridge
# With full E2E browser automation support for CI environments.
#
# Usage: ./run-all-tests.sh [options] [decrypt_key]
# Options:
#   --unit        Run only unit tests
#   --integration Run only integration tests
#   --e2e         Run only E2E tests
#   --quick       Run quick tests only (skip E2E)
#   --verbose     Show detailed output
#   --help        Show this help message
#
# The [decrypt_key] is optional and can be provided as the last argument,
# or through the JASYPT_PASSWORD environment variable.
# For CI, it's recommended to use the environment variable.
# Example for E2E tests: ./run-all-tests.sh --e2e dreamfulfil
# ═══════════════════════════════════════════════════════════════════════════════

set -e

# Ensure all background processes are killed on exit
trap 'kill $(jobs -p 2>/dev/null) &> /dev/null' EXIT

# ─────────────────────────────────────────────────────────────────────────────
# 0. Environment Setup & Color Codes
# ─────────────────────────────────────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Counters
UNIT_PASSED=0
UNIT_FAILED=0
INTEGRATION_PASSED=0
INTEGRATION_FAILED=0
E2E_PASSED=0
E2E_FAILED=0

# Options
RUN_UNIT=true
RUN_INTEGRATION=true
RUN_E2E=true
VERBOSE=false
DECRYPT_KEY=""

# ─────────────────────────────────────────────────────────────────────────────
# Helper Functions
# ─────────────────────────────────────────────────────────────────────────────

print_header() {
    echo ""
    echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC} ${BOLD}$1${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
}

print_subheader() {
    echo ""
    echo -e "${CYAN}┌──────────────────────────────────────────────────────────────┐${NC}"
    echo -e "${CYAN}│${NC} $1"
    echo -e "${CYAN}└──────────────────────────────────────────────────────────────┘${NC}"
}

print_step() {
    echo -e "${YELLOW}▸${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

show_help() {
    grep -E '^# ' "$0"
}

run_test() {
    local name="$1"
    local cmd="$2"
    local category="$3"
    
    print_step "Running: $name"
    
    local output_file
    output_file=$(mktemp)
    
    if $VERBOSE; then
        if eval "$cmd"; then
            print_success "$name PASSED"
            case $category in
                unit) ((UNIT_PASSED++));;
                integration) ((INTEGRATION_PASSED++));;
                e2e) ((E2E_PASSED++));;
            esac
            rm -f "$output_file"
            return 0
        else
            print_error "$name FAILED"
            case $category in
                unit) ((UNIT_FAILED++));;
                integration) ((INTEGRATION_FAILED++));;
                e2e) ((E2E_FAILED++));;
            esac
            rm -f "$output_file"
            return 1
        fi
    else
        if eval "$cmd" > "$output_file" 2>&1; then
            print_success "$name PASSED"
            case $category in
                unit) ((UNIT_PASSED++));;
                integration) ((INTEGRATION_PASSED++));;
                e2e) ((E2E_PASSED++));;
            esac
            rm -f "$output_file"
            return 0
        else
            print_error "$name FAILED"
            echo -e "${RED}Output:${NC}"
            tail -20 "$output_file"
            case $category in
                unit) ((UNIT_FAILED++));;
                integration) ((INTEGRATION_FAILED++));;
                e2e) ((E2E_FAILED++));;
            esac
            rm -f "$output_file"
            return 1
        fi
    fi
}

# ─────────────────────────────────────────────────────────────────────────────
# 1. Argument Parsing
# ─────────────────────────────────────────────────────────────────────────────

# First, check for JASYPT_PASSWORD environment variable (common in CI)
if [ -n "$JASYPT_PASSWORD" ]; then
    DECRYPT_KEY="$JASYPT_PASSWORD"
    print_step "Using DECRYPT_KEY from JASYPT_PASSWORD environment variable."
fi

# Parse options
while [[ $# -gt 0 ]]; do
    case "$1" in
        --unit) RUN_UNIT=true; RUN_INTEGRATION=false; RUN_E2E=false; shift ;;
        --integration) RUN_UNIT=false; RUN_INTEGRATION=true; RUN_E2E=false; shift ;;
        --e2e) RUN_UNIT=false; RUN_INTEGRATION=false; RUN_E2E=true; shift ;;
        --quick) RUN_E2E=false; shift ;;
        --verbose) VERBOSE=true; shift ;;
        --help) show_help; exit 0 ;;
        -*) print_error "Unknown option: $1"; exit 1 ;;
        *) break ;; # Found first non-option argument, assume it's the decrypt key
    esac
done

# If DECRYPT_KEY is still empty, try to get it from the remaining argument
if [ -z "$DECRYPT_KEY" ] && [ -n "$1" ]; then
    DECRYPT_KEY="$1"
    shift
fi

# Any remaining arguments are invalid
if [ "$#" -gt 0 ]; then
    print_error "Too many arguments."
    exit 1
fi

export DECRYPT_KEY

# ─────────────────────────────────────────────────────────────────────────────
# 2. Unit & Integration Tests (Non-E2E)
# ─────────────────────────────────────────────────────────────────────────────

# --- Backend Build & Test ---
if $RUN_UNIT || $RUN_INTEGRATION; then
    print_header "📦🧪 Backend Build & Test (Java/Maven)"
    cd "$SCRIPT_DIR/backend"
    
    print_step "Compiling and installing backend to local Maven repository..."
    if ! mvn clean install -DskipTests=true -B -q; then
        print_error "Backend build failed. Running with full output..."
        mvn clean install -DskipTests=true -B
        exit 1
    fi
    print_success "Backend built and installed successfully."

    if $RUN_UNIT; then
        run_test "Backend Unit Tests" "mvn test -Dtest='*ServiceTest,*UtilTest'" "unit" || true
    fi
    if $RUN_INTEGRATION; then
        run_test "Backend Integration Tests" "mvn test -Dtest='*ControllerTest,*RepositoryTest,*IntegrationTest,ApiContractTest'" "integration" || true
    fi
fi

# --- Frontend Tests ---
if $RUN_UNIT; then
    print_header "🧪 Frontend Unit Tests"
    cd "$SCRIPT_DIR/frontend"
    if [ ! -d "node_modules" ]; then print_step "Installing frontend dependencies..."; npm install; fi
    run_test "Frontend Unit Tests" "npm test -- --run" "unit" || true
fi
if $RUN_INTEGRATION; then
    print_header "🔗 Frontend Integration & Contract Tests"
    cd "$SCRIPT_DIR/frontend"
    if [ ! -d "node_modules" ]; then print_step "Installing frontend dependencies..."; npm install; fi
    run_test "Frontend Integration Tests" "npm test -- --run src/__tests__/integration.test.ts src/__tests__/contract/api-contract.test.ts" "integration" || true
fi

# --- Python Bridge Tests ---
if $RUN_UNIT; then
    print_header "🧪 Python Bridge Unit Tests"
    cd "$SCRIPT_DIR/shioaji_bridge"
    if [ ! -d "venv" ]; then print_step "Creating Python venv..."; python3 -m venv venv; fi
    source venv/bin/activate
    pip install -r requirements.txt > /dev/null
    run_test "Python Bridge Unit Tests" "pytest tests/test_shioaji*.py" "unit" || true
    deactivate
fi
if $RUN_INTEGRATION; then
    print_header "🔗 Python Bridge Integration & Contract Tests"
    cd "$SCRIPT_DIR/shioaji_bridge"
    source venv/bin/activate
    run_test "Python Bridge Integration Tests" "pytest tests/test_integration.py tests/test_contracts.py" "integration" || true
    deactivate
fi

# ─────────────────────────────────────────────────────────────────────────────
# 3. E2E Tests
# ─────────────────────────────────────────────────────────────────────────────

if $RUN_E2E; then
    print_header "🌐 End-to-End Tests"

    if [ -z "$DECRYPT_KEY" ]; then
        print_error "Decrypt key is required to start services for E2E tests."
        print_step "Usage: ./run-all-tests.sh --e2e [decrypt_key]"
        exit 1
    fi

    # Start the full application stack in the background
    print_step "Starting application stack with key '$DECRYPT_KEY'..."
    ./run.sh "$DECRYPT_KEY" &> /tmp/run_sh.log &

    # Wait for the frontend to be available
    print_step "Waiting for application to be live at http://localhost:8080..."
    for i in {1..60}; do
        if curl --silent --fail http://localhost:8080/api/health > /dev/null; then
            print_success "Application is live!"
            break
        fi
        if [ $i -eq 60 ]; then
            print_error "Application failed to start within 60 seconds."
            echo "--- Log from run.sh ---"
            cat /tmp/run_sh.log
            exit 1
        fi
        echo -n "."
        sleep 1
    done

    # Run Playwright tests
    cd "$SCRIPT_DIR/frontend"
    print_step "Installing Playwright browsers if necessary..."
    npx playwright install --with-deps chromium > /dev/null

    print_step "Running all E2E tests..."
    # Run tests from both the root e2e directory and the frontend e2e directory
    run_test "Playwright E2E Tests" "npx playwright test ../e2e frontend/e2e --project=chromium" "e2e"

    # The 'trap' command will handle cleanup
fi

# ─────────────────────────────────────────────────────────────────────────────
# 4. Summary
# ─────────────────────────────────────────────────────────────────────────────

print_header "📊 Test Summary"

TOTAL_PASSED=$((UNIT_PASSED + INTEGRATION_PASSED + E2E_PASSED))
TOTAL_FAILED=$((UNIT_FAILED + INTEGRATION_FAILED + E2E_FAILED))

echo -e "  ${GREEN}Passed: $TOTAL_PASSED${NC}"
echo -e "  ${RED}Failed: $TOTAL_FAILED${NC}"
echo ""

if [ $TOTAL_FAILED -eq 0 ]; then
    print_success "All tests passed!"
    exit 0
else
    print_error "Some tests failed."
    exit 1
fi