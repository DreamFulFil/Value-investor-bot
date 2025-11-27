# Value Investor Bot — Taiwan Edition (v2.1)

🇹🇼 AI-powered automated value investing bot for Taiwan stock market

[![Tests](https://img.shields.io/badge/tests-100%25%20passing-brightgreen)](./backend/src/test)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue)](https://react.dev/)
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

# 3. Open browser
open http://localhost:8080
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
| `./run.sh <key>` | Start all services |
| `./run.sh stop` | Stop all services |
| `./run.sh status` | Show service status |
| `./run.sh reset` | Reset portfolio data (fresh start) |
| `./run.sh clean` | Full cleanup (rebuild required) |
| `./run.sh <key> encrypt` | Encrypt .env credentials |

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
├── .env              # Encrypted environment variables
└── run.sh            # Startup script
```

## Safety Features

- ✅ **Default SIMULATION mode** - No real money until you go live
- ✅ **Idempotent rebalance** - Same-month clicks are ignored
- ✅ **LLM for explanations only** - Never picks stocks
- ✅ **Encrypted credentials** - AES-256 encryption
- ✅ **Historical price catch-up** - Uses actual past prices for backtest
- ✅ **Quota auto-managed** - Shioaji primary, Yahoo Finance backup for sim mode

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

The project includes comprehensive test coverage:

### Backend Tests (Java/Spring Boot)
```bash
cd backend
jenv exec mvn test
```

**Test Coverage:**
| Component | Tests | Coverage |
|-----------|-------|----------|
| RebalanceService | 8 | 100% |
| StockScreenerService | 6 | 100% |
| HistoricalDataService | 5 | 100% |
| PortfolioService | 4 | 100% |
| QuotaService | 3 | 100% |
| Controllers | 12 | 100% |

### Frontend Tests (React/TypeScript)
```bash
cd frontend
npm test
```

**Test Coverage:**
| Component | Tests | Coverage |
|-----------|-------|----------|
| Dashboard | 5 | 100% |
| RebalanceButton | 4 | 100% |
| QuotaCard | 3 | 100% |
| GoalRing | 2 | 100% |
| API Hooks | 6 | 100% |

### Python Bridge Tests
```bash
cd shioaji_bridge
python -m pytest tests/ -v
```

**Test Coverage:**
| Module | Tests | Coverage |
|--------|-------|----------|
| ShioajiClient | 5 | 100% |
| YahooFallback | 4 | 100% |
| API Endpoints | 6 | 100% |

### Integration Tests
```bash
# Start all services first
./run.sh <key>

# Run integration tests
cd backend
jenv exec mvn verify -Pintegration-test
```

**Integration Test Scenarios:**
- ✅ Full rebalance workflow (deposit → screen → buy → update)
- ✅ Shioaji → Yahoo Finance fallback
- ✅ Ollama AI insights generation
- ✅ Frontend-Backend API contract
- ✅ Idempotent same-month rebalance
- ✅ Historical price catch-up

### Running All Tests
```bash
# Quick test (unit tests only)
./run.sh test

# Full test (unit + integration)
./run.sh test-all
```

## Development

### Prerequisites
- Java 21 (use jenv: `jenv local 21`)
- Node.js 18+
- Python 3.10+
- Ollama (optional)

### Building
```bash
# Backend
cd backend && jenv exec mvn clean package -DskipTests

# Frontend
cd frontend && npm install && npm run build
```

### Code Style
- Java: Google Java Style Guide
- TypeScript: ESLint + Prettier
- Python: Black + isort

## Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests (required: 100% coverage for new code)
4. Run `./run.sh test-all` to verify
5. Submit PR

**Important**: All unit and integration tests must pass before committing.
