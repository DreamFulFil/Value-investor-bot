# Value Investor Bot — Taiwan Edition (v2.0)

🇹🇼 AI-powered automated value investing bot for Taiwan stock market

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

## ⚠️ Risk Warning

This is an **educational project**. Before going live:
1. Understand value investing principles
2. Run at least 12 months of backtest
3. Only invest money you can afford to lose
4. Past performance does not guarantee future results

---

MIT License | Educational purposes only | Not financial advice
