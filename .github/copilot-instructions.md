# Universal Copilot / Claude Code Instructions

You are an expert full-stack engineer (Java 21 + Spring Boot, React + TypeScript + Vite, Python).  
Be precise, surgical, and extremely token-efficient.

## Core Rules — Never Break These

### 1. Imports First (Non-Negotiable)
- Add every required import in the same edit where the symbol is first used
- Never write code that references undefined classes/types
- Common Java imports to add instantly:  
  `java.io.*`, `java.nio.file.*`, `java.time.*`, `java.util.stream.*`, `StandardCharsets`, Lombok/Spring annotations

### 2. Targeted File Reads
- When line numbers are provided, read only ±30 lines around them (offset + limit)
- Only read entire files when genuinely needed for context

### 3. Always Build & Test Clean Before Completion
- Java → `mvn compile` then `mvn test`
- React/TypeScript → `npm run build` (or `vite build`)
- Python → `pytest`
- Never mark a task finished unless the project compiles cleanly and all tests pass

### 4. Preferred Shell Tools (Use Only These)
| Purpose               | Must Use    | Never Use             |
|-----------------------|-------------|-----------------------|
| Find files            | `fd`        | `find`, `ls -R`       |
| Search text           | `rg`        | `grep`, `ag`          |
| Code structure search | `ast-grep`  | regex/sed/grep        |
| Fuzzy selection       | `fzf`       | manual listing        |
| JSON processing       | `jq`        | python json.tool      |
| YAML processing       | `yq`        | manual parsing        |

### 5. Security
- Never commit plaintext secrets, API keys, passwords, or decrypt keys
- Sensitive values belong in encrypted config (Jasypt, .env + runtime decrypt, etc.)

### 6. Java Version (Enforced in pom.xml)
- All projects require and compile with exactly Java 21 via `<maven.compiler.release>21</maven.compiler.release>`
- Never use `./mvnw`, `jenv`, or local JDK overrides — pom.xml guarantees Java 21 everywhere

## Default Code Style
- React: functional components + hooks only
- TypeScript: strict mode, no `any`
- Prefer named exports over default exports
- Format with Prettier + ESLint
- Commit messages follow Conventional Commits

---

## Project-Specific Section (replace or delete per repository)

### Current Project: Value Investor Bot – Taiwan Edition
- Strategy: Monthly NT$16,000 into high-dividend Taiwan stocks only
- Target: ~NT$1,600/week passive income (long-term value, no day trading)

### Tech Stack
- Backend: Java 21 + Spring Boot 3.3
- Frontend: React 18 + TypeScript + Vite + Tailwind CSS
- Market data bridge: Python + Shioaji → Yahoo Finance fallback → cached price
- LLM: Ollama llama3.1:8b for insights only (never stock picking)

### Critical Business Rules
1. Rebalance exactly once per month and idempotent — block duplicates even with `force=true`
2. Store symbols with suffix (`2330.TW`, `2881.TWO`); strip only for Shioaji API calls
3. Screening: dividend yield > 2%, reasonable P/E & P/B, high ROE

### Rate Limits
- Shioaji ≤ 500 MB/day
- Yahoo Finance ~200 req/hour → add delays on fallback

### Common Commands
```bash
./run.sh <decrypt_key>   # start all services
./run.sh stop            # stop
./run.sh reset           # clean DB + restart
mvn test                 # backend tests
npm test                 # frontend tests
pytest                   # Python bridge tests