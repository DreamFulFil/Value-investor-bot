# User-Level Claude Code Instructions

## Shell Tools Usage Guidelines
⚠️ **IMPORTANT**: Use the following specialized tools instead of traditional Unix commands: (Install if missing)
| Task Type | Must Use | Do Not Use |
|-----------|----------|------------|
| Find Files | `fd` | `find`, `ls -R` |
| Search Text | `rg` (ripgrep) | `grep`, `ag` |
| Analyze Code Structure | `ast-grep` | `grep`, `sed` |
| Interactive Selection | `fzf` | Manual filtering |
| Process JSON | `jq` | `python -m json.tool` |
| Process YAML/XML | `yq` | Manual parsing |

## File Reading Efficiency
⚠️ **IMPORTANT**: When given specific line numbers to modify:
- **DO NOT** read entire files unnecessarily
- **DO** use targeted reads with `offset` and `limit` parameters around the specified line numbers
- **ONLY** read the full file if broader context is genuinely required
- Trust the provided line numbers - be surgical, not exploratory

## Code Compilation Verification
⚠️ **IMPORTANT**: When all tasks are completed:
- **DO** run the appropriate build/compile command to verify all changes compile successfully
- **DO** fix any compilation errors before marking work as complete
- For Java projects: Use `mvn compile` or `mvn clean compile`
- For JavaScript/React projects: Use `npm run build` or equivalent
- Never consider a task fully complete without compilation verification

## Import Statement Management
🚨 **CRITICAL - DO NOT SKIP**: Add imports IMMEDIATELY when making code changes:
- **NEVER** add code using new classes without adding the corresponding import statements FIRST
- **ALWAYS** add imports in the SAME edit where you introduce new class usage
- **DO NOT** defer import additions to later - this wastes massive amounts of tokens on compilation errors
- When adding code that uses: ByteArrayOutputStream, FileOutputStream, InputStream, StandardCharsets, or any utility class → ADD THE IMPORT IMMEDIATELY
- Check BOTH standard library imports (java.io.*, java.nio.charset.*) AND project-specific imports (custom converters, utilities)
- Forgetting imports means wasting tokens on:
  1. Failed compilation
  2. Reading error messages
  3. Re-reading files to add imports
  4. Re-compilation
- **This is extremely wasteful - add imports when you write the code, not after it fails to compile**

## Testing Requirements
🚨 **CRITICAL**: Always check all unit and integration tests pass before you commit
- Run `mvn test` for Java backend tests
- Run `npm test` for React frontend tests
- Run `pytest` for Python bridge tests
- All tests MUST pass before any commit is made
- If tests fail, fix the issues before committing

---

# Project-Specific Notes (Value Investor Bot - Taiwan Edition)

## Overview
- Taiwan-focused value investing robo-advisor
- Monthly NT$16,000 investment in high-dividend Taiwan stocks (.TW)
- Target: NT$1,600/week passive income
- Backend: Java 21 + Spring Boot 3.3
- Frontend: React 18 + TypeScript + Vite + Tailwind
- Python Bridge: Shioaji integration for Taiwan market data
- LLM: Ollama (llama3.1:8b-instruct-q5_K_M) for insights ONLY - never for stock picking

## Taiwan Stock Symbols
- TSE stocks: Use `.TW` suffix (e.g., `2330.TW` for TSMC)
- OTC stocks: Use `.TWO` suffix (e.g., `2881.TWO`)
- Strip suffix when calling Shioaji API, add back for storage

## Critical Business Rules
1. **Rebalance Idempotency**: NEVER allow duplicate rebalances in same month
   - Even with `force=true`, same-month rebalances are BLOCKED
   - User must wait until next month
2. **Price Fallback**: When fetching historical prices:
   - Try Shioaji first
   - Fall back to Yahoo Finance if Shioaji fails/quota exceeded
   - Use most recent cached price as last resort
3. **Stock Screening**: Use dividend yield > 2%, low P/E, low P/B, high ROE
4. **No Day Trading**: Monthly rebalance only, boring value strategy

## API Quotas
- Shioaji: 500MB/day for non-real-transaction accounts
- Yahoo Finance: ~100-250 requests/hour soft limit
- Add delays between requests to avoid rate limits

## Security
- NEVER commit API keys, passwords, or decrypt keys
- Use jasypt encryption for sensitive values in .env
- Decrypt key passed at runtime: `./run.sh <decrypt_key>`

## Common Commands
```bash
./run.sh <key>    # Start with decrypt key
./run.sh stop     # Stop all services
./run.sh reset    # Reset database, clean start
./run.sh status   # Check service status
```

## Test Commands
```bash
cd backend && mvn test                    # Java tests
cd frontend && npm test                   # React tests  
cd shioaji_bridge && python -m pytest     # Python tests
```
