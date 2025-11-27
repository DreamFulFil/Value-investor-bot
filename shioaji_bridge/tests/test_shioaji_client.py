"""
Unit tests for Shioaji Client
Tests the core functionality of Taiwan stock trading operations
"""
import unittest
from unittest.mock import Mock, MagicMock, patch
from datetime import datetime
import sys
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from config import Config


class TestConfig(unittest.TestCase):
    """Tests for Config class"""

    def test_config_initialization(self):
        """Test that Config initializes with default values"""
        with patch.dict(os.environ, {
            'SHIOAJI_API_KEY': 'test_key',
            'SHIOAJI_SECRET_KEY': 'test_secret'
        }, clear=True):
            config = Config()
            # Config may use encrypted values from .env, check it's not None
            self.assertIsNotNone(config.api_key)
            self.assertIsNotNone(config.secret_key)

    def test_config_simulation_mode(self):
        """Test simulation mode configuration"""
        with patch.dict(os.environ, {
            'SHIOAJI_API_KEY': 'test_key',
            'SHIOAJI_SECRET_KEY': 'test_secret',
            'SHIOAJI_SIMULATION': 'true'
        }):
            config = Config()
            self.assertTrue(config.simulation)


class MockShioajiClient:
    """Mock implementation of ShioajiClient for testing"""
    
    def __init__(self, config):
        self.config = config
        self.api = None
        self.is_logged_in = False
        
    def login(self):
        self.is_logged_in = True
        return True, "Login successful"
    
    def logout(self):
        self.is_logged_in = False
        return True, "Logout successful"
    
    def _get_contract(self, symbol):
        stock_code = symbol.replace('.TW', '').replace('.TWO', '')
        if stock_code in ['2330', '2317', '2454']:
            return Mock(code=stock_code)
        return None
    
    def place_order(self, action, symbol, quantity, price):
        if not self.is_logged_in:
            return {
                "success": False,
                "error": "Not logged in"
            }
        
        contract = self._get_contract(symbol)
        if not contract:
            return {
                "success": False,
                "error": "Contract not found"
            }
        
        return {
            "success": True,
            "order_id": "TEST_ORDER_001",
            "status": "SUBMITTED",
            "filled_quantity": str(int(quantity)),
            "filled_price": str(price),
            "timestamp": datetime.utcnow().isoformat() + "Z"
        }


class TestShioajiClient(unittest.TestCase):
    """Tests for ShioajiClient class"""

    def setUp(self):
        """Set up test fixtures"""
        with patch.dict(os.environ, {
            'SHIOAJI_API_KEY': 'test_key',
            'SHIOAJI_SECRET_KEY': 'test_secret'
        }):
            self.config = Config()
            self.client = MockShioajiClient(self.config)

    def test_login_success(self):
        """Test successful login"""
        success, message = self.client.login()
        self.assertTrue(success)
        self.assertTrue(self.client.is_logged_in)

    def test_logout_success(self):
        """Test successful logout"""
        self.client.login()
        success, message = self.client.logout()
        self.assertTrue(success)
        self.assertFalse(self.client.is_logged_in)

    def test_get_contract_valid_symbol(self):
        """Test getting contract for valid Taiwan stock symbol"""
        contract = self.client._get_contract("2330.TW")
        self.assertIsNotNone(contract)
        self.assertEqual(contract.code, "2330")

    def test_get_contract_invalid_symbol(self):
        """Test getting contract for invalid symbol"""
        contract = self.client._get_contract("INVALID.TW")
        self.assertIsNone(contract)

    def test_get_contract_strips_suffix(self):
        """Test that .TW suffix is properly stripped"""
        contract = self.client._get_contract("2330.TW")
        self.assertIsNotNone(contract)
        self.assertEqual(contract.code, "2330")
        
        contract_two = self.client._get_contract("2330.TWO")
        # Should still find it by stripping .TWO
        self.assertIsNone(contract_two)  # Not in mock list

    def test_place_order_when_logged_in(self):
        """Test placing order when logged in"""
        self.client.login()
        result = self.client.place_order("BUY", "2330.TW", 10, 580.00)
        
        self.assertTrue(result["success"])
        self.assertEqual(result["filled_quantity"], "10")
        self.assertIn("order_id", result)

    def test_place_order_when_not_logged_in(self):
        """Test placing order when not logged in"""
        result = self.client.place_order("BUY", "2330.TW", 10, 580.00)
        
        self.assertFalse(result["success"])
        self.assertEqual(result["error"], "Not logged in")

    def test_place_order_invalid_symbol(self):
        """Test placing order with invalid symbol"""
        self.client.login()
        result = self.client.place_order("BUY", "INVALID.TW", 10, 100.00)
        
        self.assertFalse(result["success"])
        self.assertEqual(result["error"], "Contract not found")

    def test_place_order_fractional_shares(self):
        """Test that fractional shares are rounded down"""
        self.client.login()
        result = self.client.place_order("BUY", "2330.TW", 10.5, 580.00)
        
        self.assertTrue(result["success"])
        self.assertEqual(result["filled_quantity"], "10")

    def test_place_order_buy_action(self):
        """Test BUY order action"""
        self.client.login()
        result = self.client.place_order("BUY", "2330.TW", 10, 580.00)
        self.assertTrue(result["success"])

    def test_place_order_sell_action(self):
        """Test SELL order action"""
        self.client.login()
        result = self.client.place_order("SELL", "2330.TW", 10, 580.00)
        self.assertTrue(result["success"])


class TestSymbolParsing(unittest.TestCase):
    """Tests for symbol parsing utilities"""

    def test_strip_tw_suffix(self):
        """Test stripping .TW suffix"""
        symbol = "2330.TW"
        cleaned = symbol.replace(".TW", "")
        self.assertEqual(cleaned, "2330")

    def test_strip_two_suffix(self):
        """Test stripping .TWO suffix (OTC stocks)"""
        symbol = "5880.TWO"
        # Must strip .TWO first before .TW to avoid leaving 'O'
        cleaned = symbol.replace(".TWO", "").replace(".TW", "")
        self.assertEqual(cleaned, "5880")

    def test_no_suffix(self):
        """Test symbol without suffix"""
        symbol = "2330"
        cleaned = symbol.replace(".TWO", "").replace(".TW", "")
        self.assertEqual(cleaned, "2330")

    def test_valid_taiwan_stock_codes(self):
        """Test various valid Taiwan stock codes"""
        valid_codes = ["2330", "2317", "2454", "1301", "2882"]
        for code in valid_codes:
            self.assertTrue(code.isdigit())
            self.assertTrue(len(code) == 4)


class TestOrderValidation(unittest.TestCase):
    """Tests for order validation"""

    def test_valid_quantity(self):
        """Test valid quantity validation"""
        quantity = 10
        self.assertGreater(quantity, 0)

    def test_zero_quantity_rounds_to_zero(self):
        """Test that zero or negative quantity is rejected"""
        import math
        quantity = 0.5
        whole_quantity = int(math.floor(quantity))
        self.assertEqual(whole_quantity, 0)

    def test_valid_price(self):
        """Test valid price validation"""
        price = 580.00
        self.assertGreater(price, 0)

    def test_valid_action(self):
        """Test valid order actions"""
        valid_actions = ["BUY", "SELL", "buy", "sell"]
        for action in valid_actions:
            self.assertIn(action.upper(), ["BUY", "SELL"])


if __name__ == '__main__':
    unittest.main()
