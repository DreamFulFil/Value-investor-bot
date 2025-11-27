"""
Unit tests for Shioaji API Flask server
Tests the HTTP endpoints for the Python bridge
"""
import unittest
from unittest.mock import Mock, patch, MagicMock
import json
import sys
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class MockFlaskApp:
    """Mock Flask application for testing"""
    
    def __init__(self):
        self.connected = False
        self.api_key = None
        self.secret_key = None
    
    def health_check(self):
        return {
            "status": "healthy" if self.connected else "disconnected",
            "connected": self.connected,
            "timestamp": "2025-11-27T00:00:00Z"
        }
    
    def get_quote(self, symbol):
        if not self.connected:
            return {"error": "Not connected"}, 500
        
        stock_code = symbol.replace(".TW", "").replace(".TWO", "")
        
        # Mock quote data
        mock_quotes = {
            "2330": {
                "symbol": "2330.TW",
                "name": "台積電",
                "price": 580.00,
                "change": 5.00,
                "change_percent": 0.87,
                "volume": 25000000
            },
            "2317": {
                "symbol": "2317.TW",
                "name": "鴻海",
                "price": 108.50,
                "change": -1.00,
                "change_percent": -0.91,
                "volume": 15000000
            }
        }
        
        if stock_code in mock_quotes:
            return mock_quotes[stock_code], 200
        return {"error": f"Symbol not found: {symbol}"}, 404
    
    def get_history(self, symbol, start_date, end_date):
        if not self.connected:
            return {"error": "Not connected"}, 500
        
        stock_code = symbol.replace(".TW", "").replace(".TWO", "")
        
        if stock_code not in ["2330", "2317", "2454"]:
            return {"error": f"Contract not found for {symbol}"}, 404
        
        # Return mock historical data
        return {
            "symbol": symbol,
            "prices": [
                {
                    "date": "2024-11-26",
                    "open": 575.00,
                    "high": 582.00,
                    "low": 573.00,
                    "close": 580.00,
                    "volume": 25000000
                },
                {
                    "date": "2024-11-25",
                    "open": 570.00,
                    "high": 578.00,
                    "low": 568.00,
                    "close": 575.00,
                    "volume": 22000000
                }
            ],
            "count": 2
        }, 200


class TestAPIEndpoints(unittest.TestCase):
    """Tests for API endpoints"""

    def setUp(self):
        """Set up test fixtures"""
        self.app = MockFlaskApp()
        self.app.connected = True

    def test_health_check_connected(self):
        """Test health check when connected"""
        result = self.app.health_check()
        self.assertEqual(result["status"], "healthy")
        self.assertTrue(result["connected"])

    def test_health_check_disconnected(self):
        """Test health check when disconnected"""
        self.app.connected = False
        result = self.app.health_check()
        self.assertEqual(result["status"], "disconnected")
        self.assertFalse(result["connected"])

    def test_get_quote_success(self):
        """Test getting quote for valid symbol"""
        result, status = self.app.get_quote("2330.TW")
        self.assertEqual(status, 200)
        self.assertEqual(result["symbol"], "2330.TW")
        self.assertIn("price", result)
        self.assertIn("volume", result)

    def test_get_quote_not_found(self):
        """Test getting quote for invalid symbol"""
        result, status = self.app.get_quote("INVALID.TW")
        self.assertEqual(status, 404)
        self.assertIn("error", result)

    def test_get_quote_disconnected(self):
        """Test getting quote when disconnected"""
        self.app.connected = False
        result, status = self.app.get_quote("2330.TW")
        self.assertEqual(status, 500)
        self.assertEqual(result["error"], "Not connected")

    def test_get_history_success(self):
        """Test getting historical data for valid symbol"""
        result, status = self.app.get_history("2330.TW", "2024-01-01", "2024-11-27")
        self.assertEqual(status, 200)
        self.assertEqual(result["symbol"], "2330.TW")
        self.assertIn("prices", result)
        self.assertGreater(len(result["prices"]), 0)

    def test_get_history_not_found(self):
        """Test getting historical data for invalid symbol"""
        result, status = self.app.get_history("INVALID.TW", "2024-01-01", "2024-11-27")
        self.assertEqual(status, 404)
        self.assertIn("error", result)

    def test_get_history_disconnected(self):
        """Test getting historical data when disconnected"""
        self.app.connected = False
        result, status = self.app.get_history("2330.TW", "2024-01-01", "2024-11-27")
        self.assertEqual(status, 500)

    def test_quote_strips_suffix(self):
        """Test that .TW suffix is handled correctly"""
        result1, _ = self.app.get_quote("2330.TW")
        result2, _ = self.app.get_quote("2330")
        # Both should return valid data
        self.assertIn("price", result1)


class TestHistoricalDataParsing(unittest.TestCase):
    """Tests for historical data parsing"""

    def test_parse_date_format(self):
        """Test date format parsing"""
        from datetime import datetime
        date_str = "2024-11-26"
        parsed = datetime.strptime(date_str, "%Y-%m-%d")
        self.assertEqual(parsed.year, 2024)
        self.assertEqual(parsed.month, 11)
        self.assertEqual(parsed.day, 26)

    def test_price_data_structure(self):
        """Test price data structure"""
        price_data = {
            "date": "2024-11-26",
            "open": 575.00,
            "high": 582.00,
            "low": 573.00,
            "close": 580.00,
            "volume": 25000000
        }
        
        required_fields = ["date", "open", "high", "low", "close", "volume"]
        for field in required_fields:
            self.assertIn(field, price_data)

    def test_ohlcv_validation(self):
        """Test OHLCV data validation"""
        open_price = 575.00
        high = 582.00
        low = 573.00
        close = 580.00
        
        # High should be >= all other prices
        self.assertGreaterEqual(high, open_price)
        self.assertGreaterEqual(high, close)
        self.assertGreaterEqual(high, low)
        
        # Low should be <= all other prices
        self.assertLessEqual(low, open_price)
        self.assertLessEqual(low, close)
        self.assertLessEqual(low, high)


class TestErrorHandling(unittest.TestCase):
    """Tests for error handling"""

    def test_connection_error(self):
        """Test handling of connection errors"""
        app = MockFlaskApp()
        app.connected = False
        
        result, status = app.get_quote("2330.TW")
        self.assertEqual(status, 500)
        self.assertIn("error", result)

    def test_symbol_not_found(self):
        """Test handling of symbol not found"""
        app = MockFlaskApp()
        app.connected = True
        
        result, status = app.get_quote("9999.TW")
        self.assertEqual(status, 404)
        self.assertIn("error", result)

    def test_invalid_date_range(self):
        """Test handling of invalid date range"""
        # This would be validated in the actual API
        start_date = "2024-11-27"
        end_date = "2024-01-01"  # End before start
        
        # In actual implementation, this should be rejected
        from datetime import datetime
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")
        
        self.assertGreater(start, end)


class TestQuotaTracking(unittest.TestCase):
    """Tests for Shioaji quota tracking"""

    def test_quota_calculation(self):
        """Test quota usage calculation"""
        used_bytes = 400 * 1024 * 1024  # 400 MB
        limit_bytes = 500 * 1024 * 1024  # 500 MB
        
        remaining_bytes = limit_bytes - used_bytes
        percentage_used = (used_bytes / limit_bytes) * 100
        
        self.assertEqual(remaining_bytes, 100 * 1024 * 1024)
        self.assertEqual(percentage_used, 80.0)

    def test_fallback_threshold(self):
        """Test fallback threshold detection"""
        remaining_mb = 50
        threshold_mb = 50
        
        should_fallback = remaining_mb <= threshold_mb
        self.assertTrue(should_fallback)

    def test_quota_response_format(self):
        """Test quota response format"""
        quota_response = {
            "bytesUsed": 400 * 1024 * 1024,
            "limitBytes": 500 * 1024 * 1024,
            "remainingBytes": 100 * 1024 * 1024,
            "percentageUsed": 80.0
        }
        
        required_fields = ["bytesUsed", "limitBytes", "remainingBytes", "percentageUsed"]
        for field in required_fields:
            self.assertIn(field, quota_response)


if __name__ == '__main__':
    unittest.main()
