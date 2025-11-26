#!/usr/bin/env python3
"""
Test script to verify Shioaji API connection and authentication
Run this before attempting to place real orders
"""
import sys
import json
from config import Config
from shioaji_client import ShioajiClient

def test_connection():
    """Test Shioaji connection and authentication"""
    print("=" * 60)
    print("Shioaji Connection Test")
    print("=" * 60)
    print()

    # Load configuration
    print("1. Loading configuration...")
    config = Config()
    print(f"   {config}")
    print()

    # Validate credentials
    print("2. Validating credentials...")
    is_valid, error_msg = config.validate()
    if not is_valid:
        print(f"   FAILED: {error_msg}")
        print()
        print("Please set the following environment variables:")
        print("  - SHIOAJI_API_KEY")
        print("  - SHIOAJI_SECRET_KEY")
        print("  - SHIOAJI_PERSON_ID")
        print()
        print("Or create a .env file based on .env.example")
        return False
    print("   OK: All required credentials present")
    print()

    # Test login
    print("3. Testing API login...")
    client = ShioajiClient(config)
    success, message = client.login()

    if not success:
        print(f"   FAILED: {message}")
        return False

    print(f"   OK: {message}")
    print()

    # Test fetching positions
    print("4. Fetching account positions...")
    positions_result = client.get_positions()

    if positions_result.get("success"):
        count = positions_result.get("count", 0)
        print(f"   OK: Retrieved {count} positions")

        if count > 0:
            print()
            print("   Current Positions:")
            for pos in positions_result.get("positions", []):
                print(f"     - {pos.get('symbol')}: {pos.get('quantity')} shares @ ${pos.get('price')}")
    else:
        print(f"   WARNING: Could not fetch positions: {positions_result.get('error')}")
    print()

    # Test logout
    print("5. Testing logout...")
    success, message = client.logout()
    if success:
        print(f"   OK: {message}")
    else:
        print(f"   WARNING: {message}")
    print()

    # Summary
    print("=" * 60)
    print("Connection Test: PASSED")
    print("=" * 60)
    print()
    print("You can now place orders using execute_order.py")
    print()

    return True

def main():
    """Main entry point"""
    try:
        success = test_connection()
        sys.exit(0 if success else 1)

    except KeyboardInterrupt:
        print("\nTest cancelled by user")
        sys.exit(1)

    except Exception as e:
        print(f"\nFATAL ERROR: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
