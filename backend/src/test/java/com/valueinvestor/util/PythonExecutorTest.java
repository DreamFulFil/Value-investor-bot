package com.valueinvestor.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PythonExecutorTest {

    private PythonExecutor pythonExecutor;

    @BeforeEach
    void setUp() {
        pythonExecutor = new PythonExecutor();
    }

    @Test
    void should_testShioajiConnection_when_scriptNotAvailable() {
        // When
        boolean isConnected = pythonExecutor.testShioajiConnection();

        // Then - Will likely be false since Shioaji is not set up in test environment
        assertThat(isConnected).isFalse();
    }

    @Test
    void should_throwException_when_scriptPathInvalid() {
        // When/Then
        assertThatThrownBy(() ->
            pythonExecutor.executePython("/invalid/path/script.py")
        ).isInstanceOf(Exception.class);
    }

    @Test
    void should_createOrderResult_when_allFieldsProvided() {
        // Given
        PythonExecutor.ShioajiOrderResult result = new PythonExecutor.ShioajiOrderResult();
        result.setSuccess(true);
        result.setOrderId("12345");
        result.setMessage("Order filled");
        result.setStatus("FILLED");
        result.setFilledQuantity(new BigDecimal("10"));
        result.setFilledPrice(new BigDecimal("150.00"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderId()).isEqualTo("12345");
        assertThat(result.getMessage()).isEqualTo("Order filled");
        assertThat(result.getStatus()).isEqualTo("FILLED");
        assertThat(result.getFilledQuantity()).isEqualByComparingTo("10");
        assertThat(result.getFilledPrice()).isEqualByComparingTo("150.00");
        assertThat(result.toString()).contains("12345");
    }
}
