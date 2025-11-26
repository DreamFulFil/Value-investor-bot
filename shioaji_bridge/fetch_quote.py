#!/usr/bin/env python3
"""
Fetch current quote for a Taiwan stock symbol via Shioaji
Falls back to Yahoo Finance when Shioaji data unavailable
Usage: python fetch_quote.py SYMBOL
Returns: JSON with quote data
"""
import sys
import json
import logging
from config import Config
from shioaji_client import ShioajiClient
import yfinance as yf

logging.basicConfig(level=logging.ERROR)
logger = logging.getLogger(__name__)

def fetch_quote(symbol: str) -> dict:
    """
    Fetch current quote for a Taiwan stock symbol

    Args:
        symbol: Taiwan stock symbol (e.g., '2330', '2330.TW')

    Returns:
        dict: Quote data with price, symbol, etc.
    """
    # Try Shioaji first
    try:
        config = Config()
        client = ShioajiClient(config)

        success, message = client.login()
        if success:
            contract = client._get_contract(symbol)
            if contract:
                snapshots = client.api.snapshots([contract])
                if snapshots:
                    quote = snapshots[0]
                    price = None
                    if hasattr(quote, 'close') and quote.close:
                        price = float(quote.close)
                    elif hasattr(quote, 'last_price') and quote.last_price:
                        price = float(quote.last_price)
                    
                    if price:
                        client.logout()
                        return {
                            "success": True,
                            "symbol": symbol,
                            "price": price,
                            "open": float(quote.open) if hasattr(quote, 'open') and quote.open else None,
                            "high": float(quote.high) if hasattr(quote, 'high') and quote.high else None,
                            "low": float(quote.low) if hasattr(quote, 'low') and quote.low else None,
                            "volume": int(quote.volume) if hasattr(quote, 'volume') and quote.volume else None
                        }
            client.logout()
    except Exception as e:
        logger.error(f"Shioaji error: {e}")

    # Fallback to Yahoo Finance
    return fetch_quote_yahoo(symbol)


def fetch_quote_yahoo(symbol: str) -> dict:
    """Fetch quote from Yahoo Finance"""
    try:
        yahoo_symbol = symbol if '.TW' in symbol or '.TWO' in symbol else f"{symbol}.TW"
        ticker = yf.Ticker(yahoo_symbol)
        info = ticker.info
        
        price = info.get('regularMarketPrice') or info.get('currentPrice')
        if price is None:
            # Try OTC
            if '.TW' in yahoo_symbol:
                yahoo_symbol = yahoo_symbol.replace('.TW', '.TWO')
                ticker = yf.Ticker(yahoo_symbol)
                info = ticker.info
                price = info.get('regularMarketPrice') or info.get('currentPrice')
        
        if price is None:
            return {"success": False, "error": f"No price data for {symbol}"}
        
        return {
            "success": True,
            "symbol": symbol,
            "price": float(price),
            "open": float(info.get('regularMarketOpen')) if info.get('regularMarketOpen') else None,
            "high": float(info.get('regularMarketDayHigh')) if info.get('regularMarketDayHigh') else None,
            "low": float(info.get('regularMarketDayLow')) if info.get('regularMarketDayLow') else None,
            "volume": int(info.get('regularMarketVolume')) if info.get('regularMarketVolume') else None
        }
    except Exception as e:
        return {"success": False, "error": str(e)}

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
