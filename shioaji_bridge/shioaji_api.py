#!/usr/bin/env python3
"""
FastAPI server for Shioaji data fetching
Provides REST endpoints for quote and historical data retrieval
"""
import logging
from datetime import datetime
from typing import Optional
from fastapi import FastAPI, HTTPException, Query
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from config import Config
from shioaji_client import ShioajiClient

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(
    title="Shioaji Data API",
    description="REST API for fetching US stock data via Shioaji",
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
    Get current quote for a stock symbol

    Args:
        symbol: Stock symbol (e.g., AAPL, GOOGL)

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
            return QuoteResponse(
                success=False,
                error=f"Contract not found for {symbol}"
            )

        # Fetch quote
        quote = client.api.snapshots([contract])[0]

        # Extract price
        price = None
        if hasattr(quote, 'close') and quote.close:
            price = float(quote.close)
        elif hasattr(quote, 'last_price') and quote.last_price:
            price = float(quote.last_price)

        if price is None:
            return QuoteResponse(
                success=False,
                error=f"No price data available for {symbol}"
            )

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
        return QuoteResponse(
            success=False,
            error=str(e)
        )


@app.get("/history/{symbol}", response_model=HistoryResponse)
async def get_history(
    symbol: str,
    start_date: str = Query(..., description="Start date in YYYY-MM-DD format"),
    end_date: str = Query(..., description="End date in YYYY-MM-DD format")
):
    """
    Get historical price data for a stock symbol

    Args:
        symbol: Stock symbol (e.g., AAPL, GOOGL)
        start_date: Start date in YYYY-MM-DD format
        end_date: End date in YYYY-MM-DD format

    Returns:
        HistoryResponse with historical price data
    """
    try:
        client = get_client()
        symbol = symbol.upper()

        logger.info(f"Fetching history for {symbol} from {start_date} to {end_date}")

        # Get contract
        contract = client._get_contract(symbol)
        if not contract:
            return HistoryResponse(
                success=False,
                error=f"Contract not found for {symbol}"
            )

        # Validate dates
        start_dt = datetime.strptime(start_date, "%Y-%m-%d")
        end_dt = datetime.strptime(end_date, "%Y-%m-%d")

        # Fetch kbars (daily candles)
        kbars = client.api.kbars(
            contract=contract,
            start=start_dt.strftime("%Y-%m-%d"),
            end=end_dt.strftime("%Y-%m-%d")
        )

        # Debug logging
        logger.info(f"kbars object type: {type(kbars)}")
        logger.info(f"kbars object: {kbars}")
        if hasattr(kbars, '__dict__'):
            logger.info(f"kbars __dict__: {kbars.__dict__}")

        # Convert kbars to price bars using correct Shioaji pattern
        # Shioaji Kbars is a namedtuple-like object, unpack with {**kbars}
        prices = []
        
        try:
            # Unpack kbars to dict (correct Shioaji pattern)
            kbars_dict = {**kbars}
            
            # Check if we have data
            if not kbars_dict.get('ts') or len(kbars_dict['ts']) == 0:
                logger.info(f"No kbars data for {symbol}")
                return HistoryResponse(
                    success=True,
                    symbol=symbol,
                    prices=[],
                    count=0
                )
            
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
            return HistoryResponse(
                success=True,
                symbol=symbol,
                prices=[],
                count=0
            )

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
        return HistoryResponse(
            success=False,
            error=str(e)
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
