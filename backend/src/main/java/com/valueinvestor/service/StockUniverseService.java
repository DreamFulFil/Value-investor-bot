package com.valueinvestor.service;

import com.valueinvestor.model.entity.StockUniverse;
import com.valueinvestor.model.dto.StockUniverseDTO;
import com.valueinvestor.repository.StockUniverseRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StockUniverseService {

    private static final Logger logger = LoggerFactory.getLogger(StockUniverseService.class);

    @Autowired
    private StockUniverseRepository stockUniverseRepository;

    /**
     * Top 30 Taiwan dividend-paying stocks to pre-seed
     */
    private static final String[][] INITIAL_STOCKS = {
        // Symbol, Name, Sector - All Taiwan stocks with .TW suffix
        {"2330.TW", "Taiwan Semiconductor (TSMC)", "Technology"},
        {"2317.TW", "Hon Hai Precision (Foxconn)", "Technology"},
        {"2454.TW", "MediaTek", "Technology"},
        {"2308.TW", "Delta Electronics", "Technology"},
        {"2303.TW", "United Microelectronics (UMC)", "Technology"},
        {"2357.TW", "Asustek Computer (ASUS)", "Technology"},
        {"2382.TW", "Quanta Computer", "Technology"},
        {"3008.TW", "Largan Precision", "Technology"},
        {"2474.TW", "Catcher Technology", "Technology"},
        {"2408.TW", "Nanya Technology", "Technology"},
        {"2881.TW", "Fubon Financial", "Financials"},
        {"2882.TW", "Cathay Financial", "Financials"},
        {"2884.TW", "E.SUN Financial", "Financials"},
        {"2886.TW", "Mega Financial", "Financials"},
        {"2891.TW", "CTBC Financial", "Financials"},
        {"2892.TW", "First Financial", "Financials"},
        {"2801.TW", "Chang Hwa Bank", "Financials"},
        {"2823.TW", "China Development Financial", "Financials"},
        {"5880.TW", "Taiwan Cooperative Financial", "Financials"},
        {"1301.TW", "Formosa Plastics", "Materials"},
        {"1303.TW", "Nan Ya Plastics", "Materials"},
        {"1326.TW", "Formosa Chemicals & Fibre", "Materials"},
        {"2002.TW", "China Steel", "Materials"},
        {"1216.TW", "Uni-President", "Consumer Staples"},
        {"2912.TW", "President Chain Store", "Consumer Staples"},
        {"1101.TW", "Taiwan Cement", "Materials"},
        {"2105.TW", "Cheng Shin Rubber", "Consumer Discretionary"},
        {"9910.TW", "FengTay Enterprise", "Consumer Discretionary"},
        {"2207.TW", "Ho Tung Chemical", "Materials"},
        {"2301.TW", "Lite-On Technology", "Technology"}
    };

    /**
     * Initialize stock universe on application startup
     */
    @PostConstruct
    @Transactional
    public void initializeUniverse() {
        long existingCount = stockUniverseRepository.countByActiveTrue();

        if (existingCount == 0) {
            logger.info("Initializing stock universe with {} Taiwan dividend stocks", INITIAL_STOCKS.length);

            for (String[] stockData : INITIAL_STOCKS) {
                try {
                    StockUniverse stock = new StockUniverse(stockData[0], stockData[1], stockData[2]);
                    stockUniverseRepository.save(stock);
                    logger.debug("Added stock to universe: {} - {}", stockData[0], stockData[1]);
                } catch (Exception e) {
                    logger.error("Failed to add stock {} to universe: {}", stockData[0], e.getMessage());
                }
            }

            logger.info("Stock universe initialized with {} stocks", INITIAL_STOCKS.length);
        } else {
            logger.info("Stock universe already initialized with {} active stocks", existingCount);
        }
    }

    /**
     * Add a stock to the universe
     */
    @Transactional
    public StockUniverse addStock(String symbol, String name, String sector) {
        Optional<StockUniverse> existing = stockUniverseRepository.findBySymbol(symbol);

        if (existing.isPresent()) {
            StockUniverse stock = existing.get();
            if (!stock.getActive()) {
                stock.setActive(true);
                stock.setAddedDate(LocalDate.now());
                stock.setRemovedDate(null);
                stock.setUpdatedAt(LocalDateTime.now());
                logger.info("Reactivated stock in universe: {}", symbol);
                return stockUniverseRepository.save(stock);
            } else {
                logger.warn("Stock {} already exists in active universe", symbol);
                return stock;
            }
        }

        StockUniverse newStock = new StockUniverse(symbol, name, sector);
        StockUniverse saved = stockUniverseRepository.save(newStock);
        logger.info("Added new stock to universe: {} - {}", symbol, name);
        return saved;
    }

    /**
     * Remove a stock from the universe (mark as inactive)
     */
    @Transactional
    public void removeStock(String symbol) {
        Optional<StockUniverse> stock = stockUniverseRepository.findBySymbol(symbol);

        if (stock.isPresent()) {
            StockUniverse stockEntity = stock.get();
            stockEntity.setActive(false);
            stockEntity.setRemovedDate(LocalDate.now());
            stockEntity.setUpdatedAt(LocalDateTime.now());
            stockUniverseRepository.save(stockEntity);
            logger.info("Removed stock from universe: {}", symbol);
        } else {
            logger.warn("Attempted to remove non-existent stock: {}", symbol);
        }
    }

    /**
     * Get all active stocks in the universe
     */
    public List<StockUniverse> getActiveStocks() {
        return stockUniverseRepository.findByActiveTrue();
    }

    /**
     * Get all active Taiwan stocks (for screening)
     */
    public List<StockUniverse> getActiveTWStocks() {
        return stockUniverseRepository.findByMarketAndActiveTrue("TW");
    }

    /**
     * Get stock by symbol
     */
    public Optional<StockUniverse> getStockBySymbol(String symbol) {
        return stockUniverseRepository.findBySymbol(symbol);
    }

    /**
     * Get all active stocks as DTOs
     */
    public List<StockUniverseDTO> getActiveStocksAsDTO() {
        return getActiveStocks().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get active stocks by sector
     */
    public List<StockUniverse> getActiveStocksBySector(String sector) {
        return stockUniverseRepository.findBySectorAndActiveTrue(sector);
    }

    /**
     * Get all distinct sectors
     */
    public List<String> getAllSectors() {
        return stockUniverseRepository.findDistinctActiveSectors();
    }

    /**
     * Get count of active stocks
     */
    public long getActiveStockCount() {
        return stockUniverseRepository.countByActiveTrue();
    }

    /**
     * Convert entity to DTO
     */
    private StockUniverseDTO convertToDTO(StockUniverse stock) {
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

    /**
     * Check if a symbol exists in the universe
     */
    public boolean existsBySymbol(String symbol) {
        return stockUniverseRepository.existsBySymbol(symbol);
    }

    /**
     * Get all symbols in the active universe
     */
    public List<String> getAllActiveSymbols() {
        return getActiveStocks().stream()
                .map(StockUniverse::getSymbol)
                .collect(Collectors.toList());
    }
}
