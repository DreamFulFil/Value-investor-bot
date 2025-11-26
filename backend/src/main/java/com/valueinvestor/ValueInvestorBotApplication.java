package com.valueinvestor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Value Investor Bot Application
 *
 * A sophisticated automated value investing system supporting US stock markets
 * with LLM-powered analysis and automated trading capabilities.
 */
@SpringBootApplication
@EnableScheduling
public class ValueInvestorBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValueInvestorBotApplication.class, args);
        System.out.println("========================================");
        System.out.println("  Value Investor Bot Started");
        System.out.println("  Backend API: http://localhost:8080");
        System.out.println("========================================");
    }

}
