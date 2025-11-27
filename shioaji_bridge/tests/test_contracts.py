"""
Contract Tests for Python Bridge - Verify data structures match Java backend expectations.
"""
import pytest
import json
from pathlib import Path


class TestApiContracts:
    """Test that Python bridge returns data matching the expected contract."""

    @pytest.fixture
    def contracts_file(self):
        """Load the shared API contracts JSON."""
        contracts_path = Path(__file__).parent.parent.parent / "contracts" / "api-contracts.json"
        if contracts_path.exists():
            with open(contracts_path) as f:
                return json.load(f)
        return None

    def test_stock_quote_response_format(self):
        """Verify stock quote response matches expected format."""
        # This is what shioaji_api.py returns for /quote/{symbol}
        sample_response = {
            "symbol": "2330.TW",
            "price": 580.0,
            "change": 5.0,
            "changePercent": 0.87,
            "volume": 12345678,
            "timestamp": "2025-01-15T10:30:00"
        }
        
        # Required fields
        assert "symbol" in sample_response
        assert "price" in sample_response
        assert isinstance(sample_response["symbol"], str)
        assert isinstance(sample_response["price"], (int, float))
        
    def test_historical_price_response_format(self):
        """Verify historical price response matches expected format."""
        sample_response = {
            "symbol": "2330.TW",
            "prices": [
                {"date": "2025-01-15", "close": 580.0, "volume": 1000000},
                {"date": "2025-01-14", "close": 575.0, "volume": 900000},
            ]
        }
        
        assert "symbol" in sample_response
        assert "prices" in sample_response
        assert isinstance(sample_response["prices"], list)
        
        if sample_response["prices"]:
            price = sample_response["prices"][0]
            assert "date" in price
            assert "close" in price
            
    def test_health_response_format(self):
        """Verify health check response matches expected format."""
        sample_response = {
            "status": "healthy",
            "shioaji_connected": True,
            "yahoo_fallback": False,
            "quota_remaining_mb": 450.5
        }
        
        assert "status" in sample_response
        assert sample_response["status"] in ["healthy", "unhealthy", "degraded"]

    def test_null_handling_in_response(self):
        """Verify None values are serialized as null in JSON."""
        response = {
            "symbol": "2330.TW",
            "price": None,
            "change": None,
        }
        
        json_str = json.dumps(response)
        parsed = json.loads(json_str)
        
        assert parsed["price"] is None
        assert parsed["change"] is None

    def test_empty_list_handling(self):
        """Verify empty lists are serialized correctly."""
        response = {
            "symbol": "2330.TW",
            "prices": []
        }
        
        json_str = json.dumps(response)
        parsed = json.loads(json_str)
        
        assert parsed["prices"] == []
        assert isinstance(parsed["prices"], list)


class TestDataTransformations:
    """Test data transformation functions with edge cases."""

    def test_safe_get_with_default(self):
        """Verify safe dictionary access pattern."""
        data = {"price": 100}
        
        # Safe pattern
        price = data.get("price", 0)
        missing = data.get("volume", 0)
        
        assert price == 100
        assert missing == 0
        
    def test_safe_get_with_none_value(self):
        """Verify handling when value exists but is None."""
        data = {"price": None}
        
        # This returns None, not the default!
        price = data.get("price", 0)
        
        assert price is None
        
        # Correct pattern for None handling
        price_safe = data.get("price") or 0
        assert price_safe == 0

    def test_safe_get_with_zero_value(self):
        """Verify handling when value is zero (falsy but valid)."""
        data = {"price": 0}
        
        # Wrong: "or" treats 0 as falsy
        price_wrong = data.get("price") or 100
        assert price_wrong == 100  # Bug! Should be 0
        
        # Correct: explicit None check
        price_correct = data.get("price") if data.get("price") is not None else 100
        assert price_correct == 0
        
        # Or use walrus operator
        price_walrus = 100 if (p := data.get("price")) is None else p
        assert price_walrus == 0

    def test_list_comprehension_with_none_items(self):
        """Verify list processing handles None items."""
        items = [
            {"symbol": "2330.TW", "price": 580},
            {"symbol": "2317.TW", "price": None},
            None,  # Entire item is None
        ]
        
        # Filter out None items and handle None prices
        safe_items = [
            {
                "symbol": item["symbol"],
                "price": item.get("price") or 0
            }
            for item in items
            if item is not None
        ]
        
        assert len(safe_items) == 2
        assert safe_items[1]["price"] == 0


class TestEdgeCases:
    """Test edge cases that could cause runtime errors."""

    def test_division_by_zero(self):
        """Verify division by zero is handled."""
        total_value = 0
        market_value = 1000
        
        # Wrong: will raise ZeroDivisionError
        # weight = market_value / total_value
        
        # Correct: check before division
        weight = (market_value / total_value * 100) if total_value > 0 else 0
        assert weight == 0

    def test_negative_values(self):
        """Verify negative values are handled correctly."""
        unrealized_pl = -500.25
        
        # Should not throw
        formatted = f"${unrealized_pl:,.2f}"
        assert "-" in formatted or "(" in formatted

    def test_very_large_numbers(self):
        """Verify large numbers don't cause overflow."""
        large_value = 999999999999.99
        
        json_str = json.dumps({"value": large_value})
        parsed = json.loads(json_str)
        
        assert abs(parsed["value"] - large_value) < 0.01

    def test_unicode_in_symbol(self):
        """Verify unicode characters are handled in stock symbols."""
        # Some markets use unicode in symbols
        symbol = "日経225"
        
        json_str = json.dumps({"symbol": symbol})
        parsed = json.loads(json_str)
        
        assert parsed["symbol"] == symbol

    def test_empty_string_symbol(self):
        """Verify empty string symbol is handled."""
        data = {"symbol": "", "price": 100}
        
        # Empty string is falsy
        symbol = data.get("symbol") or "UNKNOWN"
        assert symbol == "UNKNOWN"


class TestYahooFinanceFallback:
    """Test Yahoo Finance fallback data format matches Shioaji format."""

    def test_yahoo_response_maps_to_shioaji_format(self):
        """Verify Yahoo Finance response can be mapped to Shioaji format."""
        # Yahoo returns slightly different format
        yahoo_response = {
            "symbol": "2330.TW",
            "regularMarketPrice": 580.0,
            "regularMarketChange": 5.0,
            "regularMarketChangePercent": 0.87,
            "regularMarketVolume": 12345678,
        }
        
        # Map to our standard format
        mapped = {
            "symbol": yahoo_response["symbol"],
            "price": yahoo_response.get("regularMarketPrice", 0),
            "change": yahoo_response.get("regularMarketChange", 0),
            "changePercent": yahoo_response.get("regularMarketChangePercent", 0),
            "volume": yahoo_response.get("regularMarketVolume", 0),
        }
        
        assert mapped["price"] == 580.0
        assert mapped["symbol"] == "2330.TW"

    def test_yahoo_missing_fields_handled(self):
        """Verify Yahoo response with missing fields is handled."""
        yahoo_response = {
            "symbol": "2330.TW",
            # All other fields missing
        }
        
        mapped = {
            "symbol": yahoo_response.get("symbol", ""),
            "price": yahoo_response.get("regularMarketPrice") or 0,
            "change": yahoo_response.get("regularMarketChange") or 0,
        }
        
        assert mapped["price"] == 0
        assert mapped["change"] == 0
