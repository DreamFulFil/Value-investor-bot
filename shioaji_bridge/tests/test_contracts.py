"""
Contract Tests for Python Bridge - Verify data structures match expected contracts,
and that actual API responses conform to their defined Pydantic schemas.
"""
import pytest
import json
from pathlib import Path
from fastapi.testclient import TestClient
from jsonschema import validate, ValidationError

# Import the FastAPI app and Pydantic models from shioaji_api
from shioaji_api import app, QuoteResponse, PriceBar, HistoryResponse, HealthResponse, QuotaResponse, FundamentalsResponse

class TestApiContracts:
    """Test that Python bridge returns data matching the expected contract."""

    @pytest.fixture(scope="module")
    def client(self):
        """Fixture to provide a test client for the FastAPI app."""
        with TestClient(app) as c:
            yield c

    @pytest.fixture(scope="module")
    def pydantic_schemas(self):
        """Generate JSON schemas from Pydantic models."""
        schemas = {
            "QuoteResponse": QuoteResponse.model_json_schema(),
            "HistoryResponse": HistoryResponse.model_json_schema(),
            "HealthResponse": HealthResponse.model_json_schema(),
            "QuotaResponse": QuotaResponse.model_json_schema(),
            "FundamentalsResponse": FundamentalsResponse.model_json_schema(),
            "PriceBar": PriceBar.model_json_schema(),
        }
        # Inject PriceBar as a definition if not already part of HistoryResponse schema
        if "PriceBar" not in schemas["HistoryResponse"].get("definitions", {}):
            if "definitions" not in schemas["HistoryResponse"]:
                schemas["HistoryResponse"]["definitions"] = {}
            schemas["HistoryResponse"]["definitions"]["PriceBar"] = schemas["PriceBar"]
            
            # Update $ref in HistoryResponse items if necessary (Pydantic usually handles this)
            history_props = schemas["HistoryResponse"].get("properties", {})
            if "prices" in history_props:
                items_def = history_props["prices"].get("items", {})
                if "$ref" not in items_def and "title" in items_def and items_def["title"] == "PriceBar":
                    items_def["$ref"] = "#/definitions/PriceBar"
                    # Remove inline definition if it exists
                    items_def.pop("title", None)
                    items_def.pop("type", None)
                    items_def.pop("properties", None)
                    items_def.pop("required", None)

        return schemas

    def test_root_endpoint(self, client):
        """Verify the root endpoint returns expected status."""
        response = client.get("/")
        assert response.status_code == 200
        assert response.json() == {
            "service": "Shioaji Data API",
            "version": "1.0.0",
            "status": "running"
        }

    def test_health_check_endpoint_schema(self, client, pydantic_schemas):
        """Verify the /health endpoint response conforms to HealthResponse schema."""
        response = client.get("/health")
        assert response.status_code == 200
        try:
            validate(instance=response.json(), schema=pydantic_schemas["HealthResponse"])
        except ValidationError as e:
            pytest.fail(f"HealthResponse schema validation failed: {e.message}")

    def test_quota_endpoint_schema(self, client, pydantic_schemas):
        """Verify the /quota endpoint response conforms to QuotaResponse schema."""
        response = client.get("/quota")
        assert response.status_code == 200
        try:
            validate(instance=response.json(), schema=pydantic_schemas["QuotaResponse"])
        except ValidationError as e:
            pytest.fail(f"QuotaResponse schema validation failed: {e.message}")

        
    def test_quote_endpoint_schema(self, client, pydantic_schemas):
        """
        Verify the /quote/{symbol} endpoint response conforms to QuoteResponse schema.
        Using a valid symbol to test the success case.
        """
        response = client.get("/quote/2330.TW") # Use a valid symbol
        assert response.status_code == 200
        
        response_json = response.json()
        assert response_json["success"] # Expect success for a valid symbol
        
        try:
            validate(instance=response_json, schema=pydantic_schemas["QuoteResponse"])
        except ValidationError as e:
            pytest.fail(f"QuoteResponse schema validation failed: {e.message}")

    def test_history_endpoint_schema(self, client, pydantic_schemas):
        """
        Verify the /history/{symbol} endpoint response conforms to HistoryResponse schema.
        Using a valid symbol and date range to test the success case.
        """
        response = client.get("/history/2330.TW?start_date=2023-01-01&end_date=2023-01-05") # Valid symbol and date range
        assert response.status_code == 200
        
        response_json = response.json()
        assert response_json["success"] # Expect success for a valid symbol and date
        
        try:
            schema = pydantic_schemas["HistoryResponse"]
            validate(instance=response_json, schema=schema)
        except ValidationError as e:
            pytest.fail(f"HistoryResponse schema validation failed: {e.message}")

    def test_fundamentals_endpoint_schema(self, client, pydantic_schemas):
        """Verify the /fundamentals/{symbol} endpoint response conforms to FundamentalsResponse schema."""
        response = client.get("/fundamentals/2330.TW") # Use a valid symbol
        assert response.status_code == 200
        
        response_json = response.json()
        assert response_json["success"] # Expect success for a valid symbol
        
        try:
            validate(instance=response_json, schema=pydantic_schemas["FundamentalsResponse"])
        except ValidationError as e:
            pytest.fail(f"FundamentalsResponse schema validation failed: {e.message}")

    # The original tests for null handling and empty lists are still relevant,
    # as schema validation ensures types but these specific serialization behaviors
    # are good to explicitly confirm.
    def test_null_handling_in_response(self):
        """Verify None values are serialized as null in JSON."""
        # This test now ensures that Pydantic's serialization of None aligns with JSON null.
        # This is implicitly covered by schema validation (e.g., type: ["string", "null"]),
        # but kept for explicit confirmation of serialization behavior.
        response_model = QuoteResponse(success=True, symbol="TEST", price=None)
        json_str = response_model.model_dump_json() # Use Pydantic's serialization
        parsed = json.loads(json_str)
        assert parsed["price"] is None

    def test_empty_list_handling(self):
        """Verify empty lists are serialized correctly."""
        # Ensure Pydantic serializes empty lists as empty JSON arrays.
        response_model = HistoryResponse(success=True, symbol="TEST", prices=[])
        json_str = response_model.model_dump_json()
        parsed = json.loads(json_str)
        assert parsed["prices"] == []
        assert isinstance(parsed["prices"], list)

# Keeping other classes for now, as they test generic Python data handling.
# Consider moving them to a more appropriate unit test file if they are not
# directly related to the FastAPI contract.

# class TestDataTransformations: ...
# class TestEdgeCases: ...
# class TestYahooFinanceFallback: ...