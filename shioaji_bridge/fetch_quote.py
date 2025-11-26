#!/usr/bin/env python3
"""
Fetch current quote for a US stock symbol via Shioaji
Usage: python fetch_quote.py SYMBOL
Returns: JSON with quote data
"""
import sys
import json
import logging
from config import Config
from shioaji_client import ShioajiClient

logging.basicConfig(level=logging.ERROR)
logger = logging.getLogger(__name__)

def fetch_quote(symbol: str) -> dict:
    """
    Fetch current quote for a symbol

    Args:
        symbol: Stock symbol (e.g., 'AAPL')

    Returns:
        dict: Quote data with price, symbol, etc.
    """
    try:
        config = Config()
        client = ShioajiClient(config)

        # Login
        success, message = client.login()
        if not success:
            return {
                "success": False,
                "error": f"Login failed: {message}"
            }

        # Get contract
        contract = client._get_contract(symbol)
        if not contract:
            return {
                "success": False,
                "error": f"Contract not found for {symbol}"
            }

        # Fetch quote
        try:
            quote = client.api.snapshots([contract])[0]

            # Extract price (use close, or latest traded price)
            price = None
            if hasattr(quote, 'close') and quote.close:
                price = float(quote.close)
            elif hasattr(quote, 'last_price') and quote.last_price:
                price = float(quote.last_price)

            if price is None:
                return {
                    "success": False,
                    "error": f"No price data available for {symbol}"
                }

            return {
                "success": True,
                "symbol": symbol,
                "price": price,
                "open": float(quote.open) if hasattr(quote, 'open') and quote.open else None,
                "high": float(quote.high) if hasattr(quote, 'high') and quote.high else None,
                "low": float(quote.low) if hasattr(quote, 'low') and quote.low else None,
                "volume": int(quote.volume) if hasattr(quote, 'volume') and quote.volume else None
            }

        except Exception as e:
            return {
                "success": False,
                "error": f"Failed to fetch quote: {str(e)}"
            }
        finally:
            # Logout
            try:
                client.api.logout()
            except:
                pass

    except Exception as e:
        return {
            "success": False,
            "error": str(e)
        }

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({
            "success": False,
            "error": "Usage: python fetch_quote.py SYMBOL"
        }))
        sys.exit(1)

    symbol = sys.argv[1].upper()
    result = fetch_quote(symbol)
    print(json.dumps(result))
