import sys
import json
import yfinance as yf

def get_sector(ticker_symbol):
    """
    Fetches the sector for a given stock ticker using yfinance.
    Prints a JSON object with the sector or an error.
    """
    try:
        # For Taiwan stocks, yfinance expects a .TW suffix for TSE listed
        # and .TWO for OTC listed. We'll try .TW first as a default.
        if len(ticker_symbol) == 4 and not ticker_symbol.endswith(('.TW', '.TWO')):
             ticker_symbol += '.TW'
        
        ticker = yf.Ticker(ticker_symbol)
        info = ticker.info
        
        # yfinance can return different keys for sector, e.g., 'sector' or 'sectorDisp'
        sector = info.get('sector')
        if not sector:
            sector = info.get('sectorDisp')

        if sector:
            print(json.dumps({'ticker': ticker_symbol, 'sector': sector}))
        else:
            # If .TW fails, try .TWO for OTC stocks
            if ticker_symbol.endswith('.TW'):
                return get_sector(ticker_symbol.replace('.TW', '.TWO'))
            print(json.dumps({'ticker': ticker_symbol, 'error': 'Sector information not available.'}))

    except Exception as e:
        print(json.dumps({'ticker': ticker_symbol, 'error': f"An error occurred: {str(e)}"}))

def main():
    if len(sys.argv) > 1:
        get_sector(sys.argv[1])
    else:
        print(json.dumps({'error': 'No ticker symbol provided.'}))

if __name__ == "__main__":
    main()
