package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated snapshot powering the main Dashboard. Sales / production /
 * investment figures respect the requested date range; inventory and employee
 * figures are current (point-in-time) snapshots.
 */
@Data
@Builder
public class DashboardResponse {

    private String fromDate;
    private String toDate;

    // ── Sales (date-ranged) ──────────────────────────────
    private long salesCount;
    private BigDecimal salesRevenue;
    private BigDecimal salesDiscount;
    private BigDecimal salesReceived; // amount actually collected
    private BigDecimal salesDue;      // outstanding (revenue − received)
    private BigDecimal avgOrderValue;

    // ── Production (date-ranged) ─────────────────────────
    private long productionEntries;
    private long productionQty;
    private BigDecimal productionAmount;

    // ── Investment / expenses (date-ranged) ──────────────
    private long investmentCount;
    private BigDecimal investmentTotal;
    private BigDecimal expensesDue;

    // ── Inventory (current snapshot) ─────────────────────
    private BigDecimal inventoryStockValue;
    private BigDecimal inventoryUnits;
    private long lowStockCount;

    // ── Workforce (current snapshot) ─────────────────────
    private long activeEmployees;
    private long totalEmployees;

    // ── Customers (date-ranged) ──────────────────────────
    private long newCustomerCount;          // first-ever order falls in the range
    private List<TopCustomer> topCustomers; // most orders in the range

    // Revenue minus production cost and investment/expense over the range.
    private BigDecimal netProfit;

    // Gross profit on sales = revenue - cost of goods sold (range).
    private BigDecimal grossProfit;

    // ── Series & breakdowns ──────────────────────────────
    private List<SeriesPoint> revenueSeries;
    private List<Breakdown> paymentBreakdown;
    private List<TopProduct> topProducts;
    private List<TopEmployee> topEmployees;
    private List<RecentSale> recentSales;

    @Data
    @Builder
    public static class SeriesPoint {
        private String date;
        private BigDecimal revenue;
        private long orders;
    }

    @Data
    @Builder
    public static class Breakdown {
        private String label;
        private long count;
        private BigDecimal amount;
    }

    @Data
    @Builder
    public static class TopProduct {
        private String name;
        private long quantity;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    public static class TopEmployee {
        private String name;
        private long quantity;
        private BigDecimal earnings;
    }

    @Data
    @Builder
    public static class TopCustomer {
        private String name;
        private String mobile;
        private long orders;
        private BigDecimal spend;
    }

    @Data
    @Builder
    public static class RecentSale {
        private String invoiceNo;
        private String customerName;
        private BigDecimal amount;
        private String paymentStatus;
        private String date;
    }
}
