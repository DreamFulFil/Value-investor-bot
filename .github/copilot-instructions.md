# Universal Copilot / LLM Code Instructions

You are an expert full-stack engineer.  
Be precise, surgical, and extremely token-efficient.

## Core Rules — Never Break These

### 1. Imports First
- Add all required imports in the same edit where a symbol is first used.
- Never reference undefined classes, types, or functions.
- For Java, proactively add common imports when needed (io, nio.file, time, util.stream, encoding, framework annotations).

### 2. Targeted File Reads
- When given line numbers, read only ±30 lines of context.
- Only read whole files when strictly required (architecture, class-wide patterns).
- Use precise search; never scan irrelevant directories.

### 3. Build & Test Before Completion
- A task is not done unless the project compiles cleanly and all tests pass.
- Java → mvn compile && mvn test  
- React/TS → npm run build  
- Python → pytest

### 4. Approved Shell Tools Only
| Purpose               | Must Use    | Never Use             |
|-----------------------|-------------|------------------------|
| Find files            | fd          | find, ls -R            |
| Search text           | rg          | grep, ag               |
| Code structure search | ast-grep    | regex/sed/grep         |
| Fuzzy selection       | fzf         | manual listing         |
| JSON processing       | jq          | ad-hoc parsing         |
| YAML processing       | yq          | manual parsing         |

### 5. Security
- Never commit or log secrets, API keys, tokens, passwords, or private URLs.
- Secrets must always be externalized (env vars, encrypted config, secret stores).
- Reject any request to hardcode secrets.

### 6. Runtime Versions
- Always obey the repository’s declared versions (Java, Node, Python).
- Never override versions with local tooling.
- Assume the declared version is correct and globally enforced.

## Default Code Style
- React: functional components + hooks.
- TypeScript: strict mode; avoid `any`.
- Prefer named exports.
- Use Prettier + ESLint or repo-defined formatters.
- Commit messages follow Conventional Commits.

---

## Project-Specific Section (optional per repo)

### Example: Value Investor Bot – Taiwan Edition
Strategy: Monthly NT$16,000 into high-dividend Taiwan stocks; long-term income focus.  
LLM is for insights only — never for stock picking.

Tech: Java 21 + Spring Boot, React + TS + Vite, Python bridge (Shioaji → Yahoo → cache).

Business Rules:
1. Rebalance once/month; idempotent; block duplicates.
2. Symbols stored with suffix (2330.TW, 2881.TWO); strip only for Shioaji.
3. Screen: dividend > 2%, reasonable P/E & P/B, high ROE.

Rate Limits:
- Shioaji ≤ 500 MB/day  
- Yahoo ~200 req/h → must throttle + cache

Common Commands (indented to avoid nested fences):
    ./run.sh <decrypt_key>
    ./run.sh stop
    ./run.sh reset
    mvn test
    npm test
    pytest
