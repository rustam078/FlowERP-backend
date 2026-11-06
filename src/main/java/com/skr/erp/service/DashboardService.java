package com.skr.erp.service;

import com.skr.erp.dto.response.DailyReportRow;
import com.skr.erp.dto.response.DashboardResponse;
import com.skr.erp.entity.SalesOrder;
import com.skr.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final ProductionEntryDetailRepository productionEntryDetailRepository;
    private final InvestmentRepository investmentRepository;
    private final InvestmentPaymentRepository investmentPaymentRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final EmployeeRepository employeeRepository;
    private final InventoryService inventoryService;

    private static BigDecimal[] zeros() {
        return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
    }

    private static LocalDate toLocalDate(Object o) {
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof java.util.Date d) return new java.sql.Date(d.getTime()).toLocalDate();
        return LocalDate.parse(o.toString());
    }

    // ── Null-safe parsing helpers for JPQL Object[] rows ─────
    private static Object[] first(List<Object[]> rows) {
        return (rows == null || rows.isEmpty()) ? new Object[]{0L, BigDecimal.ZERO, BigDecimal.ZERO} : rows.get(0);
    }

    private static BigDecimal bd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    private static long lng(Object o) {
        return (o instanceof Number n) ? n.longValue() : 0L;
    }

    private static String dateStr(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate d) return d.toString();
        if (o instanceof java.sql.Date d) return d.toLocalDate().toString();
        if (o instanceof java.util.Date d) return new java.sql.Date(d.getTime()).toLocalDate().toString();
        return o.toString();
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(LocalDate fromDate, LocalDate toDate) {

        // Default to the trailing 30 days when no range is supplied.
        LocalDate to = (toDate != null) ? toDate : LocalDate.now();
        LocalDate from = (fromDate != null) ? fromDate : to.minusDays(29);
        if (from.isAfter(to)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        // ── Sales ────────────────────────────────────────
        Object[] salesAgg = first(salesOrderRepository.aggregateSalesBetween(start, end));
        long salesCount = lng(salesAgg[0]);
        BigDecimal salesRevenue = bd(salesAgg[1]);
        BigDecimal salesDiscount = bd(salesAgg[2]);
        BigDecimal salesReceived = bd(salesAgg[3]); // actual cash tendered (may exceed revenue, clears old dues)
        BigDecimal salesDue = bd(salesAgg[4]);      // per-invoice due, floored at 0 in SQL
        BigDecimal avgOrderValue = salesCount > 0
                                   ? salesRevenue.divide(BigDecimal.valueOf(salesCount), 2, RoundingMode.HALF_UP)
                                   : BigDecimal.ZERO;

        // ── Production ───────────────────────────────────
        Object[] prodAgg = first(productionEntryDetailRepository.aggregateProductionBetween(from, to));
        long productionEntries = lng(prodAgg[0]);
        long productionQty = lng(prodAgg[1]);
        BigDecimal productionAmount = bd(prodAgg[2]);

        // ── Investment / expenses ────────────────────────
        Object[] invAgg = first(investmentRepository.aggregateInvestmentBetween(from, to));
        long investmentCount = lng(invAgg[0]);
        BigDecimal investmentTotal = bd(invAgg[1]);
        BigDecimal investmentPaid = bd(investmentPaymentRepository.paidForInvestmentsBetween(from, to));
        BigDecimal expensesDue = investmentTotal.subtract(investmentPaid).max(BigDecimal.ZERO);

        // ── Inventory snapshot ───────────────────────────
        Object[] stockAgg = first(inventoryBatchRepository.aggregateActiveInventory());
        BigDecimal inventoryUnits = bd(stockAgg[0]);
        BigDecimal inventoryStockValue = bd(stockAgg[1]);
        long lowStockCount = inventoryService.getLowStockAlerts().size();

        // ── Workforce snapshot ───────────────────────────
        long activeEmployees = employeeRepository.countByActiveTrue();
        long totalEmployees = employeeRepository.count();

        BigDecimal netProfit = salesRevenue.subtract(productionAmount).subtract(investmentTotal);

        // Gross profit on sales = revenue - cost of goods sold over the range.
        BigDecimal grossProfit = bd(salesOrderItemRepository.grossProfitBetween(start, end));

        // ── Revenue trend ────────────────────────────────
        List<DashboardResponse.SeriesPoint> revenueSeries = salesOrderRepository.revenueSeriesBetween(start, end)
                                                            .stream().map(r -> DashboardResponse.SeriesPoint.builder()
                                                                                        .date(dateStr(r[0])).revenue(bd(r[1]))
                                                                                        .orders(lng(r[2])).build()).toList();

        // ── Received by payment mode, plus a Due slice ───
        List<DashboardResponse.Breakdown> paymentBreakdown = new java.util.ArrayList<>(
                salesOrderRepository.paymentBreakdownBetween(start, end).stream()
                        .filter(r -> bd(r[2]).signum() > 0)
                        .map(r -> DashboardResponse.Breakdown.builder()
                                .label(r[0] != null ? r[0].toString() : "UNKNOWN")
                                .count(lng(r[1])).amount(bd(r[2])).build()).toList());
        if (salesDue.signum() > 0) {
            paymentBreakdown.add(DashboardResponse.Breakdown.builder().label("Due").amount(salesDue).build());
        }

        // ── Top products ─────────────────────────────────
        List<DashboardResponse.TopProduct> topProducts = salesOrderItemRepository.topProductsBetween(start, end, PageRequest.of(0, 12))
                .stream().map(r -> DashboardResponse.TopProduct.builder()
                        .name(r[0] != null ? r[0].toString() : "Unknown")
                        .quantity(lng(r[1])).revenue(bd(r[2])).build()).toList();

        // ── Top employees by production (piece-rate output) ──
        List<DashboardResponse.TopEmployee> topEmployees = productionEntryDetailRepository.topEmployeesBetween(from, to, PageRequest.of(0, 20))
                                            .stream().map(r -> DashboardResponse.TopEmployee.builder()
                                                                    .name(r[0] != null ? r[0].toString() : "Unknown")
                                                                    .quantity(lng(r[1])).earnings(bd(r[2])).build()).toList();

        // ── Customers (date-ranged) ──────────────────────
        List<DashboardResponse.TopCustomer> topCustomers = salesOrderRepository.topCustomersBetween(start, end, PageRequest.of(0, 10))
                                                            .stream().map(r -> DashboardResponse.TopCustomer.builder()
                                                                                .name(r[0] != null ? r[0].toString() : "Walk-in")
                                                                                .mobile(r[1] != null ? r[1].toString() : "")
                                                                                .orders(lng(r[2])).spend(bd(r[3])).build())
                                                                                  .toList();
        long newCustomerCount = salesOrderRepository.newCustomerMobilesBetween(start, end).size();

        // ── Recent sales ─────────────────────────────────
        List<DashboardResponse.RecentSale> recentSales = salesOrderRepository.findRecent(PageRequest.of(0, 8))
                                                           .stream().map(this::toRecentSale).toList();

        return DashboardResponse.builder()
                .fromDate(from.toString())
                .toDate(to.toString())
                .salesCount(salesCount)
                .salesRevenue(salesRevenue)
                .salesDiscount(salesDiscount)
                .salesReceived(salesReceived)
                .salesDue(salesDue)
                .avgOrderValue(avgOrderValue)
                .productionEntries(productionEntries)
                .productionQty(productionQty)
                .productionAmount(productionAmount)
                .investmentCount(investmentCount)
                .investmentTotal(investmentTotal)
                .expensesDue(expensesDue)
                .inventoryStockValue(inventoryStockValue)
                .inventoryUnits(inventoryUnits)
                .lowStockCount(lowStockCount)
                .activeEmployees(activeEmployees)
                .totalEmployees(totalEmployees)
                .newCustomerCount(newCustomerCount)
                .topCustomers(topCustomers)
                .netProfit(netProfit)
                .grossProfit(grossProfit)
                .revenueSeries(revenueSeries)
                .paymentBreakdown(paymentBreakdown)
                .topProducts(topProducts)
                .topEmployees(topEmployees)
                .recentSales(recentSales).build();
    }

    @Transactional(readOnly = true)
    public List<DailyReportRow> getDailyReport(LocalDate fromDate, LocalDate toDate) {

        LocalDate to = (toDate != null) ? toDate : LocalDate.now();
        LocalDate from = (fromDate != null) ? fromDate : to.minusDays(29);
        if (from.isAfter(to)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        // date -> [investment, wages, sales, cogs]
        Map<LocalDate, BigDecimal[]> byDate = new TreeMap<>();

        for (Object[] r : investmentRepository.investmentSeriesBetween(from, to)) {
            byDate.computeIfAbsent(toLocalDate(r[0]), k -> zeros())[0] = bd(r[1]);
        }
        for (Object[] r : productionEntryDetailRepository.wagesSeriesBetween(from, to)) {
            byDate.computeIfAbsent(toLocalDate(r[0]), k -> zeros())[1] = bd(r[1]);
        }
        for (Object[] r : salesOrderRepository.revenueSeriesBetween(start, end)) {
            byDate.computeIfAbsent(toLocalDate(r[0]), k -> zeros())[2] = bd(r[1]);
        }
        for (Object[] r : salesOrderItemRepository.cogsSeriesBetween(start, end)) {
            byDate.computeIfAbsent(toLocalDate(r[0]), k -> zeros())[3] = bd(r[1]);
        }

        return byDate.entrySet().stream()
                .map(e -> DailyReportRow.builder()
                        .date(e.getKey().toString()).investment(e.getValue()[0])
                        .wages(e.getValue()[1]).sales(e.getValue()[2]).cogs(e.getValue()[3])
                        .build()).toList();
    }

    private DashboardResponse.RecentSale toRecentSale(SalesOrder s) {
        return DashboardResponse.RecentSale.builder()
                .invoiceNo(s.getInvoiceNo())
                .customerName(s.getCustomerName())
                .amount(bd(s.getGrandTotal()))
                .paymentStatus(s.getPaymentStatus() != null ? s.getPaymentStatus().name() : null)
                .date(s.getCreatedAt() != null ? s.getCreatedAt().toLocalDate().toString() : null)
                .build();
    }
}
