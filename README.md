# Value Investor Bot — Taiwan Edition (v2.4)

🇹🇼 AI-powered automated value investing bot for Taiwan stock market

[![Tests](https://img.shields.io/badge/tests-14%20suites%20passing-brightgreen)](./run-all-tests.sh)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue)](https://react.dev/)
[![License](https://img.shields.io/badge/license-MIT-blue)](./LICENSE)

## Investment Goal
- **Monthly Investment**: NT$16,000
- **Weekly Target**: NT$1,600 passive income
- **Universe**: Taiwan high-dividend stocks (.TW)
- **Strategy**: Monthly rebalance, top 5 undervalued dividend stocks

## Quick Start

```bash
# 1. Start Ollama (optional - for AI insights)
ollama pull llama3.1:8b-instruct-q5_K_M
ollama serve &

# 2. Start the application (decrypt key required)
./run.sh <decrypt-key>

# Browser opens automatically with loading splash screen
# Dashboard ready in ~8 seconds
```

## How It Works

### Backtest Mode (Default)
1. Click the big blue "Run First Monthly Rebalance" button
2. The bot invests NT$16,000/month into top 5 dividend stocks
3. **Idempotent**: Clicking twice in the same month does nothing (safe!)
4. Run 12-24 months of backtest to see performance

### Go Live (3 Options)
After backtesting, click "Go Live" to start real trading:

| Option | Description | Best For |
|--------|-------------|----------|
| **Start Fresh** | Deposit NT$16,000+ and begin new portfolio from today | New investors |
| **Gradual Catch-Up** | Sync to backtest over 6-18 months | Budget-conscious |
| **One-Shot Match** | Deposit full amount to instantly match backtest | Ready to commit |

## Commands

| Command | Description |
|---------|-------------|
| `./run.sh <key>` | Start all services (auto-opens browser) |
| `./run.sh stop` | Stop all services |
| `./run.sh status` | Show service status |
| `./run.sh reset` | Reset portfolio data (fresh start) |
| `./run.sh clean` | Full cleanup (rebuild required) |
| `./run.sh <key> encrypt` | Encrypt .env credentials |
| `./run-all-tests.sh` | Run all tests (Unit + Integration + E2E) |
| `./run-all-tests.sh --quick` | Run tests without E2E (faster) |
| `./run-all-tests.sh --unit` | Run only unit tests |
| `./run-all-tests.sh --e2e` | Run only E2E browser tests |

## .env Encryption

Sensitive values are encrypted with AES-256 (Jasypt compatible):
```
SHIOAJI_API_KEY=ENC(base64...)
SHIOAJI_SECRET_KEY=ENC(base64...)
SHIOAJI_PERSON_ID=ENC(base64...)
```

## Architecture

```
localhost:8080  ← Spring Boot + React Dashboard
     │
     ├── Ollama LLM (localhost:11434) - AI insights only
     ├── Shioaji FastAPI (localhost:8888) - Taiwan stock data
     └── Yahoo Finance (fallback) - Historical prices
```

## Directory Structure

```
Value-investor-bot/
├── backend/          # Spring Boot 3.3 (Java 21)
├── frontend/         # React 18 + TypeScript + Tailwind
├── shioaji_bridge/   # FastAPI Python (Shioaji + Yahoo Finance)
├── contracts/        # Shared API contract definitions (JSON Schema)
├── .env              # Encrypted environment variables
├── run.sh            # Startup script (bulletproof, auto-opens browser)
└── run-all-tests.sh  # Universal test runner (Unit/Integration/E2E)
```

## Safety Features

- ✅ **Default SIMULATION mode** - No real money until you go live
- ✅ **Idempotent rebalance** - Same-month clicks are ignored
- ✅ **LLM for explanations only** - Never picks stocks
- ✅ **Encrypted credentials** - AES-256 encryption
- ✅ **Historical price catch-up** - Uses actual past prices for backtest
- ✅ **Quota auto-managed** - Shioaji primary, Yahoo Finance backup for sim mode
- ✅ **Loading splash screen** - Never see blank page on startup

## API Quota Management

The dashboard shows real-time Shioaji API quota usage:
- **Green (0-50%)**: Normal usage, Shioaji active
- **Yellow (50-90%)**: Moderate usage, monitor closely
- **Red (>90%)**: High usage, auto-fallback to Yahoo Finance

When Shioaji quota is low (<50MB remaining), the system automatically:
1. Switches to Yahoo Finance for historical price data
2. Displays a warning banner on the dashboard
3. Continues rebalancing without interruption

**Note**: Shioaji accounts without real transaction history have a 500MB/day limit.

## ⚠️ Risk Warning

This is an **educational project**. Before going live:
1. Understand value investing principles
2. Run at least 12 months of backtest
3. Only invest money you can afford to lose
4. Past performance does not guarantee future results

---

MIT License | Educational purposes only | Not financial advice

## Testing

The project includes comprehensive test coverage across all stacks, organized into Unit, Integration, and E2E layers.

**Latest Test Run:** All 14 test suites passing ✅

### Run All Tests
```bash
# Start services first (required for E2E tests)
./run.sh <decrypt_key>

# Run all tests
./run-all-tests.sh
```

### Test Options
```bash
./run-all-tests.sh              # Run all tests (Unit + Integration + E2E)
./run-all-tests.sh --quick      # Skip E2E tests (faster)
./run-all-tests.sh --unit       # Run only unit tests
./run-all-tests.sh --integration # Run only integration tests
./run-all-tests.sh --e2e        # Run only E2E browser tests
./run-all-tests.sh --verbose    # Show detailed output
```

### Test Breakdown

| Category | Stack | Suite | Individual Tests |
|----------|-------|-------|------------------|
| **Unit** | Frontend (TypeScript) | 1 | 68 tests |
| **Unit** | Backend (Java) | 2 | 50+ tests |
| **Unit** | Python Bridge | 1 | 16 tests |
| **Integration** | Frontend | 1 | Included above |
| **Integration** | Backend | 3 | 30+ tests |
| **Integration** | Python | 1 | Included above |
| **Contract** | All stacks | 3 | API compatibility |
| **E2E** | Playwright | 2 | 37 tests × 5 browsers |
| | | **14 suites** | **250+ test cases** |

### E2E Tests (Playwright)

Browser automation tests covering critical user journeys:

```bash
# Install Playwright browsers
cd frontend && npm run playwright:install

# Run E2E tests
npm run test:e2e              # Headless (CI-friendly)
npm run test:e2e:headed       # With browser visible
npm run test:e2e:ui           # Interactive UI mode
```

**E2E Test Coverage:**
- Dashboard page load and display
- Rebalance button interaction
- Progress modal tracking
- First-time user journey
- Same-month idempotency check
- Error handling and recovery
- Responsive design (mobile/tablet)
- Language toggle (EN/中文)

### Contract Tests (Most Critical)
These tests catch field name mismatches between backend and frontend:
- `contracts/api-contracts.json` - Shared schema definitions
- Java `ApiContractTest` - Verify DTOs serialize correctly
- TypeScript `api-contract.test.ts` - Verify field mapping (quantity→shares)
- Python `test_contracts.py` - Verify response formats

### Individual Test Commands
```bash
# Frontend Unit Tests (Vitest)
cd frontend && npm test              # Watch mode
cd frontend && npm run test:run      # Run once
cd frontend && npm run test:unit     # Unit only
cd frontend && npm run test:coverage # With coverage

# Frontend E2E Tests (Playwright)
cd frontend && npm run test:e2e      # All browsers
cd frontend && npx playwright test --project=chromium  # Chrome only

# Backend (Java/JUnit 5)
cd backend && mvn test                              # All tests
cd backend && mvn test -Dtest='*ServiceTest'        # Unit tests
cd backend && mvn test -Dtest='*ControllerTest'     # Controller tests
cd backend && mvn test -Dtest='*IntegrationTest'    # Integration tests

# Python (pytest)
cd shioaji_bridge && source venv/bin/activate
pytest tests/ -v                                    # All tests
pytest tests/test_shioaji_client.py -v              # Unit tests
pytest tests/test_integration.py -v                 # Integration tests
```

### Test Coverage Highlights
- ✅ **Unit Tests**: Isolated logic testing with mocked dependencies
- ✅ **Integration Tests**: Component interaction and API contracts
- ✅ **E2E Tests**: Full user journey with Playwright browser automation
- ✅ Null/undefined safety (prevents `toLocaleString()` crashes)
- ✅ API field name contracts (catches `quantity` vs `shares` mismatches)
- ✅ Empty array/object handling
- ✅ Division by zero protection
- ✅ Negative value formatting
- ✅ Responsive design validation
- ✅ Multi-browser compatibility (Chrome, Firefox, Safari, Mobile)

## Development

### Prerequisites
- Java 21 (use jenv: `jenv local 21`)
- Node.js 18+
- Python 3.10+
- Ollama (optional)
- Playwright browsers (for E2E tests): `cd frontend && npm run playwright:install`

### Building
```bash
# Backend
cd backend && mvn clean package -DskipTests

# Frontend (outputs to backend/src/main/resources/static)
cd frontend && npm install && npm run build
```

### Code Style
- Java: Google Java Style Guide
- TypeScript: ESLint + Prettier
- Python: Black + isort

## Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests (required: maintain 100% contract test coverage)
4. Run `./run-all-tests.sh` to verify all tests pass
5. Submit PR

**Important**: All tests must pass before committing. The CI will reject PRs with failing tests.
