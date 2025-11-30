package com.valueinvestor.bot.repository;

import com.valueinvestor.bot.model.entity.TradingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the TradingConfig entity.
 */
@Repository
public interface TradingConfigRepository extends JpaRepository<TradingConfig, Long> {

    /**
     * Finds a TradingConfig entity by its user ID.
     * Assumes TradingConfig has a 'userId' field.
     *
     * @param userId The ID of the user.
     * @return An Optional containing the TradingConfig if found.
     */
    Optional<TradingConfig> findByUserId(String userId);
}
