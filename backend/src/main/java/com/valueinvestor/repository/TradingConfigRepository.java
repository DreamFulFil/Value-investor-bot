package com.valueinvestor.repository;

import com.valueinvestor.model.entity.TradingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TradingConfigRepository extends JpaRepository<TradingConfig, Long> {

    Optional<TradingConfig> findByKey(String key);

    default String getConfigValue(String key, String defaultValue) {
        return findByKey(key).map(TradingConfig::getValue).orElse(defaultValue);
    }

    default void setConfigValue(String key, String value) {
        TradingConfig config = findByKey(key).orElse(new TradingConfig(key, value));
        config.setValue(value);
        save(config);
    }
}
