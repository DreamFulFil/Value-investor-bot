package com.valueinvestor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Database configuration for SQLite
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.valueinvestor.repository")
public class DatabaseConfig {
    // SQLite-specific configuration will be added here as needed
}
