package com.valueinvestor.controller;

import com.valueinvestor.model.dto.DataStatusDTO;
import com.valueinvestor.model.dto.HistoricalPriceDTO;
import com.valueinvestor.model.dto.StockUniverseDTO;
import com.valueinvestor.model.entity.StockPriceHistory;
import com.valueinvestor.model.entity.StockUniverse;
import com.valueinvestor.service.DataCatchUpService;
import com.valueinvestor.service.DataRefreshScheduler;
import com.valueinvestor.service.HistoricalDataService;
import com.valueinvestor.service.StockUniverseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "*")
public class DataController {

    private static final Logger logger = LoggerFactory.getLogger(DataController.class);

    @Autowired
    private StockUniverseService stockUniverseService;

    @Autowired
    private HistoricalDataService historicalDataService;

    @Autowired
    private DataCatchUpService dataCatchUpService;

    @Autowired
    private DataRefreshScheduler dataRefreshScheduler;

    /**
     * GET /api/data/universe - Get all tradeable stocks
     */
    @GetMapping("/universe")
    public ResponseEntity<List<StockUniverseDTO>> getStockUniverse(
            @RequestParam(required = false) String sector) {
        try {
            List<StockUniverseDTO> stocks;

            if (sector != null && !sector.isEmpty()) {
                stocks = stockUniverseService.getActiveStocksBySector(sector).stream()
                        .map(this::convertToUniverseDTO)
                        .collect(Collectors.toList());
            } else {
                stocks = stockUniverseService.getActiveStocksAsDTO();
            }

            logger.info("Retrieved {} stocks from universe", stocks.size());
            return ResponseEntity.ok(stocks);

        } catch (Exception e) {
            logger.error("Error retrieving stock universe: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/data/universe/sectors - Get all sectors
     */
    @GetMapping("/universe/sectors")
    public ResponseEntity<List<String>> getSectors() {
        try {
            List<String> sectors = stockUniverseService.getAllSectors();
            return ResponseEntity.ok(sectors);
        } catch (Exception e) {
            logger.error("Error retrieving sectors: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/data/historical/{symbol} - Get historical prices
     */
    @GetMapping("/historical/{symbol}")
    public ResponseEntity<List<HistoricalPriceDTO>> getHistoricalPrices(
            @PathVariable String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            // Default to last 1 year if dates not provided
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            if (startDate == null) {
                startDate = endDate.minusYears(1);
            }

            List<StockPriceHistory> priceHistory =
                    historicalDataService.getHistoricalPrices(symbol.toUpperCase(), startDate, endDate);

            List<HistoricalPriceDTO> dtos = priceHistory.stream()
                    .map(this::convertToHistoricalPriceDTO)
                    .collect(Collectors.toList());

            logger.info("Retrieved {} historical prices for {} from {} to {}",
                    dtos.size(), symbol, startDate, endDate);

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            logger.error("Error retrieving historical prices for {}: {}", symbol, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/data/latest/{symbol} - Get latest price for a symbol
     */
    @GetMapping("/latest/{symbol}")
    public ResponseEntity<HistoricalPriceDTO> getLatestPrice(@PathVariable String symbol) {
        try {
            Optional<StockPriceHistory> latestPrice =
                    historicalDataService.getLatestPrice(symbol.toUpperCase());

            if (latestPrice.isPresent()) {
                HistoricalPriceDTO dto = convertToHistoricalPriceDTO(latestPrice.get());
                return ResponseEntity.ok(dto);
            } else {
                logger.warn("No price data found for {}", symbol);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error retrieving latest price for {}: {}", symbol, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/data/refresh - Manual data refresh trigger
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> triggerRefresh(
            @RequestParam(required = false, defaultValue = "all") String type) {

        try {
            Map<String, String> response = new HashMap<>();

            switch (type.toLowerCase()) {
                case "fundamentals":
                    dataRefreshScheduler.refreshFundamentalsDaily();
                    response.put("message", "Fundamentals refresh triggered");
                    break;

                case "historical":
                    dataRefreshScheduler.refreshHistoricalPricesWeekly();
                    response.put("message", "Historical prices refresh triggered");
                    break;

                case "quotes":
                    dataRefreshScheduler.refreshQuotesHourly();
                    response.put("message", "Quote refresh triggered");
                    break;

                case "catchup":
                    dataCatchUpService.forceCatchUp();
                    response.put("message", "Data catch-up process triggered");
                    break;

                case "all":
                default:
                    dataRefreshScheduler.manualRefreshAll();
                    response.put("message", "Full data refresh triggered");
                    break;
            }

            response.put("type", type);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("status", "success");

            logger.info("Manual data refresh triggered: {}", type);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error triggering refresh: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * GET /api/data/status - Data freshness status
     */
    @GetMapping("/status")
    public ResponseEntity<DataStatusDTO> getDataStatus() {
        try {
            DataStatusDTO status = new DataStatusDTO();

            // Get stock universe info
            List<StockUniverse> activeStocks = stockUniverseService.getActiveStocks();
            status.setTotalStocksInUniverse(activeStocks.size());

            // Count stocks with historical data
            int stocksWithData = 0;
            Map<String, LocalDateTime> lastUpdateMap = new HashMap<>();

            for (StockUniverse stock : activeStocks) {
                String symbol = stock.getSymbol();
                if (historicalDataService.hasHistoricalData(symbol)) {
                    stocksWithData++;

                    Optional<StockPriceHistory> latest = historicalDataService.getLatestPrice(symbol);
                    if (latest.isPresent()) {
                        lastUpdateMap.put(symbol, latest.get().getCreatedAt());
                    }
                }
            }

            status.setStocksWithHistoricalData(stocksWithData);
            status.setStocksMissingData(activeStocks.size() - stocksWithData);
            status.setLastUpdateBySymbol(lastUpdateMap);

            // Get last refresh times
            status.setLastFundamentalsRefresh(dataRefreshScheduler.getLastFundamentalsRefresh());
            status.setLastHistoricalRefresh(dataRefreshScheduler.getLastHistoricalRefresh());
            status.setLastQuoteRefresh(dataRefreshScheduler.getLastQuoteRefresh());

            // Check if catch-up is in progress
            status.setCatchUpInProgress(dataCatchUpService.isCatchUpInProgress());

            // Set overall status
            if (status.getStocksMissingData() == 0) {
                status.setStatus("COMPLETE");
            } else if (status.isCatchUpInProgress()) {
                status.setStatus("IN_PROGRESS");
            } else {
                status.setStatus("INCOMPLETE");
            }

            logger.info("Data status retrieved: {} stocks with data, {} missing",
                    stocksWithData, status.getStocksMissingData());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("Error retrieving data status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/data/market-status - Get market open/closed status
     */
    @GetMapping("/market-status")
    public ResponseEntity<Map<String, String>> getMarketStatus() {
        try {
            Map<String, String> response = new HashMap<>();
            response.put("status", dataRefreshScheduler.getMarketStatus());
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving market status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/data/universe/add - Add stock to universe
     */
    @PostMapping("/universe/add")
    public ResponseEntity<StockUniverseDTO> addStockToUniverse(
            @RequestParam String symbol,
            @RequestParam String name,
            @RequestParam(required = false) String sector) {

        try {
            StockUniverse stock = stockUniverseService.addStock(
                    symbol.toUpperCase(), name, sector);

            StockUniverseDTO dto = convertToUniverseDTO(stock);

            logger.info("Added stock to universe: {}", symbol);
            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            logger.error("Error adding stock to universe: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/data/universe/{symbol} - Remove stock from universe
     */
    @DeleteMapping("/universe/{symbol}")
    public ResponseEntity<Map<String, String>> removeStockFromUniverse(@PathVariable String symbol) {
        try {
            stockUniverseService.removeStock(symbol.toUpperCase());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Stock removed from universe");
            response.put("symbol", symbol.toUpperCase());
            response.put("status", "success");

            logger.info("Removed stock from universe: {}", symbol);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error removing stock from universe: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper methods to convert entities to DTOs

    private StockUniverseDTO convertToUniverseDTO(StockUniverse stock) {
        return new StockUniverseDTO(
                stock.getId(),
                stock.getSymbol(),
                stock.getName(),
                stock.getSector(),
                stock.getMarket(),
                stock.getActive(),
                stock.getAddedDate()
        );
    }

    private HistoricalPriceDTO convertToHistoricalPriceDTO(StockPriceHistory price) {
        return new HistoricalPriceDTO(
                price.getSymbol(),
                price.getDate(),
                price.getOpen(),
                price.getHigh(),
                price.getLow(),
                price.getClose(),
                price.getVolume(),
                price.getAdjustedClose()
        );
    }
}
