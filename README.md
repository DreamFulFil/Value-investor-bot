# Value Investor Bot — Taiwan Edition (v2.6)

🇹🇼 AI-powered automated value investing bot for Taiwan stock market

[![CI](https://github.com/DreamFulFil/Value-investor-bot/actions/workflows/ci.yml/badge.svg)](https://github.com/DreamFulFil/Value-investor-bot/actions/workflows/ci.yml)
[![Tests](https://img.shields.io/badge/tests-190%20Java%20%7C%2063%20Python%20%7C%2037%20E2E-brightgreen)](./run-tests.sh)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue)](https://react.dev/)
[![Shioaji](https://img.shields.io/badge/Shioaji-1.2.9-blue)](https://sinotrade.github.io/)
[![License](https://img.shields.io/badge/license-MIT-blue)](./LICENSE)

---

## 🔴 GOING LIVE – FINAL CHECKLIST

> **⚠️ WARNING: Once you activate LIVE mode, it is PERMANENT and cannot be reverted!**

Before clicking "Go Live", verify ALL of the following:

### Pre-Flight Checklist

| # | Item | Status |
|---|------|--------|
| 1 | ✅ Run at least **12 months of simulation** | Required |
| 2 | ✅ Verify Shioaji API credentials are correct | Required |
| 3 | ✅ Confirm your brokerage account has sufficient funds | Required |
| 4 | ✅ Understand that real money will be traded | Required |
| 5 | ✅ Accept that past performance ≠ future results | Required |

### What Happens When You Go Live

1. **Permanent DB Change**: `trading.mode = LIVE` is written to database
2. **No Revert**: The "Go Live" button disappears forever
3. **Real Orders**: On the 1st of each month, real buy orders execute via Shioaji
4. **Green Badge**: Dashboard shows permanent "🔴 LIVE MODE – Real Money" badge
5. **Startup Toast**: Every app start shows "LIVE mode active" notification

### Go Live Options

| Option | Description | Initial Deposit |
|--------|-------------|-----------------|
| **Start Fresh** | Begin new portfolio from today | Your choice (NT$16,000+) |
| **Gradual Catch-Up** | Sync to backtest over 6-18 months | Auto-calculated monthly |
| **One-Shot Match** | Instantly match backtest value | Full backtest value |

### After Going Live

- Orders execute automatically on the 1st of each month
- Failed orders retry 3 times with 2-second backoff
- Check Shioaji for order confirmations
- Monitor the dashboard for real portfolio performance

---

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
| `./run-tests.sh <key>` | Run all tests (Java + Python + E2E) |
| `./run-tests.sh help` | Show test runner documentation |

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
├── frontend/         # React 19 + TypeScript + Vite + Tailwind
├── shioaji_bridge/   # FastAPI Python (Shioaji 1.2.9 + Yahoo Finance)
├── contracts/        # Shared API contract definitions (JSON Schema)
├── .env              # Encrypted environment variables
├── run.sh            # Startup script (bulletproof, auto-opens browser)
└── run-tests.sh      # Complete test suite runner (auto-starts services)
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

Comprehensive test coverage with automated service management—no manual setup required.

### Quick Start

```bash
# Run all tests (auto-starts services if needed)
./run-tests.sh <jasypt-password>

# Show test runner help
./run-tests.sh help
```

### How It Works

The test runner automatically:
- Detects if services are already running
- Starts services in background if needed
- Waits for backend health check (up to 90s)
- Runs all test suites in sequence
- Stops services on completion or failure
- Cleans up properly on Ctrl+C

### Test Suites

| Suite | Count | Runtime | What's Tested |
|-------|-------|---------|---------------|
| **Java Tests** | 190 tests | ~90s | Core business logic, API contracts, integration |
| **Python Tests** | 63 tests | ~8s | Bridge logic, Shioaji client, API endpoints |
| **E2E Tests** | 37 scenarios | ~49s | Full user journeys with Playwright |
| **Total** | **290 tests** | **~3 min** | Complete system validation |

### Version Protection

To prevent issues with dependency version changes (e.g., Shioaji 1.1.4 disappearing):

```bash
# Verify specific version availability before upgrading
cd shioaji_bridge
python verify_dependencies.py shioaji 1.2.9
```

This script checks PyPI to ensure the version exists before attempting installation, preventing broken builds from vanished packages.

### Individual Test Commands
```bash
# Frontend Unit Tests (Vitest)
cd frontend && npm test              # Watch mode
cd frontend && npm run test:run      # Run once
cd frontend && npm run test:coverage # With coverage

# Frontend E2E Tests (Playwright)
cd frontend && npm run test:e2e      # Headless (CI-friendly)
cd frontend && npm run test:e2e:headed  # With browser visible
cd frontend && npm run test:e2e:ui   # Interactive UI mode

# Backend (Java/JUnit 5)
cd backend && mvn test                              # All tests
cd backend && mvn test -Dtest='*ServiceTest'        # Unit tests
cd backend && mvn test -Dtest='*IntegrationTest'    # Integration tests

# Python (pytest)
cd shioaji_bridge && source venv/bin/activate
pytest tests/ -v                                    # All tests
pytest tests/test_shioaji_client.py -v              # Unit tests
pytest tests/test_contracts.py -v                   # API contract tests
```

### Test Coverage Highlights
- ✅ **Unit Tests**: Isolated logic with mocked dependencies
- ✅ **Integration Tests**: Python bridge communication, API contracts
- ✅ **E2E Tests**: Complete user journeys with browser automation
- ✅ **Contract Tests**: Field name validation (quantity→shares)
- ✅ **Version Protection**: Dependency availability checks
- ✅ Null/undefined safety (prevents `toLocaleString()` crashes)
- ✅ Empty array/object handling
- ✅ Division by zero protection
- ✅ Responsive design validation
- ✅ Multi-browser compatibility (Chromium)

## Development

### Prerequisites
- Java 21 (use jenv: `jenv local 21`)
- Node.js 18+
- Python 3.10+ with venv
- Ollama (optional, for AI insights)
- Playwright browsers: `cd frontend && npx playwright install --with-deps chromium`

### Key Dependencies
- **Backend**: Spring Boot 3.3, Java 21, JUnit 5
- **Frontend**: React 19, TypeScript, Vite, Tailwind CSS, Playwright
- **Python Bridge**: FastAPI, Shioaji 1.2.9, yfinance, pytest

### Building
```bash
# Backend
cd backend && mvn clean package -DskipTests

# Frontend (outputs to backend/src/main/resources/static)
cd frontend && npm install && npm run build

# Python Bridge
cd shioaji_bridge && python3 -m venv venv && source venv/bin/activate
pip install -r requirements.txt
```

### Code Style
- Java: Google Java Style Guide
- TypeScript: ESLint + Prettier
- Python: Black + isort

## Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests (required: maintain contract test coverage)
4. Run `./run-tests.sh <password>` to verify all tests pass
5. Submit PR

**Important**: All 290 tests must pass before committing. CI will reject PRs with failing tests.

## Recent Updates (v2.6)

- ✅ Upgraded Shioaji from 1.1.4 to 1.2.9
- ✅ Added version protection script (`verify_dependencies.py`)
- ✅ Enhanced Python bridge error handling for invalid symbols
- ✅ Improved test automation with auto-service management
- ✅ Renamed `run-all-tests.sh` to `run-tests.sh` for clarity
- ✅ Added comprehensive integration tests for Python bridge
- ✅ All 290 tests passing (190 Java + 63 Python + 37 E2E)
