package com.valueinvestor.service;

import com.valueinvestor.model.entity.StockUniverse;
import com.valueinvestor.model.dto.StockUniverseDTO;
import com.valueinvestor.repository.StockUniverseRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StockUniverseService {

    private static final Logger logger = LoggerFactory.getLogger(StockUniverseService.class);

    @Autowired
    private StockUniverseRepository stockUniverseRepository;
    
    @Autowired
    private TaiwanStockScreenerService stockScreenerService;
    
    @Value("${app.stock-universe.initial-size:50}")
    private int initialUniverseSize;

    /**
     * Initialize stock universe on application startup
     * Dynamically fetches top Taiwan dividend stocks from TaiwanStockScreenerService
     */
    @PostConstruct
    @Transactional
    public void initializeUniverse() {
        long existingCount = stockUniverseRepository.countByActiveTrue();

        if (existingCount == 0) {
            // Get top dividend stocks from screener service
            List<TaiwanStockScreenerService.StockInfo> topStocks = 
                stockScreenerService.getTopDividendStocks(initialUniverseSize);
            
            logger.info("Initializing stock universe with top {} Taiwan dividend stocks", topStocks.size());

            int successCount = 0;
            for (TaiwanStockScreenerService.StockInfo stockInfo : topStocks) {
                try {
                    StockUniverse stock = new StockUniverse(
                        stockInfo.getSymbol(), 
                        stockInfo.getName(), 
                        stockInfo.getSector()
                    );
                    stockUniverseRepository.save(stock);
                    logger.debug("Added stock to universe: {} - {}", stockInfo.getSymbol(), stockInfo.getName());
                    successCount++;
                } catch (Exception e) {
                    logger.error("Failed to add stock {} to universe: {}", stockInfo.getSymbol(), e.getMessage());
                }
            }

            logger.info("Stock universe initialized with {} stocks", successCount);
        } else {
            logger.info("Stock universe already initialized with {} active stocks", existingCount);
        }
    }
    
    /**
     * Refresh stock universe with latest top dividend stocks
     * Can be called periodically or manually to update the universe
     */
    @Transactional
    public int refreshUniverse() {
        logger.info("Refreshing stock universe with latest dividend stocks");
        
        List<TaiwanStockScreenerService.StockInfo> topStocks = 
            stockScreenerService.getAllDividendStocks();
        
        int addedCount = 0;
        for (TaiwanStockScreenerService.StockInfo stockInfo : topStocks) {
            Optional<StockUniverse> existing = stockUniverseRepository.findBySymbol(stockInfo.getSymbol());
            if (existing.isEmpty()) {
                try {
                    StockUniverse stock = new StockUniverse(
                        stockInfo.getSymbol(), 
                        stockInfo.getName(), 
                        stockInfo.getSector()
                    );
                    stockUniverseRepository.save(stock);
                    addedCount++;
                    logger.info("Added new stock to universe: {}", stockInfo.getSymbol());
                } catch (Exception e) {
                    logger.error("Failed to add stock {}: {}", stockInfo.getSymbol(), e.getMessage());
                }
            }
        }
        
        logger.info("Universe refresh complete. Added {} new stocks", addedCount);
        return addedCount;
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
