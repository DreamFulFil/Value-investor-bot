"""
Shioaji API client wrapper for Taiwan stock trading
Handles authentication, order placement, and position management
"""
import logging
import math
from typing import Optional, Dict, Any
from datetime import datetime
import shioaji as sj
from shioaji import constant
from config import Config

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('shioaji_bridge.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

class ShioajiClient:
    """Wrapper class for Shioaji API operations"""

    def __init__(self, config: Config):
        """
        Initialize Shioaji client with configuration

        Args:
            config: Config object with API credentials
        """
        self.config = config
        self.api: Optional[sj.Shioaji] = None
        self.is_logged_in = False

    def login(self) -> tuple[bool, str]:
        """
        Authenticate with Shioaji API

        Returns:
            tuple: (success, message)
        """
        try:
            logger.info("Attempting to login to Shioaji API...")

            # Initialize Shioaji API
            self.api = sj.Shioaji(simulation=self.config.simulation)

            # Login with credentials
            accounts = self.api.login(
                api_key=self.config.api_key,
                secret_key=self.config.secret_key,
                contracts_cb=lambda security_type: logger.info(f"Downloading {security_type} contracts...")
            )

            if not accounts:
                return False, "Login failed: No accounts returned"

            logger.info(f"Successfully logged in. Accounts: {len(accounts)}")
            self.is_logged_in = True

            # Activate CA for trading (required for placing orders)
            if self.config.ca_path and self.config.ca_passwd and self.config.person_id:
                try:
                    ca_result = self.api.activate_ca(
                        ca_path=self.config.ca_path,
                        ca_passwd=self.config.ca_passwd,
                        person_id=self.config.person_id
                    )
                    logger.info("CA certificate activated for trading")
                except Exception as ca_error:
                    logger.warning(f"CA activation failed: {ca_error}")
            else:
                logger.warning("CA certificate not configured, skipping activation")

            return True, "Login successful"

        except Exception as e:
            error_msg = f"Login error: {str(e)}"
            logger.error(error_msg)
            return False, error_msg

    def _get_contract(self, symbol: str) -> Optional[Any]:
        """
        Get contract object for given Taiwan stock symbol

        Args:
            symbol: Stock symbol (e.g., '2330', '2454')

        Returns:
            Contract object or None if not found
        """
        try:
            # For Taiwan stocks, we need to search TSE, OTC, and OES exchanges
            # Shioaji uses different contract structures for different exchanges

            # Try Taiwan exchanges in order: TSE (main), OTC, OES
            for exchange in ['TSE', 'OTC', 'OES']:
                try:
                    exchange_contracts = getattr(self.api.Contracts.Stocks, exchange)

                    # Search for the symbol in this exchange
                    for contract in exchange_contracts:
                        if contract.code == symbol:
                            logger.info(f"Found contract for {symbol} in {exchange}: {contract}")
                            return contract

                except AttributeError:
                    logger.debug(f"Exchange {exchange} not available")
                    continue
                except Exception as ex:
                    logger.debug(f"Error searching {exchange}: {ex}")
                    continue

            logger.error(f"Contract not found for symbol: {symbol} in any Taiwan exchange (TSE/OTC/OES)")
            return None

        except Exception as e:
            logger.error(f"Error getting contract for {symbol}: {e}")
            return None

    def place_order(
        self,
        action: str,
        symbol: str,
        quantity: float,
        price: float
    ) -> Dict[str, Any]:
        """
        Place an order for Taiwan stocks

        Args:
            action: 'BUY' or 'SELL'
            symbol: Stock symbol
            quantity: Number of shares (will be rounded to whole shares)
            price: Limit price per share

        Returns:
            dict: Order result with success status and details
        """
        if not self.is_logged_in or not self.api:
            return {
                "success": False,
                "error": "Not logged in",
                "message": "Must login before placing orders",
                "timestamp": datetime.utcnow().isoformat() + "Z"
            }

        try:
            logger.info(f"Placing order: {action} {quantity} shares of {symbol} at ${price}")

            # Get contract
            contract = self._get_contract(symbol)
            if not contract:
                return {
                    "success": False,
                    "error": "Contract not found",
                    "message": f"Unable to find contract for symbol: {symbol}",
                    "timestamp": datetime.utcnow().isoformat() + "Z"
                }

            # Round quantity to whole shares (US stocks typically don't support fractional shares via Shioaji)
            whole_quantity = int(math.floor(quantity))
            if whole_quantity <= 0:
                return {
                    "success": False,
                    "error": "Invalid quantity",
                    "message": f"Quantity {quantity} rounds to zero shares",
                    "timestamp": datetime.utcnow().isoformat() + "Z"
                }

            if whole_quantity != quantity:
                logger.warning(f"Fractional shares not supported. Rounded {quantity} to {whole_quantity}")

            # Determine order action
            if action.upper() == "BUY":
                order_action = constant.Action.Buy
            elif action.upper() == "SELL":
                order_action = constant.Action.Sell
            else:
                return {
                    "success": False,
                    "error": "Invalid action",
                    "message": f"Action must be BUY or SELL, got: {action}",
                    "timestamp": datetime.utcnow().isoformat() + "Z"
                }

            # Create order object
            order = self.api.Order(
                price=price,
                quantity=whole_quantity,
                action=order_action,
                price_type=constant.StockPriceType.LMT,  # Limit order
                order_type=constant.OrderType.ROD,  # Rest of Day
                account=self.api.stock_account  # Use stock trading account
            )

            # Place the order
            trade = self.api.place_order(contract, order)

            if not trade:
                return {
                    "success": False,
                    "error": "Order placement failed",
                    "message": "Shioaji API returned no trade object",
                    "timestamp": datetime.utcnow().isoformat() + "Z"
                }

            # Extract order details
            order_id = trade.status.id if hasattr(trade, 'status') else "UNKNOWN"
            status = trade.status.status if hasattr(trade, 'status') else "SUBMITTED"

            logger.info(f"Order placed successfully. Order ID: {order_id}, Status: {status}")

            return {
                "success": True,
                "order_id": order_id,
                "message": f"{action} order placed successfully",
                "status": status,
                "filled_quantity": str(whole_quantity),
                "filled_price": str(price),
                "timestamp": datetime.utcnow().isoformat() + "Z",
                "warning": f"Rounded to {whole_quantity} shares (fractional shares not supported)" if whole_quantity != quantity else None
            }

        except Exception as e:
            error_msg = f"Order placement error: {str(e)}"
            logger.error(error_msg)
            return {
                "success": False,
                "error": "Order execution failed",
                "message": error_msg,
                "timestamp": datetime.utcnow().isoformat() + "Z"
            }

    def get_positions(self) -> Dict[str, Any]:
        """
        Get current positions from account

        Returns:
            dict: Position information
        """
        if not self.is_logged_in or not self.api:
            return {
                "success": False,
                "error": "Not logged in",
                "positions": []
            }

        try:
            logger.info("Fetching current positions...")

            positions = self.api.list_positions()

            position_list = []
            for pos in positions:
                position_list.append({
                    "symbol": pos.code,
                    "quantity": pos.quantity,
                    "price": pos.price,
                    "pnl": pos.pnl if hasattr(pos, 'pnl') else 0
                })

            logger.info(f"Retrieved {len(position_list)} positions")

            return {
                "success": True,
                "positions": position_list,
                "count": len(position_list)
            }

        except Exception as e:
            error_msg = f"Error fetching positions: {str(e)}"
            logger.error(error_msg)
            return {
                "success": False,
                "error": error_msg,
                "positions": []
            }

    def logout(self) -> tuple[bool, str]:
        """
        Logout from Shioaji API

        Returns:
            tuple: (success, message)
        """
        try:
            if self.api and self.is_logged_in:
                logger.info("Logging out from Shioaji API...")
                self.api.logout()
                self.is_logged_in = False
                logger.info("Successfully logged out")
                return True, "Logout successful"
            else:
                return True, "Already logged out"

        except Exception as e:
            error_msg = f"Logout error: {str(e)}"
            logger.error(error_msg)
            return False, error_msg

    def __enter__(self):
        """Context manager entry"""
        success, message = self.login()
        if not success:
            raise Exception(f"Login failed: {message}")
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit"""
        self.logout()
        return False
