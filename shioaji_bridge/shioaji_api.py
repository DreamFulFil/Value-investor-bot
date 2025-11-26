#!/usr/bin/env python3
"""
FastAPI server for Taiwan stock data fetching via Shioaji
Provides REST endpoints for quote and historical data retrieval
Falls back to Yahoo Finance when Shioaji quota is exceeded
"""
import logging
from datetime import datetime
from typing import Optional
from fastapi import FastAPI, HTTPException, Query
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from config import Config
from shioaji_client import ShioajiClient
import yfinance as yf

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(
    title="Taiwan Stock Data API",
    description="REST API for fetching Taiwan stock data via Shioaji with Yahoo Finance fallback",
    version="1.0.0"
)

# Global client instance (reused across requests)
shioaji_client: Optional[ShioajiClient] = None
config: Optional[Config] = None


# Response models
class QuoteResponse(BaseModel):
    success: bool
    symbol: Optional[str] = None
    price: Optional[float] = None
    open: Optional[float] = None
    high: Optional[float] = None
    low: Optional[float] = None
    volume: Optional[int] = None
    error: Optional[str] = None


class PriceBar(BaseModel):
    date: str
    open: float
    high: float
    low: float
    close: float
    volume: int
    adjusted_close: float


class HistoryResponse(BaseModel):
    success: bool
    symbol: Optional[str] = None
    prices: list[PriceBar] = []
    count: Optional[int] = None
    error: Optional[str] = None


class HealthResponse(BaseModel):
    status: str
    connected: bool
    message: str


def get_client() -> ShioajiClient:
    """Get or create Shioaji client instance"""
    global shioaji_client, config

    if shioaji_client is None:
        config = Config()
        shioaji_client = ShioajiClient(config)

        # Login on first use
        success, message = shioaji_client.login()
        if not success:
            logger.error(f"Failed to login to Shioaji: {message}")
            raise HTTPException(status_code=500, detail=f"Shioaji login failed: {message}")

        logger.info("Shioaji client initialized and logged in")

    return shioaji_client


@app.on_event("startup")
async def startup_event():
    """Initialize Shioaji client on startup"""
    try:
        logger.info("Starting Shioaji API server...")
        get_client()
        logger.info("Shioaji API server ready")
    except Exception as e:
        logger.error(f"Failed to initialize Shioaji client: {e}")
        # Don't fail startup, allow lazy initialization


@app.on_event("shutdown")
async def shutdown_event():
    """Cleanup on shutdown"""
    global shioaji_client
    if shioaji_client and shioaji_client.api:
        try:
            shioaji_client.api.logout()
            logger.info("Logged out from Shioaji")
        except:
            pass


@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "service": "Shioaji Data API",
        "version": "1.0.0",
        "status": "running"
    }


@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint"""
    try:
        client = get_client()
        return HealthResponse(
            status="healthy",
            connected=client.is_logged_in,
            message="Shioaji client is connected" if client.is_logged_in else "Not connected"
        )
    except Exception as e:
        return HealthResponse(
            status="unhealthy",
            connected=False,
            message=str(e)
        )


@app.get("/quote/{symbol}", response_model=QuoteResponse)
async def get_quote(symbol: str):
    """
    Get current quote for a Taiwan stock symbol

    Args:
        symbol: Taiwan stock symbol (e.g., 2330, 2330.TW)

    Returns:
        QuoteResponse with current price data
    """
    try:
        client = get_client()
        symbol = symbol.upper()

        logger.info(f"Fetching quote for {symbol}")

        # Get contract
        contract = client._get_contract(symbol)
        if not contract:
            # Fallback to Yahoo Finance
            logger.info(f"Contract not found, trying Yahoo Finance for {symbol}")
            return await get_quote_yahoo(symbol)

        # Fetch quote from Shioaji
        snapshots = client.api.snapshots([contract])
        if not snapshots:
            return await get_quote_yahoo(symbol)
            
        quote = snapshots[0]

        # Extract price
        price = None
        if hasattr(quote, 'close') and quote.close:
            price = float(quote.close)
        elif hasattr(quote, 'last_price') and quote.last_price:
            price = float(quote.last_price)

        if price is None:
            # Fallback to Yahoo Finance
            return await get_quote_yahoo(symbol)

        return QuoteResponse(
            success=True,
            symbol=symbol,
            price=price,
            open=float(quote.open) if hasattr(quote, 'open') and quote.open else None,
            high=float(quote.high) if hasattr(quote, 'high') and quote.high else None,
            low=float(quote.low) if hasattr(quote, 'low') and quote.low else None,
            volume=int(quote.volume) if hasattr(quote, 'volume') and quote.volume else None
        )

    except Exception as e:
        logger.error(f"Error fetching quote for {symbol}: {e}")
        # Fallback to Yahoo Finance
        return await get_quote_yahoo(symbol)


async def get_quote_yahoo(symbol: str) -> QuoteResponse:
    """Fallback to Yahoo Finance for quote data"""
    try:
        # Convert to Yahoo Finance format (add .TW if not present)
        yahoo_symbol = symbol if '.TW' in symbol or '.TWO' in symbol else f"{symbol}.TW"
        logger.info(f"Fetching quote from Yahoo Finance for {yahoo_symbol}")
        
        ticker = yf.Ticker(yahoo_symbol)
        info = ticker.info
        
        price = info.get('regularMarketPrice') or info.get('currentPrice')
        if price is None:
            return QuoteResponse(success=False, error=f"No price data from Yahoo Finance for {symbol}")
        
        return QuoteResponse(
            success=True,
            symbol=symbol,
            price=float(price),
            open=float(info.get('regularMarketOpen')) if info.get('regularMarketOpen') else None,
            high=float(info.get('regularMarketDayHigh')) if info.get('regularMarketDayHigh') else None,
            low=float(info.get('regularMarketDayLow')) if info.get('regularMarketDayLow') else None,
            volume=int(info.get('regularMarketVolume')) if info.get('regularMarketVolume') else None
        )
    except Exception as e:
        logger.error(f"Yahoo Finance quote failed for {symbol}: {e}")
        return QuoteResponse(success=False, error=f"Failed to fetch quote: {str(e)}")


@app.get("/history/{symbol}", response_model=HistoryResponse)
async def get_history(
    symbol: str,
    start_date: str = Query(..., description="Start date in YYYY-MM-DD format"),
    end_date: str = Query(..., description="End date in YYYY-MM-DD format")
):
    """
    Get historical price data for a Taiwan stock symbol

    Args:
        symbol: Taiwan stock symbol (e.g., 2330, 2330.TW)
        start_date: Start date in YYYY-MM-DD format
        end_date: End date in YYYY-MM-DD format

    Returns:
        HistoryResponse with historical price data
    """
    try:
        client = get_client()
        symbol = symbol.upper()

        logger.info(f"Fetching history for {symbol} from {start_date} to {end_date}")

        # Check API usage first
        try:
            usage = client.api.usage()
            if hasattr(usage, 'remaining_bytes') and usage.remaining_bytes <= 0:
                logger.warning(f"Shioaji API quota exceeded, falling back to Yahoo Finance")
                return await get_history_yahoo(symbol, start_date, end_date)
        except:
            pass

        # Get contract
        contract = client._get_contract(symbol)
        if not contract:
            logger.info(f"Contract not found, trying Yahoo Finance for {symbol}")
            return await get_history_yahoo(symbol, start_date, end_date)

        # Validate dates
        start_dt = datetime.strptime(start_date, "%Y-%m-%d")
        end_dt = datetime.strptime(end_date, "%Y-%m-%d")

        # Fetch kbars (daily candles)
        kbars = client.api.kbars(
            contract=contract,
            start=start_date,
            end=end_date
        )

        # Convert kbars to price bars using correct Shioaji pattern
        prices = []
        
        try:
            # Unpack kbars to dict (correct Shioaji pattern)
            kbars_dict = {**kbars}
            
            # Check if we have data - if empty, fallback to Yahoo Finance
            if not kbars_dict.get('ts') or len(kbars_dict['ts']) == 0:
                logger.info(f"No kbars data from Shioaji for {symbol}, falling back to Yahoo Finance")
                return await get_history_yahoo(symbol, start_date, end_date)
            
            num_bars = len(kbars_dict['ts'])
            logger.info(f"Processing {num_bars} kbars for {symbol}")
            
            # Convert columnar data to row-based price bars
            for i in range(num_bars):
                try:
                    # Convert nanosecond timestamp to datetime
                    timestamp_ns = kbars_dict['ts'][i]
                    timestamp_sec = timestamp_ns / 1_000_000_000
                    date_val = datetime.fromtimestamp(timestamp_sec)
                    date_str = date_val.strftime("%Y-%m-%d")

                    price_bar = PriceBar(
                        date=date_str,
                        open=float(kbars_dict['Open'][i]),
                        high=float(kbars_dict['High'][i]),
                        low=float(kbars_dict['Low'][i]),
                        close=float(kbars_dict['Close'][i]),
                        volume=int(kbars_dict['Volume'][i]),
                        adjusted_close=float(kbars_dict['Close'][i])
                    )
                    prices.append(price_bar)
                except Exception as bar_error:
                    logger.error(f"Error parsing bar at index {i}: {bar_error}")
                    continue
                    
        except Exception as unpack_error:
            logger.error(f"Error unpacking kbars: {unpack_error}")
            return await get_history_yahoo(symbol, start_date, end_date)

        if len(prices) == 0:
            # Fallback to Yahoo Finance if no data
            return await get_history_yahoo(symbol, start_date, end_date)

        logger.info(f"Successfully fetched {len(prices)} price bars for {symbol}")

        return HistoryResponse(
            success=True,
            symbol=symbol,
            prices=prices,
            count=len(prices)
        )

    except ValueError as e:
        logger.error(f"Invalid date format: {e}")
        return HistoryResponse(
            success=False,
            error=f"Invalid date format. Use YYYY-MM-DD"
        )
    except Exception as e:
        logger.error(f"Error fetching history for {symbol}: {e}")
        # Fallback to Yahoo Finance
        return await get_history_yahoo(symbol, start_date, end_date)


async def get_history_yahoo(symbol: str, start_date: str, end_date: str) -> HistoryResponse:
    """Fallback to Yahoo Finance for historical data"""
    try:
        # Convert to Yahoo Finance format (add .TW if not present)
        yahoo_symbol = symbol if '.TW' in symbol or '.TWO' in symbol else f"{symbol}.TW"
        logger.info(f"Fetching history from Yahoo Finance for {yahoo_symbol}")
        
        ticker = yf.Ticker(yahoo_symbol)
        
        # Add one day to end_date since yfinance end is exclusive
        end_dt = datetime.strptime(end_date, "%Y-%m-%d")
        from datetime import timedelta
        end_dt_inclusive = end_dt + timedelta(days=1)
        
        df = ticker.history(start=start_date, end=end_dt_inclusive.strftime("%Y-%m-%d"))
        
        if df.empty:
            # Try OTC market (.TWO)
            if '.TW' in yahoo_symbol:
                yahoo_symbol = yahoo_symbol.replace('.TW', '.TWO')
                logger.info(f"Trying OTC market: {yahoo_symbol}")
                ticker = yf.Ticker(yahoo_symbol)
                df = ticker.history(start=start_date, end=end_dt_inclusive.strftime("%Y-%m-%d"))
        
        if df.empty:
            return HistoryResponse(
                success=True,
                symbol=symbol,
                prices=[],
                count=0
            )
        
        prices = []
        for date_idx, row in df.iterrows():
            price_bar = PriceBar(
                date=date_idx.strftime("%Y-%m-%d"),
                open=float(row['Open']),
                high=float(row['High']),
                low=float(row['Low']),
                close=float(row['Close']),
                volume=int(row['Volume']),
                adjusted_close=float(row['Close'])
            )
            prices.append(price_bar)
        
        logger.info(f"Yahoo Finance returned {len(prices)} price bars for {symbol}")
        
        return HistoryResponse(
            success=True,
            symbol=symbol,
            prices=prices,
            count=len(prices)
        )
    except Exception as e:
        logger.error(f"Yahoo Finance history failed for {symbol}: {e}")
        return HistoryResponse(
            success=False,
            error=f"Failed to fetch history: {str(e)}"
        )


if __name__ == "__main__":
    import uvicorn

    # Run the API server
    uvicorn.run(
        app,
        host="127.0.0.1",
        port=8888,
        log_level="info"
    )
