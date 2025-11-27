package com.valueinvestor.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valueinvestor.model.dto.PositionDTO;
import com.valueinvestor.model.dto.PortfolioSummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract Tests - Verify that Java DTOs serialize to JSON that frontend expects.
 * These tests catch field naming mismatches like "quantity" vs "shares".
 */
@DisplayName("API Contract Tests")
public class ApiContractTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("PositionDTO Contract")
    class PositionDTOContract {

        @Test
        @DisplayName("serializes with exact field names frontend expects")
        void serializesWithCorrectFieldNames() throws Exception {
            PositionDTO dto = new PositionDTO();
            dto.setSymbol("2330.TW");
            dto.setQuantity(BigDecimal.valueOf(100.5));
            dto.setAveragePrice(BigDecimal.valueOf(580.00));
            dto.setCurrentPrice(BigDecimal.valueOf(595.00));
            dto.setMarketValue(BigDecimal.valueOf(59797.50));
            dto.setUnrealizedPL(BigDecimal.valueOf(1507.50));
            dto.setPlPercentage(BigDecimal.valueOf(2.59));

            String json = objectMapper.writeValueAsString(dto);
            JsonNode node = objectMapper.readTree(json);

            // Verify exact field names that frontend expects
            assertTrue(node.has("symbol"), "Must have 'symbol' field");
            assertTrue(node.has("quantity"), "Must have 'quantity' field (frontend maps to 'shares')");
            assertTrue(node.has("averagePrice"), "Must have 'averagePrice' field");
            assertTrue(node.has("currentPrice"), "Must have 'currentPrice' field");
            assertTrue(node.has("marketValue"), "Must have 'marketValue' field");
            assertTrue(node.has("unrealizedPL"), "Must have 'unrealizedPL' field (frontend maps to 'unrealizedGain')");
            assertTrue(node.has("plPercentage"), "Must have 'plPercentage' field");

            // Verify values
            assertEquals("2330.TW", node.get("symbol").asText());
            assertEquals(100.5, node.get("quantity").asDouble(), 0.001);
        }

        @Test
        @DisplayName("handles null values without throwing")
        void handlesNullValues() throws Exception {
            PositionDTO dto = new PositionDTO();
            dto.setSymbol("2330.TW");
            // All other fields are null

            String json = objectMapper.writeValueAsString(dto);
            JsonNode node = objectMapper.readTree(json);

            assertEquals("2330.TW", node.get("symbol").asText());
            assertTrue(node.get("quantity").isNull(), "Null quantity should serialize as null");
            assertTrue(node.get("marketValue").isNull(), "Null marketValue should serialize as null");
        }

        @Test
        @DisplayName("handles zero values correctly")
        void handlesZeroValues() throws Exception {
            PositionDTO dto = new PositionDTO();
            dto.setSymbol("2330.TW");
            dto.setQuantity(BigDecimal.ZERO);
            dto.setMarketValue(BigDecimal.ZERO);
            dto.setUnrealizedPL(BigDecimal.ZERO);

            String json = objectMapper.writeValueAsString(dto);
            JsonNode node = objectMapper.readTree(json);

            assertEquals(0, node.get("quantity").asDouble(), 0.001);
            assertEquals(0, node.get("marketValue").asDouble(), 0.001);
        }

        @Test
        @DisplayName("handles negative unrealizedPL correctly")
        void handlesNegativeValues() throws Exception {
            PositionDTO dto = new PositionDTO();
            dto.setSymbol("2317.TW");
            dto.setUnrealizedPL(BigDecimal.valueOf(-1500.25));

            String json = objectMapper.writeValueAsString(dto);
            JsonNode node = objectMapper.readTree(json);

            assertEquals(-1500.25, node.get("unrealizedPL").asDouble(), 0.001);
        }
    }

    @Nested
    @DisplayName("PortfolioSummaryDTO Contract")
    class PortfolioSummaryDTOContract {

        @Test
        @DisplayName("serializes with exact field names frontend expects")
        void serializesWithCorrectFieldNames() throws Exception {
            PortfolioSummaryDTO dto = new PortfolioSummaryDTO();
            dto.setTotalValue(BigDecimal.valueOf(150000.00));
            dto.setCashBalance(BigDecimal.valueOf(5000.00));
            dto.setInvestedAmount(BigDecimal.valueOf(145000.00));
            dto.setTotalPL(BigDecimal.valueOf(5000.00));
            dto.setPlPercentage(BigDecimal.valueOf(3.45));
            dto.setPositionCount(5);
            dto.setPositions(List.of());

            String json = objectMapper.writeValueAsString(dto);
            JsonNode node = objectMapper.readTree(json);

            // Verify exact field names
            assertTrue(node.has("totalValue"), "Must have 'totalValue' field");
            assertTrue(node.has("cashBalance"), "Must have 'cashBalance' field");
            assertTrue(node.has("investedAmount"), "Must have 'investedAmount' field");
            assertTrue(node.has("totalPL"), "Must have 'totalPL' field");
            assertTrue(node.has("plPercentage"), "Must have 'plPercentage' field");
            assertTrue(node.has("positionCount"), "Must have 'positionCount' field");
            assertTrue(node.has("positions"), "Must have 'positions' array");
            assertTrue(node.get("positions").isArray(), "'positions' must be an array");
        }

        @Test
        @DisplayName("handles empty positions array")
        void handlesEmptyPositions() throws Exception {
            PortfolioSummaryDTO dto = new PortfolioSummaryDTO();
            dto.setTotalValue(BigDecimal.ZERO);
            dto.setPositions(List.of());

            String json = objectMapper.writeValueAsString(dto);
            JsonNode node = objectMapper.readTree(json);

            assertTrue(node.get("positions").isArray());
            assertEquals(0, node.get("positions").size());
        }

        @Test
        @DisplayName("handles null positions array")
        void handlesNullPositions() throws Exception {
            PortfolioSummaryDTO dto = new PortfolioSummaryDTO();
            dto.setPositions(null);

            String json = objectMapper.writeValueAsString(dto);
            JsonNode node = objectMapper.readTree(json);

            assertTrue(node.get("positions").isNull(), "Null positions should serialize as null, not throw");
        }

        @Test
        @DisplayName("positions array contains correctly formatted PositionDTOs")
        void positionsArrayContainsCorrectFormat() throws Exception {
            PositionDTO position = new PositionDTO();
            position.setSymbol("2330.TW");
            position.setQuantity(BigDecimal.valueOf(100));
            position.setMarketValue(BigDecimal.valueOf(58000));

            PortfolioSummaryDTO dto = new PortfolioSummaryDTO();
            dto.setTotalValue(BigDecimal.valueOf(58000));
            dto.setPositionCount(1);
            dto.setPositions(Arrays.asList(position));

            String json = objectMapper.writeValueAsString(dto);
            JsonNode node = objectMapper.readTree(json);

            JsonNode positions = node.get("positions");
            assertEquals(1, positions.size());
            
            JsonNode firstPosition = positions.get(0);
            assertEquals("2330.TW", firstPosition.get("symbol").asText());
            assertEquals(100, firstPosition.get("quantity").asDouble(), 0.001);
            assertEquals(58000, firstPosition.get("marketValue").asDouble(), 0.001);
        }
    }

    @Nested
    @DisplayName("Field Mapping Documentation")
    class FieldMappingDocumentation {

        @Test
        @DisplayName("documents backend-to-frontend field name differences")
        void documentFieldMappings() {
            // This test serves as documentation for field mappings
            // Frontend api.ts must map these fields:
            
            // Backend PositionDTO.quantity -> Frontend Position.shares
            // Backend PositionDTO.unrealizedPL -> Frontend Position.unrealizedGain
            // Backend PortfolioSnapshot.timestamp -> Frontend PortfolioHistory.date
            // Backend PortfolioSnapshot.totalValue -> Frontend PortfolioHistory.value
            
            // If these mappings change, update:
            // 1. contracts/api-contracts.json
            // 2. frontend/src/lib/api.ts fetchPositions()
            // 3. This test
            
            assertTrue(true, "Field mappings documented - see comments above");
        }
    }
}
