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


class QuotaResponse(BaseModel):
    success: bool
    usedBytes: int = 0
    limitBytes: int = 500 * 1024 * 1024  # 500MB default
    remainingBytes: int = 500 * 1024 * 1024
    usedMB: float = 0
    limitMB: float = 500
    remainingMB: float = 500
    percentageUsed: float = 0
    fallbackActive: bool = False
    error: Optional[str] = None


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


@app.get("/quota", response_model=QuotaResponse)
async def get_quota():
    """
    Get Shioaji API quota usage
    Returns current usage, limits, and whether fallback mode is active
    """
    try:
        client = get_client()
        
        # Try to get usage from Shioaji API
        try:
            usage = client.api.usage()
            
            # Extract bytes - Shioaji returns bytes used/remaining
            used_bytes = 0
            limit_bytes = 500 * 1024 * 1024  # 500MB default
            remaining_bytes = limit_bytes
            
            if hasattr(usage, 'bytes'):
                used_bytes = usage.bytes
            elif hasattr(usage, 'used_bytes'):
                used_bytes = usage.used_bytes
            elif isinstance(usage, dict):
                used_bytes = usage.get('bytes', 0) or usage.get('used_bytes', 0)
            
            if hasattr(usage, 'remaining_bytes'):
                remaining_bytes = usage.remaining_bytes
            elif isinstance(usage, dict):
                remaining_bytes = usage.get('remaining_bytes', limit_bytes - used_bytes)
            else:
                remaining_bytes = limit_bytes - used_bytes
            
            if hasattr(usage, 'limit_bytes'):
                limit_bytes = usage.limit_bytes
            elif isinstance(usage, dict):
                limit_bytes = usage.get('limit_bytes', 500 * 1024 * 1024)
            
            used_mb = used_bytes / (1024 * 1024)
            limit_mb = limit_bytes / (1024 * 1024)
            remaining_mb = remaining_bytes / (1024 * 1024)
            percentage_used = (used_bytes / limit_bytes * 100) if limit_bytes > 0 else 0
            
            # Fallback active if remaining < 50MB
            fallback_active = remaining_bytes < 50 * 1024 * 1024
            
            return QuotaResponse(
                success=True,
                usedBytes=used_bytes,
                limitBytes=limit_bytes,
                remainingBytes=remaining_bytes,
                usedMB=round(used_mb, 2),
                limitMB=round(limit_mb, 2),
                remainingMB=round(remaining_mb, 2),
                percentageUsed=round(percentage_used, 2),
                fallbackActive=fallback_active
            )
            
        except Exception as usage_error:
            logger.warning(f"Could not get Shioaji usage: {usage_error}")
            # Return default values - assume quota is available
            return QuotaResponse(
                success=True,
                usedBytes=0,
                limitBytes=500 * 1024 * 1024,
                remainingBytes=500 * 1024 * 1024,
                usedMB=0,
                limitMB=500,
                remainingMB=500,
                percentageUsed=0,
                fallbackActive=False,
                error=f"Could not fetch usage: {str(usage_error)}"
            )
            
    except Exception as e:
        logger.error(f"Error getting quota: {e}")
        return QuotaResponse(
            success=False,
            fallbackActive=True,
            error=str(e)
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


class FundamentalsResponse(BaseModel):
    success: bool
    symbol: Optional[str] = None
    name: Optional[str] = None
    sector: Optional[str] = None
    dividendYield: Optional[float] = None
    peRatio: Optional[float] = None
    pbRatio: Optional[float] = None
    roe: Optional[float] = None
    eps: Optional[float] = None
    marketCap: Optional[float] = None
    currentPrice: Optional[float] = None
    error: Optional[str] = None


@app.get("/fundamentals/{symbol}", response_model=FundamentalsResponse)
async def get_fundamentals(symbol: str):
    """
    Get fundamental data for a Taiwan stock symbol.
    Fetches dividend yield, P/E, P/B, ROE, EPS, market cap from Yahoo Finance.
    """
    try:
        # Convert to Yahoo Finance format
        yahoo_symbol = symbol.upper()
        if not ('.TW' in yahoo_symbol or '.TWO' in yahoo_symbol):
            yahoo_symbol = f"{yahoo_symbol}.TW"
        
        logger.info(f"Fetching fundamentals for {yahoo_symbol}")
        
        ticker = yf.Ticker(yahoo_symbol)
        info = ticker.info
        
        if not info or 'symbol' not in info:
            # Try OTC market
            yahoo_symbol = yahoo_symbol.replace('.TW', '.TWO')
            ticker = yf.Ticker(yahoo_symbol)
            info = ticker.info
        
        if not info:
            return FundamentalsResponse(success=False, error=f"No data found for {symbol}")
        
        # Extract fundamentals
        dividend_yield = None
        if info.get('dividendYield'):
            dividend_yield = float(info['dividendYield']) * 100  # Convert to percentage
        elif info.get('trailingAnnualDividendYield'):
            dividend_yield = float(info['trailingAnnualDividendYield']) * 100
        
        pe_ratio = info.get('trailingPE') or info.get('forwardPE')
        pb_ratio = info.get('priceToBook')
        
        # ROE calculation from profitMargins and other metrics if available
        roe = info.get('returnOnEquity')
        if roe:
            roe = float(roe) * 100  # Convert to percentage
        
        eps = info.get('trailingEps')
        market_cap = info.get('marketCap')
        current_price = info.get('regularMarketPrice') or info.get('currentPrice')
        
        name = info.get('shortName') or info.get('longName') or symbol
        sector = info.get('sector') or determine_sector_from_code(symbol)
        
        return FundamentalsResponse(
            success=True,
            symbol=symbol.upper(),
            name=name,
            sector=sector,
            dividendYield=round(dividend_yield, 2) if dividend_yield else None,
            peRatio=round(float(pe_ratio), 2) if pe_ratio else None,
            pbRatio=round(float(pb_ratio), 2) if pb_ratio else None,
            roe=round(roe, 2) if roe else None,
            eps=round(float(eps), 2) if eps else None,
            marketCap=float(market_cap) if market_cap else None,
            currentPrice=round(float(current_price), 2) if current_price else None
        )
        
    except Exception as e:
        logger.error(f"Error fetching fundamentals for {symbol}: {e}")
        return FundamentalsResponse(success=False, error=str(e))


def determine_sector_from_code(symbol: str) -> str:
    """Determine sector based on Taiwan stock code pattern."""
    code = symbol.replace(".TW", "").replace(".TWO", "")
    try:
        code_num = int(code)
        if 1100 <= code_num < 1300: return "Materials"
        if 1200 <= code_num < 1400: return "Consumer Staples"
        if 1400 <= code_num < 1600: return "Materials"
        if 1700 <= code_num < 1800: return "Materials"
        if 2000 <= code_num < 2100: return "Materials"
        if 2100 <= code_num < 2200: return "Consumer Discretionary"
        if 2200 <= code_num < 2400: return "Consumer Discretionary"
        if 2300 <= code_num < 2500: return "Technology"
        if 2400 <= code_num < 2500: return "Telecom"
        if 2500 <= code_num < 2700: return "Real Estate"
        if 2600 <= code_num < 2700: return "Industrials"
        if 2800 <= code_num < 3000: return "Financials"
        if 3000 <= code_num < 4000: return "Technology"
        if 4000 <= code_num < 5000: return "Telecom"
        if 5800 <= code_num < 6000: return "Financials"
        if 6000 <= code_num < 7000: return "Technology"
        if 9900 <= code_num < 10000: return "Consumer Discretionary"
    except ValueError:
        pass
    return "Other"


if __name__ == "__main__":
    import uvicorn

    # Run the API server
    uvicorn.run(
        app,
        host="127.0.0.1",
        port=8888,
        log_level="info"
    )
