package com.valueinvestor.bot.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BacktestResultDto {

    private List<DataPoint> dataPoints;

    public BacktestResultDto(List<DataPoint> dataPoints) {
        this.dataPoints = dataPoints;
    }

    public List<DataPoint> getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(List<DataPoint> dataPoints) {
        this.dataPoints = dataPoints;
    }

    public static class DataPoint {
        private LocalDate date;
        private BigDecimal value;

        public DataPoint(LocalDate date, BigDecimal value) {
            this.date = date;
            this.value = value;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }
    }
}
