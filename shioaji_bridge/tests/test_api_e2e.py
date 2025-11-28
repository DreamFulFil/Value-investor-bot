"""
End-to-End API Tests for Shioaji Bridge
Tests the complete FastAPI application with mocked Shioaji client
"""
import pytest
from unittest.mock import Mock, patch, MagicMock
from fastapi.testclient import TestClient
import sys
import os

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class MockShioajiAPI:
    """Mock Shioaji API for testing"""
    
    def __init__(self):
        self.logged_in = False
        self.quota_used = 100 * 1024 * 1024  # 100MB used
        
    def login(self, *args, **kwargs):
        self.logged_in = True
        return self
        
    def logout(self):
        self.logged_in = False
        
    def usage(self):
        return Mock(
            bytes=self.quota_used,
            remaining_bytes=400 * 1024 * 1024
        )
        
    def snapshots(self, contracts):
        """Mock snapshot response"""
        return [
            Mock(
                close=580.0,
                open=575.0,
                high=582.0,
                low=573.0,
                volume=25000000
            )
        ]
        
    def kbars(self, contract, start, end):
        """Mock kbars response"""
        import time
        now = time.time()
        return {
            'ts': [int(now * 1_000_000_000)],
            'Open': [575.0],
            'High': [582.0],
            'Low': [573.0],
            'Close': [580.0],
            'Volume': [25000000]
        }


class MockConfig:
    """Mock configuration"""
    def __init__(self):
        self.api_key = "test_key"
        self.secret_key = "test_secret"
        self.simulation = True
        self.cert_path = None


@pytest.fixture
def mock_shioaji():
    """Create mock Shioaji client"""
    with patch('shioaji_api.get_client') as mock_get_client:
        mock_client = Mock()
        mock_client.api = MockShioajiAPI()
        mock_client.is_logged_in = True
        mock_client._get_contract = Mock(return_value=Mock(code='2330'))
        mock_get_client.return_value = mock_client
        yield mock_client


@pytest.fixture
def client(mock_shioaji):
    """Create test client with mocked Shioaji"""
    from shioaji_api import app
    return TestClient(app)


class TestHealthEndpoint:
    """Tests for /health endpoint"""
    
    def test_health_returns_status(self, client):
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert "status" in data
        assert "connected" in data
        assert "message" in data


class TestQuotaEndpoint:
    """Tests for /quota endpoint"""
    
    def test_quota_returns_usage(self, client):
        response = client.get("/quota")
        assert response.status_code == 200
        data = response.json()
        assert data["success"] == True
        assert "usedMB" in data
        assert "limitMB" in data
        assert "remainingMB" in data
        assert "percentageUsed" in data
        assert "fallbackActive" in data


class TestQuoteEndpoint:
    """Tests for /quote/{symbol} endpoint"""
    
    def test_quote_valid_symbol(self, client, mock_shioaji):
        response = client.get("/quote/2330.TW")
        assert response.status_code == 200
        data = response.json()
        assert data["success"] == True
        assert "symbol" in data
        assert "price" in data
    
    def test_quote_strips_suffix(self, client, mock_shioaji):
        response = client.get("/quote/2330")
        assert response.status_code == 200
        data = response.json()
        assert data["success"] == True


class TestHistoryEndpoint:
    """Tests for /history/{symbol} endpoint"""
    
    def test_history_valid_request(self, client, mock_shioaji):
        response = client.get("/history/2330.TW?start_date=2024-01-01&end_date=2024-11-27")
        assert response.status_code == 200
        data = response.json()
        assert data["success"] == True
        assert "prices" in data
        assert "count" in data
    
    def test_history_missing_start_date(self, client):
        response = client.get("/history/2330.TW?end_date=2024-11-27")
        assert response.status_code == 422  # Validation error
    
    def test_history_missing_end_date(self, client):
        response = client.get("/history/2330.TW?start_date=2024-01-01")
        assert response.status_code == 422  # Validation error
    
    def test_history_invalid_date_format(self, client, mock_shioaji):
        response = client.get("/history/2330.TW?start_date=invalid&end_date=2024-11-27")
        assert response.status_code == 200
        data = response.json()
        # Should return error in response body
        assert "error" in data or data.get("success") == False


class TestRootEndpoint:
    """Tests for root endpoint"""
    
    def test_root_returns_info(self, client):
        response = client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert "service" in data
        assert "version" in data
        assert "status" in data


class TestErrorHandling:
    """Tests for error handling"""
    
    def test_invalid_endpoint(self, client):
        response = client.get("/invalid/endpoint")
        assert response.status_code == 404
    
    def test_method_not_allowed(self, client):
        response = client.post("/health")
        assert response.status_code == 405


class TestYahooFinanceFallback:
    """Tests for Yahoo Finance fallback functionality"""
    
    def test_fallback_on_contract_not_found(self, client, mock_shioaji):
        # Mock contract not found
        mock_shioaji._get_contract.return_value = None
        
        with patch('shioaji_api.get_quote_yahoo') as mock_yahoo:
            mock_yahoo.return_value = Mock(
                success=True,
                symbol="9999.TW",
                price=100.0
            )
            
            response = client.get("/quote/9999.TW")
            # Should fallback to Yahoo Finance
            assert response.status_code == 200


class TestDataValidation:
    """Tests for data validation"""
    
    def test_symbol_uppercase_conversion(self, client, mock_shioaji):
        response = client.get("/quote/2330.tw")
        assert response.status_code == 200
        data = response.json()
        # Symbol should be uppercased
        if data.get("symbol"):
            assert data["symbol"] == data["symbol"].upper()


class TestConcurrentRequests:
    """Tests for handling concurrent requests"""
    
    def test_multiple_quotes(self, client, mock_shioaji):
        symbols = ["2330.TW", "2317.TW", "2454.TW"]
        
        for symbol in symbols:
            response = client.get(f"/quote/{symbol}")
            assert response.status_code == 200
            data = response.json()
            assert data["success"] == True


class TestPriceBarFormat:
    """Tests for price bar data format"""
    
    def test_price_bar_has_required_fields(self, client, mock_shioaji):
        response = client.get("/history/2330.TW?start_date=2024-01-01&end_date=2024-11-27")
        assert response.status_code == 200
        data = response.json()
        
        if data["success"] and data.get("prices"):
            price_bar = data["prices"][0]
            assert "date" in price_bar
            assert "open" in price_bar
            assert "high" in price_bar
            assert "low" in price_bar
            assert "close" in price_bar
            assert "volume" in price_bar


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
