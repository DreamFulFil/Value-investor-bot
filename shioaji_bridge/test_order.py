#!/usr/bin/env python3
"""
Test script to simulate order placement without actual execution
Tests the complete flow including validation and JSON response format
"""
import sys
import json
from datetime import datetime

def simulate_order(action: str, symbol: str, quantity: str, price: str):
    """
    Simulate order placement and return mock JSON response

    Args:
        action: BUY or SELL
        symbol: Stock symbol
        quantity: Number of shares
        price: Limit price
    """
    print("=" * 60)
    print("Shioaji Order Simulation Test")
    print("=" * 60)
    print()

    # Validate action
    if action.upper() not in ['BUY', 'SELL']:
        response = {
            "success": False,
            "error": "Invalid action",
            "message": f"Action must be BUY or SELL, got: {action}",
            "timestamp": datetime.utcnow().isoformat() + "Z"
        }
        print("VALIDATION ERROR:")
        print(json.dumps(response, indent=2))
        return False

    # Validate quantity
    try:
        qty = float(quantity)
        if qty <= 0:
            raise ValueError("Quantity must be positive")
    except ValueError as e:
        response = {
            "success": False,
            "error": "Invalid quantity",
            "message": f"Quantity validation failed: {str(e)}",
            "timestamp": datetime.utcnow().isoformat() + "Z"
        }
        print("VALIDATION ERROR:")
        print(json.dumps(response, indent=2))
        return False

    # Validate price
    try:
        prc = float(price)
        if prc <= 0:
            raise ValueError("Price must be positive")
    except ValueError as e:
        response = {
            "success": False,
            "error": "Invalid price",
            "message": f"Price validation failed: {str(e)}",
            "timestamp": datetime.utcnow().isoformat() + "Z"
        }
        print("VALIDATION ERROR:")
        print(json.dumps(response, indent=2))
        return False

    # Display order details
    print("Order Details:")
    print(f"  Action:   {action.upper()}")
    print(f"  Symbol:   {symbol}")
    print(f"  Quantity: {qty}")
    print(f"  Price:    ${prc}")
    print()

    # Simulate successful response
    print("SIMULATED SUCCESS RESPONSE:")
    response = {
        "success": True,
        "order_id": f"SIM{int(datetime.now().timestamp())}",
        "message": f"{action.upper()} order placed successfully",
        "status": "FILLED",
        "filled_quantity": str(int(qty)),
        "filled_price": str(prc),
        "timestamp": datetime.utcnow().isoformat() + "Z"
    }
    print(json.dumps(response, indent=2))
    print()

    # Show what Java will parse
    print("=" * 60)
    print("Java Integration Test")
    print("=" * 60)
    print()
    print("Command that would be executed:")
    print(f"  python execute_order.py {action} {symbol} {qty} {prc}")
    print()
    print("Expected JSON response format:")
    print(json.dumps(response, indent=2))
    print()

    return True

def main():
    """Main entry point"""
    if len(sys.argv) != 5:
        print("Usage: python test_order.py <action> <symbol> <quantity> <price>")
        print()
        print("Example:")
        print("  python test_order.py BUY AAPL 10 150.00")
        print()
        sys.exit(1)

    action = sys.argv[1]
    symbol = sys.argv[2]
    quantity = sys.argv[3]
    price = sys.argv[4]

    try:
        success = simulate_order(action, symbol, quantity, price)

        if success:
            print("=" * 60)
            print("Simulation Test: PASSED")
            print("=" * 60)
            print()
            print("To place a REAL order, use:")
            print(f"  python execute_order.py {action} {symbol} {quantity} {price}")
            print()
            print("WARNING: Real orders will execute actual trades!")
            print()

        sys.exit(0 if success else 1)

    except Exception as e:
        print(f"\nERROR: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
