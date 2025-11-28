#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════════
# Universal Test Runner for Value Investor Bot
# ═══════════════════════════════════════════════════════════════════════════════
# Executes all test suites: Backend (Java), Frontend (TypeScript), Python Bridge
# With full E2E browser automation support
#
# Usage: ./run-all-tests.sh [options]
# Options:
#   --unit        Run only unit tests
#   --integration Run only integration tests
#   --e2e         Run only E2E tests
#   --quick       Run quick tests only (skip E2E)
#   --verbose     Show detailed output
#   --help        Show this help message
# ═══════════════════════════════════════════════════════════════════════════════

set -e

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
PURPLE='\033[0;35m'
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

# ─────────────────────────────────────────────────────────────────────────────
# Parse command line arguments
# ─────────────────────────────────────────────────────────────────────────────

while [[ $# -gt 0 ]]; do
    case $1 in
        --unit)
            RUN_UNIT=true
            RUN_INTEGRATION=false
            RUN_E2E=false
            shift
            ;;
        --integration)
            RUN_UNIT=false
            RUN_INTEGRATION=true
            RUN_E2E=false
            shift
            ;;
        --e2e)
            RUN_UNIT=false
            RUN_INTEGRATION=false
            RUN_E2E=true
            shift
            ;;
        --quick)
            RUN_E2E=false
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  --unit        Run only unit tests"
            echo "  --integration Run only integration tests"
            echo "  --e2e         Run only E2E tests"
            echo "  --quick       Run quick tests only (skip E2E)"
            echo "  --verbose     Show detailed output"
            echo "  --help        Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

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

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

run_test() {
    local name="$1"
    local cmd="$2"
    local category="$3"
    
    print_step "Running: $name"
    
    if $VERBOSE; then
        if eval "$cmd"; then
            print_success "$name PASSED"
            case $category in
                unit) ((UNIT_PASSED++)) ;;
                integration) ((INTEGRATION_PASSED++)) ;;
                e2e) ((E2E_PASSED++)) ;;
            esac
            return 0
        else
            print_error "$name FAILED"
            case $category in
                unit) ((UNIT_FAILED++)) ;;
                integration) ((INTEGRATION_FAILED++)) ;;
                e2e) ((E2E_FAILED++)) ;;
            esac
            return 1
        fi
    else
        if eval "$cmd" > /tmp/test_output_$$.log 2>&1; then
            print_success "$name PASSED"
            case $category in
                unit) ((UNIT_PASSED++)) ;;
                integration) ((INTEGRATION_PASSED++)) ;;
                e2e) ((E2E_PASSED++)) ;;
            esac
            return 0
        else
            print_error "$name FAILED"
            echo -e "${RED}Output:${NC}"
            tail -20 /tmp/test_output_$$.log
            case $category in
                unit) ((UNIT_FAILED++)) ;;
                integration) ((INTEGRATION_FAILED++)) ;;
                e2e) ((E2E_FAILED++)) ;;
            esac
            return 1
        fi
    fi
}

check_command() {
    if ! command -v "$1" &> /dev/null; then
        print_warning "$1 is not installed"
        return 1
    fi
    return 0
}

# ─────────────────────────────────────────────────────────────────────────────
# 1. Dependency Check
# ─────────────────────────────────────────────────────────────────────────────

print_header "🔍 Dependency Check"

# Check for required tools
DEPS_OK=true

if check_command "java"; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
    print_success "Java found: $JAVA_VERSION"
else
    print_error "Java not found - backend tests will be skipped"
    DEPS_OK=false
fi

if check_command "mvn"; then
    MVN_VERSION=$(mvn -version 2>&1 | head -n 1 | awk '{print $3}')
    print_success "Maven found: $MVN_VERSION"
else
    print_error "Maven not found - backend tests will be skipped"
    DEPS_OK=false
fi

if check_command "node"; then
    NODE_VERSION=$(node --version)
    print_success "Node.js found: $NODE_VERSION"
else
    print_error "Node.js not found - frontend tests will be skipped"
    DEPS_OK=false
fi

if check_command "npm"; then
    NPM_VERSION=$(npm --version)
    print_success "npm found: $NPM_VERSION"
else
    print_error "npm not found - frontend tests will be skipped"
fi

if check_command "python3"; then
    PYTHON_VERSION=$(python3 --version)
    print_success "Python found: $PYTHON_VERSION"
else
    print_error "Python3 not found - Python bridge tests will be skipped"
    DEPS_OK=false
fi

# ─────────────────────────────────────────────────────────────────────────────
# 2. Service Status Check
# ─────────────────────────────────────────────────────────────────────────────

print_header "🔌 Service Status Check"

# Check if backend is running (needed for E2E tests)
if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
    print_success "Backend server is running on port 8080"
    BACKEND_RUNNING=true
else
    print_warning "Backend server is not running (E2E tests may be skipped)"
    BACKEND_RUNNING=false
fi

# Check if Shioaji bridge is running
if curl -s http://localhost:8888/health > /dev/null 2>&1; then
    print_success "Shioaji bridge is running on port 8888"
    SHIOAJI_RUNNING=true
else
    print_warning "Shioaji bridge is not running"
    SHIOAJI_RUNNING=false
fi

# Check if frontend dev server is running
if curl -s http://localhost:5173 > /dev/null 2>&1; then
    print_success "Frontend dev server is running on port 5173"
    FRONTEND_RUNNING=true
else
    print_warning "Frontend dev server is not running (will be started for E2E)"
    FRONTEND_RUNNING=false
fi

# ─────────────────────────────────════════════════════════════════════────────
# 3. Unit Tests
# ─────────────────────────────────────────────────────────────────────────────

if $RUN_UNIT; then
    print_header "🧪 Unit Tests"

    # ─────────────────────────────────────────────────────────────────────────
    # 3.1 Frontend Unit Tests (TypeScript/Vitest)
    # ─────────────────────────────────────────────────────────────────────────
    
    print_subheader "Frontend Unit Tests (TypeScript)"
    
    cd "$SCRIPT_DIR/frontend"
    
    # Install deps if needed
    if [ ! -d "node_modules" ]; then
        print_step "Installing frontend dependencies..."
        npm install --silent 2>/dev/null || npm install
    fi
    
    run_test "Frontend Unit Tests" "npm test -- --run 2>&1 | tail -50" "unit" || true

    # ─────────────────────────────────────────────────────────────────────────
    # 3.2 Backend Unit Tests (Java/JUnit)
    # ─────────────────────────────────────────────────────────────────────────
    
    print_subheader "Backend Unit Tests (Java)"
    
    cd "$SCRIPT_DIR/backend"
    
    # Run service tests (unit tests)
    run_test "Java Service Unit Tests" "mvn test -Dtest='*ServiceTest' -q 2>&1 | tail -30" "unit" || true
    
    # Run utility tests
    run_test "Java Utility Tests" "mvn test -Dtest='*UtilTest,OllamaClientTest,PythonExecutorTest' -q 2>&1 | tail -20" "unit" || true

    # ─────────────────────────────────────────────────────────────────────────
    # 3.3 Python Bridge Unit Tests (Pytest)
    # ─────────────────────────────────────────────────────────────────────────
    
    print_subheader "Python Bridge Unit Tests"
    
    cd "$SCRIPT_DIR/shioaji_bridge"
    
    # Setup Python virtual environment if needed
    if [ ! -d "venv" ]; then
        print_step "Creating Python virtual environment..."
        python3 -m venv venv
    fi
    
    source venv/bin/activate
    pip install -q pytest pytest-mock 2>/dev/null || pip install pytest pytest-mock
    
    run_test "Python Unit Tests" "python -m pytest tests/test_shioaji_client.py tests/test_shioaji_api.py -v 2>&1 | tail -40" "unit" || true
    
    deactivate
fi

# ─────────────────────────────────────────────────────────────────────────────
# 4. Integration Tests
# ─────────────────────────────────────────────────────────────────────────────

if $RUN_INTEGRATION; then
    print_header "🔗 Integration Tests"

    # ─────────────────────────────────────────────────────────────────────────
    # 4.1 Frontend Integration Tests
    # ─────────────────────────────────────────────────────────────────────────
    
    print_subheader "Frontend Integration Tests"
    
    cd "$SCRIPT_DIR/frontend"
    
    run_test "Frontend API Integration Tests" "npm test -- --run src/__tests__/integration.test.ts 2>&1 | tail -30" "integration" || true

    # ─────────────────────────────────────────────────────────────────────────
    # 4.2 Backend Integration Tests
    # ─────────────────────────────────────────────────────────────────────────
    
    print_subheader "Backend Integration Tests"
    
    cd "$SCRIPT_DIR/backend"
    
    # Controller tests (WebMvc integration)
    run_test "Java Controller Tests" "mvn test -Dtest='*ControllerTest' -q 2>&1 | tail -30" "integration" || true
    
    # Repository tests (Database integration)
    run_test "Java Repository Tests" "mvn test -Dtest='*RepositoryTest' -q 2>&1 | tail -20" "integration" || true
    
    # Full integration tests
    run_test "Java Integration Tests" "mvn test -Dtest='*IntegrationTest' -q 2>&1 | tail -30" "integration" || true

    # ─────────────────────────────────────────────────────────────────────────
    # 4.3 Contract Tests
    # ─────────────────────────────────────────────────────────────────────────
    
    print_subheader "Contract Tests (API Compatibility)"
    
    cd "$SCRIPT_DIR/backend"
    run_test "Java API Contract Tests" "mvn test -Dtest=ApiContractTest -q 2>&1 | tail -15" "integration" || true
    
    cd "$SCRIPT_DIR/frontend"
    run_test "Frontend API Contract Tests" "npm test -- --run src/__tests__/contract/api-contract.test.ts 2>&1 | tail -20" "integration" || true
    
    cd "$SCRIPT_DIR/shioaji_bridge"
    source venv/bin/activate
    run_test "Python Contract Tests" "python -m pytest tests/test_contracts.py -v 2>&1 | tail -25" "integration" || true
    deactivate

    # ─────────────────────────────────────────────────────────────────────────
    # 4.4 Python Integration Tests
    # ─────────────────────────────────────────────────────────────────────────
    
    print_subheader "Python Bridge Integration Tests"
    
    cd "$SCRIPT_DIR/shioaji_bridge"
    source venv/bin/activate
    run_test "Python Integration Tests" "python -m pytest tests/test_integration.py -v 2>&1 | tail -30" "integration" || true
    deactivate
fi

# ─────────────────────────────────────────────────────────────────────────────
# 5. E2E Tests
# ─────────────────────────────────────────────────────────────────────────────

if $RUN_E2E; then
    print_header "🌐 End-to-End Tests"
    
    cd "$SCRIPT_DIR/frontend"
    
    # Check if Playwright is installed
    if ! npm list @playwright/test > /dev/null 2>&1; then
        print_step "Installing Playwright..."
        npm install -D @playwright/test --legacy-peer-deps
        npx playwright install chromium
    fi
    
    # Check if services are running for E2E
    if ! $BACKEND_RUNNING; then
        print_warning "Backend not running - E2E tests require running services"
        print_step "Please start services with: ./run.sh <decrypt_key>"
        print_warning "Skipping E2E tests due to missing services"
    else
        # Run E2E tests with Playwright
        run_test "E2E Dashboard Tests" "npx playwright test e2e/dashboard.spec.ts --project=chromium 2>&1 | tail -40" "e2e" || true
        run_test "E2E Rebalance Flow Tests" "npx playwright test e2e/rebalance-flow.spec.ts --project=chromium 2>&1 | tail -40" "e2e" || true
    fi
fi

# ─────────────────────────────────────────────────────────────────────────────
# 6. Cleanup & Summary
# ─────────────────────────────────────────────────────────────────────────────

print_header "📊 Test Summary"

# Clean up temp files
rm -f /tmp/test_output_$$.log

# Calculate totals
TOTAL_PASSED=$((UNIT_PASSED + INTEGRATION_PASSED + E2E_PASSED))
TOTAL_FAILED=$((UNIT_FAILED + INTEGRATION_FAILED + E2E_FAILED))
TOTAL_TESTS=$((TOTAL_PASSED + TOTAL_FAILED))

echo ""
echo -e "${BOLD}Test Results by Category:${NC}"
echo ""

if $RUN_UNIT; then
    echo -e "  ${CYAN}Unit Tests:${NC}"
    echo -e "    ${GREEN}Passed: $UNIT_PASSED${NC}"
    echo -e "    ${RED}Failed: $UNIT_FAILED${NC}"
fi

if $RUN_INTEGRATION; then
    echo ""
    echo -e "  ${CYAN}Integration Tests:${NC}"
    echo -e "    ${GREEN}Passed: $INTEGRATION_PASSED${NC}"
    echo -e "    ${RED}Failed: $INTEGRATION_FAILED${NC}"
fi

if $RUN_E2E; then
    echo ""
    echo -e "  ${CYAN}E2E Tests:${NC}"
    echo -e "    ${GREEN}Passed: $E2E_PASSED${NC}"
    echo -e "    ${RED}Failed: $E2E_FAILED${NC}"
fi

echo ""
echo -e "${BOLD}─────────────────────────────────────────${NC}"
echo -e "${BOLD}Total: $TOTAL_TESTS tests${NC}"
echo -e "  ${GREEN}Passed: $TOTAL_PASSED${NC}"
echo -e "  ${RED}Failed: $TOTAL_FAILED${NC}"

echo ""

if [ $TOTAL_FAILED -eq 0 ]; then
    echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                                                            ║${NC}"
    echo -e "${GREEN}║          ✓ ALL TESTS PASSED - ALL SYSTEMS GO              ║${NC}"
    echo -e "${GREEN}║                                                            ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
    exit 0
else
    echo -e "${RED}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║                                                            ║${NC}"
    echo -e "${RED}║          ✗ SOME TESTS FAILED - REVIEW REQUIRED            ║${NC}"
    echo -e "${RED}║                                                            ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════════════════════════╝${NC}"
    exit 1
fi
