# Value Investor Bot — Taiwan Edition

AI-powered automated value investing bot for Taiwan stock market

## Investment Goal
- **Monthly Investment**: NT$16,000
- **Weekly Target**: NT$1,600 passive income
- **Universe**: Taiwan high-dividend stocks (.TW)

## Quick Start

```bash
# 1. Start Ollama
ollama pull llama3.1:8b-instruct-q5_K_M
ollama serve &

# 2. Start the application (decrypt key required)
./run.sh <decrypt-key>

# 3. Open browser
open http://localhost:8080
```

## Commands

| Command | Description |
|---------|-------------|
| `./run.sh <key>` | Start all services |
| `./run.sh <key> encrypt` | Encrypt .env sensitive values |
| `./run.sh stop` | Stop all services |
| `./run.sh status` | Show service status |

## .env Encryption

Sensitive values are encrypted with AES-256 (Jasypt compatible):
```
SHIOAJI_API_KEY=ENC(base64...)
SHIOAJI_SECRET_KEY=ENC(base64...)
```

## Architecture

```
localhost:8080  ← Spring Boot + React
     │
     ├── Ollama LLM (localhost:11434)
     └── Shioaji FastAPI (localhost:8888)
```

## Directory Structure

```
US-stock/
├── backend/          # Spring Boot (Java 21)
├── frontend/         # React 18 + TypeScript
├── shioaji_bridge/   # FastAPI Python
├── .env              # Encrypted environment variables
└── run.sh            # Startup script
```

## Security

- ✅ Default SIMULATION mode
- ✅ LLM for explanations only, never stock picking
- ✅ .env sensitive values encrypted
- ✅ Same-month duplicate rebalance prevention

MIT License | Educational purposes only
