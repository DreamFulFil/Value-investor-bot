#!/usr/bin/env python3
"""
Fetch historical price data for a Taiwan stock symbol via Shioaji
Falls back to Yahoo Finance when Shioaji data unavailable
Usage: python fetch_history.py SYMBOL START_DATE END_DATE
Returns: JSON with historical price data
"""
import sys
import json
import logging
from datetime import datetime, timedelta
from config import Config
from shioaji_client import ShioajiClient
import yfinance as yf

logging.basicConfig(level=logging.ERROR)
logger = logging.getLogger(__name__)

def fetch_history(symbol: str, start_date: str, end_date: str) -> dict:
    """
    Fetch historical price data for a Taiwan stock symbol

    Args:
        symbol: Taiwan stock symbol (e.g., '2330', '2330.TW')
        start_date: Start date in YYYY-MM-DD format
        end_date: End date in YYYY-MM-DD format

    Returns:
        dict: Historical price data
    """
    # Try Shioaji first
    try:
        config = Config()
        client = ShioajiClient(config)

        success, message = client.login()
        if success:
            # Check API quota
            try:
                usage = client.api.usage()
                if hasattr(usage, 'remaining_bytes') and usage.remaining_bytes <= 0:
                    logger.info("Shioaji quota exceeded, using Yahoo Finance")
                    client.logout()
                    return fetch_history_yahoo(symbol, start_date, end_date)
            except:
                pass

            contract = client._get_contract(symbol)
            if contract:
                kbars = client.api.kbars(
                    contract=contract,
                    start=start_date,
                    end=end_date
                )
                
                # Unpack kbars correctly
                kbars_dict = {**kbars}
                if kbars_dict.get('ts') and len(kbars_dict['ts']) > 0:
                    prices = []
                    for i in range(len(kbars_dict['ts'])):
                        timestamp_ns = kbars_dict['ts'][i]
                        timestamp_sec = timestamp_ns / 1_000_000_000
                        date_val = datetime.fromtimestamp(timestamp_sec)
                        
                        prices.append({
                            "date": date_val.strftime("%Y-%m-%d"),
                            "open": float(kbars_dict['Open'][i]),
                            "high": float(kbars_dict['High'][i]),
                            "low": float(kbars_dict['Low'][i]),
                            "close": float(kbars_dict['Close'][i]),
                            "volume": int(kbars_dict['Volume'][i]),
                            "adjusted_close": float(kbars_dict['Close'][i])
                        })
                    
                    client.logout()
                    return {
                        "success": True,
                        "symbol": symbol,
                        "prices": prices,
                        "count": len(prices)
                    }
            
            client.logout()
    except Exception as e:
        logger.error(f"Shioaji error: {e}")

    # Fallback to Yahoo Finance
    return fetch_history_yahoo(symbol, start_date, end_date)


def fetch_history_yahoo(symbol: str, start_date: str, end_date: str) -> dict:
    """Fetch historical data from Yahoo Finance"""
    try:
        # Convert to Yahoo Finance format
        yahoo_symbol = symbol if '.TW' in symbol or '.TWO' in symbol else f"{symbol}.TW"
        
        ticker = yf.Ticker(yahoo_symbol)
        end_dt = datetime.strptime(end_date, "%Y-%m-%d") + timedelta(days=1)
        df = ticker.history(start=start_date, end=end_dt.strftime("%Y-%m-%d"))
        
        # Try OTC if TSE fails
        if df.empty and '.TW' in yahoo_symbol:
            yahoo_symbol = yahoo_symbol.replace('.TW', '.TWO')
            ticker = yf.Ticker(yahoo_symbol)
            df = ticker.history(start=start_date, end=end_dt.strftime("%Y-%m-%d"))
        
        if df.empty:
            return {"success": True, "symbol": symbol, "prices": [], "count": 0}
        
        prices = []
        for date_idx, row in df.iterrows():
            prices.append({
                "date": date_idx.strftime("%Y-%m-%d"),
                "open": float(row['Open']),
                "high": float(row['High']),
                "low": float(row['Low']),
                "close": float(row['Close']),
                "volume": int(row['Volume']),
                "adjusted_close": float(row['Close'])
            })
        
        return {
            "success": True,
            "symbol": symbol,
            "prices": prices,
            "count": len(prices)
        }
    except Exception as e:
        return {"success": False, "error": str(e), "prices": []}

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
