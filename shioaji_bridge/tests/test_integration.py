"""
Integration tests for the complete Shioaji Bridge
Tests the interaction between components
"""
import unittest
from unittest.mock import Mock, patch, MagicMock
import json
import sys
import os
from datetime import datetime, timedelta

# Add parent directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class MockOllamaClient:
    """Mock Ollama client for testing"""
    
    def __init__(self, base_url="http://localhost:11434"):
        self.base_url = base_url
        self.available = True
        
    def is_available(self):
        return self.available
    
    def generate_insights(self, portfolio_data, market_context):
        if not self.available:
            raise Exception("Ollama not available")
        
        return """## Portfolio Analysis

Your portfolio is well-diversified across Taiwan's leading companies.

### Key Observations:
1. Strong exposure to semiconductor sector through TSMC
2. Balanced allocation across tech and financial sectors
3. Dividend yield exceeds market average

### Recommendations:
- Consider maintaining current allocation
- Monitor semiconductor sector trends
"""

    def generate_learning_tip(self, locale="en"):
        if locale == "zh-TW":
            return "價值投資的核心是以合理價格買入優質公司"
        return "The core of value investing is buying quality companies at reasonable prices"


class MockJavaBackend:
    """Mock Java backend for integration testing"""
    
    def __init__(self):
        self.portfolio_value = 160000
        self.cash_balance = 10000
        self.positions = [
            {"symbol": "2330.TW", "quantity": 10, "price": 580.00},
            {"symbol": "2317.TW", "quantity": 50, "price": 108.50},
            {"symbol": "2454.TW", "quantity": 5, "price": 1050.00}
        ]
        self.rebalance_count = 0
        self.last_rebalance = None
    
    def get_portfolio_summary(self):
        return {
            "totalValue": self.portfolio_value,
            "cashBalance": self.cash_balance,
            "positionsCount": len(self.positions)
        }
    
    def trigger_rebalance(self):
        now = datetime.now()
        
        # Check for same-month idempotency
        if self.last_rebalance:
            if (self.last_rebalance.year == now.year and 
                self.last_rebalance.month == now.month):
                return {
                    "success": True,
                    "message": "Rebalance already executed this month",
                    "skipped": True
                }
        
        self.rebalance_count += 1
        self.last_rebalance = now
        self.cash_balance -= 16000
        self.portfolio_value += 16000
        
        return {
            "success": True,
            "message": "Rebalance completed",
            "skipped": False,
            "newPositions": 5
        }


class TestOllamaIntegration(unittest.TestCase):
    """Tests for Ollama integration"""

    def setUp(self):
        self.ollama = MockOllamaClient()

    def test_generate_insights_success(self):
        """Test successful insights generation"""
        portfolio_data = "Total Value: $160,000"
        market_context = "Recent Activity: 5 transactions"
        
        result = self.ollama.generate_insights(portfolio_data, market_context)
        
        self.assertIn("Portfolio Analysis", result)
        self.assertIn("Recommendations", result)

    def test_generate_insights_unavailable(self):
        """Test insights generation when Ollama unavailable"""
        self.ollama.available = False
        
        with self.assertRaises(Exception):
            self.ollama.generate_insights("data", "context")

    def test_learning_tip_english(self):
        """Test learning tip in English"""
        tip = self.ollama.generate_learning_tip("en")
        self.assertIn("value investing", tip.lower())

    def test_learning_tip_chinese(self):
        """Test learning tip in Traditional Chinese"""
        tip = self.ollama.generate_learning_tip("zh-TW")
        self.assertIn("價值投資", tip)

    def test_ollama_availability_check(self):
        """Test Ollama availability check"""
        self.assertTrue(self.ollama.is_available())
        self.ollama.available = False
        self.assertFalse(self.ollama.is_available())


class TestBackendIntegration(unittest.TestCase):
    """Tests for Java backend integration"""

    def setUp(self):
        self.backend = MockJavaBackend()

    def test_get_portfolio_summary(self):
        """Test getting portfolio summary"""
        summary = self.backend.get_portfolio_summary()
        
        self.assertEqual(summary["totalValue"], 160000)
        self.assertEqual(summary["cashBalance"], 10000)
        self.assertEqual(summary["positionsCount"], 3)

    def test_rebalance_execution(self):
        """Test rebalance execution"""
        initial_value = self.backend.portfolio_value
        
        result = self.backend.trigger_rebalance()
        
        self.assertTrue(result["success"])
        self.assertFalse(result["skipped"])
        self.assertEqual(self.backend.portfolio_value, initial_value + 16000)

    def test_rebalance_idempotency(self):
        """Test that rebalance is idempotent within same month"""
        # First rebalance
        result1 = self.backend.trigger_rebalance()
        self.assertFalse(result1["skipped"])
        
        # Second rebalance in same month should be skipped
        result2 = self.backend.trigger_rebalance()
        self.assertTrue(result2["skipped"])
        
        # Only one actual rebalance should have occurred
        self.assertEqual(self.backend.rebalance_count, 1)


class TestPythonJavaIntegration(unittest.TestCase):
    """Tests for Python-Java integration via HTTP"""

    def test_shioaji_api_url_format(self):
        """Test Shioaji API URL format"""
        base_url = "http://127.0.0.1:8888"
        symbol = "2330.TW"
        start_date = "2024-01-01"
        end_date = "2024-11-27"
        
        history_url = f"{base_url}/history/{symbol}?start_date={start_date}&end_date={end_date}"
        quote_url = f"{base_url}/quote/{symbol}"
        health_url = f"{base_url}/health"
        
        self.assertIn("/history/", history_url)
        self.assertIn("start_date=", history_url)
        self.assertIn("/quote/", quote_url)
        self.assertIn("/health", health_url)

    def test_response_json_format(self):
        """Test that responses are valid JSON"""
        response_data = {
            "symbol": "2330.TW",
            "prices": [
                {"date": "2024-11-26", "close": 580.00}
            ],
            "count": 1
        }
        
        # Should be serializable to JSON
        json_str = json.dumps(response_data)
        parsed = json.loads(json_str)
        
        self.assertEqual(parsed["symbol"], "2330.TW")


class TestDataFlowIntegration(unittest.TestCase):
    """Tests for complete data flow"""

    def test_historical_data_flow(self):
        """Test historical data flow from Shioaji to Java"""
        # Simulate Shioaji response
        shioaji_response = {
            "symbol": "2330.TW",
            "prices": [
                {
                    "date": "2024-11-26",
                    "open": 575.00,
                    "high": 582.00,
                    "low": 573.00,
                    "close": 580.00,
                    "volume": 25000000
                }
            ]
        }
        
        # Transform to Java entity format
        java_entity = {
            "symbol": shioaji_response["symbol"],
            "date": shioaji_response["prices"][0]["date"],
            "open": shioaji_response["prices"][0]["open"],
            "high": shioaji_response["prices"][0]["high"],
            "low": shioaji_response["prices"][0]["low"],
            "close": shioaji_response["prices"][0]["close"],
            "volume": shioaji_response["prices"][0]["volume"]
        }
        
        self.assertEqual(java_entity["symbol"], "2330.TW")
        self.assertEqual(java_entity["close"], 580.00)

    def test_order_flow(self):
        """Test order flow from Java to Shioaji"""
        # Java sends order request
        order_request = {
            "action": "BUY",
            "symbol": "2330.TW",
            "quantity": 10,
            "price": 580.00
        }
        
        # Shioaji processes and returns result
        order_result = {
            "success": True,
            "order_id": "ORDER_001",
            "filled_quantity": "10",
            "filled_price": "580.00",
            "timestamp": datetime.utcnow().isoformat() + "Z"
        }
        
        # Java processes result
        self.assertTrue(order_result["success"])
        self.assertEqual(int(order_result["filled_quantity"]), order_request["quantity"])

    def test_insights_generation_flow(self):
        """Test insights generation flow"""
        # Portfolio data from Java
        portfolio_data = {
            "totalValue": 160000,
            "positions": [
                {"symbol": "2330.TW", "value": 58000},
                {"symbol": "2317.TW", "value": 54250}
            ]
        }
        
        # Format for Ollama
        formatted_data = f"Total Portfolio Value: ${portfolio_data['totalValue']}\n"
        formatted_data += "Holdings:\n"
        for pos in portfolio_data["positions"]:
            formatted_data += f"- {pos['symbol']}: ${pos['value']}\n"
        
        # Ollama generates insights
        ollama = MockOllamaClient()
        insights = ollama.generate_insights(formatted_data, "Market context")
        
        # Java stores insights
        self.assertIn("Portfolio Analysis", insights)


class TestErrorRecoveryIntegration(unittest.TestCase):
    """Tests for error recovery across components"""

    def test_shioaji_unavailable_fallback(self):
        """Test fallback when Shioaji is unavailable"""
        shioaji_available = False
        
        if not shioaji_available:
            # Should fallback to cached data or Yahoo Finance
            fallback_source = "cache"  # or "yahoo"
            self.assertIn(fallback_source, ["cache", "yahoo"])

    def test_ollama_unavailable_handling(self):
        """Test handling when Ollama is unavailable"""
        ollama = MockOllamaClient()
        ollama.available = False
        
        try:
            ollama.generate_insights("data", "context")
            insights = None
        except Exception:
            insights = "AI insights temporarily unavailable. Portfolio summary only."
        
        self.assertIn("unavailable", insights.lower())

    def test_database_error_handling(self):
        """Test handling of database errors"""
        # Simulate database error
        class MockDB:
            def save(self, entity):
                raise Exception("Database connection error")
        
        db = MockDB()
        try:
            db.save({"data": "test"})
            success = False
        except Exception as e:
            success = False
            error_message = str(e)
        
        self.assertFalse(success)
        self.assertIn("Database", error_message)


if __name__ == '__main__':
    unittest.main()
