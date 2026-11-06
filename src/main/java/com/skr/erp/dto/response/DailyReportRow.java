package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * One day's money in/out for the Dashboard daily report.
 * investment + wages = cash out; sales = cash in; cogs = cost of goods sold that day.
 */
@Data
@Builder
public class DailyReportRow {
    private String date;
    private BigDecimal investment;
    private BigDecimal wages;
    private BigDecimal sales;
    private BigDecimal cogs;
}
