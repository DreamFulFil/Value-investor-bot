# Shioaji Bridge for Value Investor Bot

Python bridge for executing real trades with Sinopac Shioaji broker from Java backend.

## Overview

This bridge provides a command-line interface for the Java backend to execute US stock trades through the Shioaji API. Orders are placed via Python scripts that return JSON responses for parsing by Java.

## Directory Structure

```
shioaji_bridge/
├── config.py              # Configuration management
├── shioaji_client.py      # Shioaji API wrapper class
├── execute_order.py       # Main execution script (called by Java)
├── test_connection.py     # Test authentication and connection
├── test_order.py          # Simulate order without execution
├── requirements.txt       # Python dependencies
├── .env.example           # Environment variable template
├── .env                   # Your actual credentials (create this, never commit!)
├── shioaji_bridge.log     # Log file (auto-generated)
└── README.md              # This file
```

## Installation

### 1. Prerequisites

- Python 3.8 or higher
- Sinopac/Shioaji brokerage account with API access
- API credentials (API Key, Secret Key, Person ID)

### 2. Install Python Dependencies

```bash
cd /Users/gc/Downloads/work/US-stock/shioaji_bridge
pip install -r requirements.txt
```

Or using virtual environment (recommended):

```bash
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### 3. Configure Environment Variables

Copy the example environment file:

```bash
cp .env.example .env
```

Edit `.env` and add your Shioaji credentials:

```bash
SHIOAJI_API_KEY=your_actual_api_key_here
SHIOAJI_SECRET_KEY=your_actual_secret_key_here
SHIOAJI_PERSON_ID=your_person_id_here
SHIOAJI_SIMULATION=false  # Set to 'true' for paper trading
SHIOAJI_TIMEOUT=30
SHIOAJI_LOG_LEVEL=INFO
```

**IMPORTANT:** Never commit the `.env` file to version control!

### 4. Test Connection

Before placing real orders, test your connection:

```bash
python test_connection.py
```

Expected output:
```
============================================================
Shioaji Connection Test
============================================================

1. Loading configuration...
   Config(api_key=***, secret_key=***, person_id=***, simulation=False, timeout=30)

2. Validating credentials...
   OK: All required credentials present

3. Testing API login...
   OK: Login successful

4. Fetching account positions...
   OK: Retrieved 5 positions

5. Testing logout...
   OK: Logout successful

============================================================
Connection Test: PASSED
============================================================
```

## Usage

### Command-Line Interface

```bash
python execute_order.py <action> <symbol> <quantity> <price>
```

**Parameters:**
- `action`: `BUY` or `SELL`
- `symbol`: Stock ticker symbol (e.g., `AAPL`, `GOOGL`)
- `quantity`: Number of shares (fractional shares will be rounded down)
- `price`: Limit price per share

**Example:**
```bash
python execute_order.py BUY AAPL 10 150.00
```

### Test Order (Simulation)

Simulate an order without actual execution:

```bash
python test_order.py BUY AAPL 10 150.00
```

This validates inputs and shows the expected JSON response format.

### Java Integration

The Java `PythonExecutor` class calls the script like this:

```java
ProcessBuilder pb = new ProcessBuilder(
    "python",
    "/Users/gc/Downloads/work/US-stock/shioaji_bridge/execute_order.py",
    "BUY",
    "AAPL",
    "10.5",
    "150.00"
);
```

## JSON Response Format

### Success Response

```json
{
  "success": true,
  "order_id": "ORD123456",
  "message": "BUY order placed successfully",
  "status": "FILLED",
  "filled_quantity": "10",
  "filled_price": "150.25",
  "timestamp": "2024-12-01T10:30:00Z",
  "warning": "Rounded to 10 shares (fractional shares not supported)"
}
```

**Fields:**
- `success`: `true` if order placed successfully
- `order_id`: Unique order identifier from Shioaji
- `message`: Human-readable success message
- `status`: Order status (`SUBMITTED`, `FILLED`, `PARTIAL`, etc.)
- `filled_quantity`: Number of shares executed
- `filled_price`: Execution price per share
- `timestamp`: ISO 8601 timestamp (UTC)
- `warning`: Optional warning message (e.g., rounding fractional shares)

### Error Response

```json
{
  "success": false,
  "error": "Authentication failed",
  "message": "Invalid API credentials",
  "timestamp": "2024-12-01T10:30:00Z"
}
```

**Fields:**
- `success`: `false` for errors
- `error`: Error category/type
- `message`: Detailed error description
- `timestamp`: ISO 8601 timestamp (UTC)

### Common Error Types

| Error Type | Description |
|------------|-------------|
| `Configuration error` | Missing or invalid environment variables |
| `Authentication failed` | Invalid API credentials or login failure |
| `Validation error` | Invalid input parameters |
| `Contract not found` | Stock symbol not found in Shioaji |
| `Order execution failed` | Order placement rejected by broker |
| `Timeout error` | Operation exceeded 30-second timeout |
| `Execution error` | Unexpected runtime error |

## Logging

All operations are logged to `shioaji_bridge.log` in the same directory.

**Log format:**
```
2024-12-01 10:30:00,123 - __main__ - INFO - Executing order: BUY 10 AAPL @ $150.0
2024-12-01 10:30:01,456 - shioaji_client - INFO - Successfully logged in. Accounts: 1
2024-12-01 10:30:02,789 - shioaji_client - INFO - Order placed successfully. Order ID: ORD123456
```

**Note:** Logs never contain sensitive data (API keys, passwords).

## Important Notes

### Fractional Shares
Shioaji typically does not support fractional shares for US stocks. The bridge automatically rounds down to whole shares:
- Input: `10.7` shares → Executed: `10` shares
- Input: `0.5` shares → Error (rounds to 0)

### Order Types
- **Order Type:** Limit orders (LMT)
- **Time in Force:** Rest of Day (ROD)
- **Market Orders:** Not currently supported (can be added)

### Timeout Protection
All operations timeout after 30 seconds (configurable via `SHIOAJI_TIMEOUT`).

### Simulation Mode
For testing, set `SHIOAJI_SIMULATION=true` in `.env` to use paper trading.

## Security Considerations

1. **Never hardcode credentials** - Always use environment variables or `.env` file
2. **Never commit `.env`** - Add to `.gitignore`
3. **Secure log files** - Logs never contain sensitive data, but restrict file permissions:
   ```bash
   chmod 600 .env
   chmod 600 shioaji_bridge.log
   ```
4. **Input validation** - All parameters validated before API calls
5. **Error handling** - Graceful degradation on failures

## Troubleshooting

### "SHIOAJI_API_KEY environment variable is not set"
- Ensure `.env` file exists and contains valid credentials
- Check environment variable names match exactly

### "Login failed: Invalid API credentials"
- Verify API key and secret key are correct
- Check if API access is enabled in your Shioaji account
- Ensure person ID matches your account

### "Contract not found for symbol: AAPL"
- Verify the symbol is available for US stock trading via Shioaji
- Check if contracts are properly downloaded during login
- Some symbols may require different formatting

### "Operation timed out after 30 seconds"
- Check internet connection
- Shioaji API may be slow or unresponsive
- Increase timeout: `SHIOAJI_TIMEOUT=60` in `.env`

### Import Error: "No module named 'shioaji'"
- Install dependencies: `pip install -r requirements.txt`
- Activate virtual environment if using one

## Testing Guide

### 1. Test Connection
```bash
python test_connection.py
```
Verifies authentication and API connectivity.

### 2. Simulate Order
```bash
python test_order.py BUY AAPL 10 150.00
```
Tests input validation and JSON response format without placing real orders.

### 3. Place Test Order (Simulation Mode)
Set `SHIOAJI_SIMULATION=true` in `.env`, then:
```bash
python execute_order.py BUY AAPL 1 150.00
```
Places order in paper trading environment.

### 4. Place Real Order
Set `SHIOAJI_SIMULATION=false` in `.env`, then:
```bash
python execute_order.py BUY AAPL 1 150.00
```
**WARNING:** This executes a real trade with real money!

## Integration with Java Backend

### PythonExecutor Usage

```java
// Example: Execute BUY order
String pythonScript = "/Users/gc/Downloads/work/US-stock/shioaji_bridge/execute_order.py";
String action = "BUY";
String symbol = "AAPL";
String quantity = "10.5";
String price = "150.00";

ProcessBuilder pb = new ProcessBuilder("python", pythonScript, action, symbol, quantity, price);
pb.redirectErrorStream(true);
Process process = pb.start();

// Read JSON response from stdout
BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
StringBuilder jsonResponse = new StringBuilder();
String line;
while ((line = reader.readLine()) != null) {
    jsonResponse.append(line);
}

// Parse JSON
JSONObject response = new JSONObject(jsonResponse.toString());
boolean success = response.getBoolean("success");

if (success) {
    String orderId = response.getString("order_id");
    // Handle success...
} else {
    String error = response.getString("error");
    // Handle error...
}
```

### Error Handling in Java

```java
try {
    // Execute order...
    int exitCode = process.waitFor();

    if (exitCode != 0) {
        // Non-zero exit code indicates error
        // Parse error JSON from response
    }
} catch (Exception e) {
    // Handle Java-side exceptions
}
```

## Next Steps for Frontend Agent

1. **Display Order Confirmation**
   - Show order details before execution
   - Display JSON response (success/error) to user
   - Format order status and filled quantities

2. **Order History View**
   - Parse JSON responses from backend
   - Display order history table with status
   - Show order IDs for tracking

3. **Error Handling UI**
   - Display error messages from JSON response
   - Show user-friendly error explanations
   - Provide retry options for failed orders

4. **Real-time Order Status**
   - Poll backend for order status updates
   - Display live order fills and status changes
   - Show notifications for completed orders

## Support

For issues with:
- **Shioaji API:** Contact Sinopac support or check [Shioaji documentation](https://sinotrade.github.io/)
- **Bridge code:** Review logs in `shioaji_bridge.log`
- **Java integration:** Check stdout/stderr from Python process

## License

This bridge is part of the Value Investor Bot application.
