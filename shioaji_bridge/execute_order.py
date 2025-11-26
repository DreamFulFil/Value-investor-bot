#!/usr/bin/env python3
"""
Main execution script for Shioaji order placement
Called by Java PythonExecutor with command-line arguments
Returns JSON response to stdout for Java to parse
"""
import sys
import json
import logging
import signal
from datetime import datetime
from typing import Dict, Any
from config import Config
from shioaji_client import ShioajiClient

# Configure logging (only to file, not stdout to keep stdout clean for JSON)
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('shioaji_bridge.log')
    ]
)
logger = logging.getLogger(__name__)

class TimeoutException(Exception):
    """Exception raised when operation times out"""
    pass

def timeout_handler(signum, frame):
    """Signal handler for timeout"""
    raise TimeoutException("Operation timed out")

def validate_inputs(action: str, symbol: str, quantity_str: str, price_str: str) -> tuple[bool, str, float, float]:
    """
    Validate command-line inputs

    Args:
        action: BUY or SELL
        symbol: Stock symbol
        quantity_str: Quantity as string
        price_str: Price as string

    Returns:
        tuple: (is_valid, error_message, quantity_float, price_float)
    """
    # Validate action
    if action.upper() not in ['BUY', 'SELL']:
        return False, f"Invalid action: {action}. Must be BUY or SELL", 0.0, 0.0

    # Validate symbol (basic check)
    if not symbol or len(symbol) > 10 or not symbol.isalnum():
        return False, f"Invalid symbol: {symbol}", 0.0, 0.0

    # Validate quantity
    try:
        quantity = float(quantity_str)
        if quantity <= 0:
            return False, f"Quantity must be positive: {quantity}", 0.0, 0.0
    except ValueError:
        return False, f"Invalid quantity format: {quantity_str}", 0.0, 0.0

    # Validate price
    try:
        price = float(price_str)
        if price <= 0:
            return False, f"Price must be positive: {price}", 0.0, 0.0
    except ValueError:
        return False, f"Invalid price format: {price_str}", 0.0, 0.0

    return True, "", quantity, price

def create_error_response(error: str, message: str) -> Dict[str, Any]:
    """
    Create standardized error response

    Args:
        error: Error type
        message: Error message

    Returns:
        dict: Error response
    """
    return {
        "success": False,
        "error": error,
        "message": message,
        "timestamp": datetime.utcnow().isoformat() + "Z"
    }

def execute_order(action: str, symbol: str, quantity: float, price: float) -> Dict[str, Any]:
    """
    Execute order via Shioaji API

    Args:
        action: BUY or SELL
        symbol: Stock symbol
        quantity: Number of shares
        price: Limit price

    Returns:
        dict: Order result
    """
    try:
        # Load configuration
        config = Config()

        # Validate credentials
        is_valid, error_msg = config.validate()
        if not is_valid:
            logger.error(f"Configuration validation failed: {error_msg}")
            return create_error_response("Configuration error", error_msg)

        logger.info(f"Executing order: {action} {quantity} {symbol} @ ${price}")

        # Create client and execute order
        client = ShioajiClient(config)

        # Login
        success, message = client.login()
        if not success:
            logger.error(f"Login failed: {message}")
            return create_error_response("Authentication failed", message)

        # Place order
        result = client.place_order(action, symbol, quantity, price)

        # Logout
        client.logout()

        logger.info(f"Order execution completed: {result.get('success')}")
        return result

    except TimeoutException:
        error_msg = f"Operation timed out after {config.get_timeout()} seconds"
        logger.error(error_msg)
        return create_error_response("Timeout error", error_msg)

    except Exception as e:
        error_msg = f"Unexpected error: {str(e)}"
        logger.error(error_msg, exc_info=True)
        return create_error_response("Execution error", error_msg)

def main():
    """Main entry point"""
    try:
        # Check command-line arguments
        if len(sys.argv) != 5:
            response = create_error_response(
                "Invalid arguments",
                f"Usage: python execute_order.py <action> <symbol> <quantity> <price>\n"
                f"Received {len(sys.argv) - 1} arguments: {sys.argv[1:]}"
            )
            print(json.dumps(response, indent=2))
            sys.exit(1)

        # Parse arguments
        action = sys.argv[1]
        symbol = sys.argv[2]
        quantity_str = sys.argv[3]
        price_str = sys.argv[4]

        logger.info(f"Received command: {action} {symbol} {quantity_str} {price_str}")

        # Validate inputs
        is_valid, error_msg, quantity, price = validate_inputs(action, symbol, quantity_str, price_str)
        if not is_valid:
            response = create_error_response("Validation error", error_msg)
            print(json.dumps(response, indent=2))
            sys.exit(1)

        # Set timeout (30 seconds)
        signal.signal(signal.SIGALRM, timeout_handler)
        signal.alarm(30)

        # Execute order
        result = execute_order(action, symbol, quantity, price)

        # Cancel timeout
        signal.alarm(0)

        # Output JSON response to stdout
        print(json.dumps(result, indent=2))

        # Exit with appropriate code
        sys.exit(0 if result.get("success") else 1)

    except KeyboardInterrupt:
        response = create_error_response("Interrupted", "Operation cancelled by user")
        print(json.dumps(response, indent=2))
        sys.exit(1)

    except Exception as e:
        response = create_error_response("Fatal error", str(e))
        print(json.dumps(response, indent=2))
        logger.error("Fatal error in main", exc_info=True)
        sys.exit(1)

if __name__ == "__main__":
    main()
