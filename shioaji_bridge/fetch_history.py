#!/usr/bin/env python3
"""
Fetch historical price data for a US stock symbol via Shioaji
Usage: python fetch_history.py SYMBOL START_DATE END_DATE
Returns: JSON with historical price data
"""
import sys
import json
import logging
from datetime import datetime
from config import Config
from shioaji_client import ShioajiClient

logging.basicConfig(level=logging.ERROR)
logger = logging.getLogger(__name__)

def fetch_history(symbol: str, start_date: str, end_date: str) -> dict:
    """
    Fetch historical price data for a symbol

    Args:
        symbol: Stock symbol (e.g., 'AAPL')
        start_date: Start date in YYYY-MM-DD format
        end_date: End date in YYYY-MM-DD format

    Returns:
        dict: Historical price data
    """
    try:
        config = Config()
        client = ShioajiClient(config)

        # Login
        success, message = client.login()
        if not success:
            return {
                "success": False,
                "error": f"Login failed: {message}",
                "prices": []
            }

        # Get contract
        contract = client._get_contract(symbol)
        if not contract:
            return {
                "success": False,
                "error": f"Contract not found for {symbol}",
                "prices": []
            }

        # Fetch historical data (kbars)
        try:
            # Convert dates to datetime objects
            start_dt = datetime.strptime(start_date, "%Y-%m-%d")
            end_dt = datetime.strptime(end_date, "%Y-%m-%d")

            # Fetch kbars (daily candles)
            kbars = client.api.kbars(
                contract=contract,
                start=start_dt.strftime("%Y-%m-%d"),
                end=end_dt.strftime("%Y-%m-%d")
            )

            if not kbars or len(kbars) == 0:
                return {
                    "success": True,
                    "symbol": symbol,
                    "prices": [],
                    "message": "No historical data available"
                }

            # Convert kbars to price history format
            prices = []
            for bar in kbars:
                try:
                    price_entry = {
                        "date": bar['ts'].strftime("%Y-%m-%d") if 'ts' in bar else bar['Date'].strftime("%Y-%m-%d"),
                        "open": float(bar.get('Open', bar.get('open', 0))),
                        "high": float(bar.get('High', bar.get('high', 0))),
                        "low": float(bar.get('Low', bar.get('low', 0))),
                        "close": float(bar.get('Close', bar.get('close', 0))),
                        "volume": int(bar.get('Volume', bar.get('volume', 0))),
                        "adjusted_close": float(bar.get('Close', bar.get('close', 0)))  # Shioaji doesn't provide adjusted close separately
                    }
                    prices.append(price_entry)
                except Exception as bar_error:
                    logger.error(f"Error parsing bar: {bar_error}")
                    continue

            return {
                "success": True,
                "symbol": symbol,
                "prices": prices,
                "count": len(prices)
            }

        except Exception as e:
            return {
                "success": False,
                "error": f"Failed to fetch historical data: {str(e)}",
                "prices": []
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
            "error": str(e),
            "prices": []
        }

if __name__ == "__main__":
    if len(sys.argv) < 4:
        print(json.dumps({
            "success": False,
            "error": "Usage: python fetch_history.py SYMBOL START_DATE END_DATE",
            "prices": []
        }))
        sys.exit(1)

    symbol = sys.argv[1].upper()
    start_date = sys.argv[2]  # YYYY-MM-DD
    end_date = sys.argv[3]    # YYYY-MM-DD

    result = fetch_history(symbol, start_date, end_date)
    print(json.dumps(result))
