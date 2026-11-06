package com.skr.erp.service;

import com.skr.erp.dto.response.EmployeeDetailsResponse;
import com.skr.erp.dto.response.EmployeeProductSummaryResponse;
import com.skr.erp.dto.response.EmployeeProductionHistoryResponse;
import com.skr.erp.report.ReportExcelWriter;
import com.skr.erp.report.ReportPeriod;
import com.skr.erp.report.ReportSection;
import com.skr.erp.report.ReportSupport;
import com.skr.erp.report.ReportWorkbookData;
import com.skr.erp.repository.ProductionEntryDetailRepository;
import com.skr.erp.repository.SalesOrderItemRepository;
import com.skr.erp.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Builds date-range Production, Sales and Employee reports as PDF (HTML templates) and Excel. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final String PRODUCTION_TEMPLATE = "PRODUCTION_REPORT_TEMPLATE";
    private static final String SALES_TEMPLATE = "SALES_REPORT_TEMPLATE";
    private static final String EMPLOYEE_TEMPLATE = "EMPLOYEE_REPORT_TEMPLATE";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd-MMM");
    private static final String DASH = "-";
    private static final PageRequest TOP = PageRequest.of(0, 1000);
    // How many invoice rows the PDF lists (readable cap); Excel still gets the full list.
    private static final PageRequest INVOICE_PDF_ROWS = PageRequest.of(0, 5000);

    private final ReportSupport support;
    private final ProductionEntryDetailRepository productionDetailRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final EmployeeService employeeService;

    public String fileToken(LocalDate from, LocalDate to) {
        return ReportPeriod.of(from, to).fileToken();
    }

    // ── Production ──────────────────────────────────────
    public byte[] productionPdf(LocalDate from, LocalDate to) {
        return support.renderPdf(PRODUCTION_TEMPLATE, productionValues(ReportPeriod.of(from, to)));
    }

    public byte[] productionExcel(LocalDate from, LocalDate to) {
        ReportPeriod p = ReportPeriod.of(from, to);
        Object[] t = productionDetailRepository.aggregateProductionBetween(p.from(), p.to()).get(0);
        List<String[]> summary = List.of(
                new String[]{"Entries", support.num(asLong(t[0]))},
                new String[]{"Total pieces", support.num(asLong(t[1]))},
                new String[]{"Total payment", support.money(asBig(t[2]))});
        List<ReportSection> sections = List.of(
                new ReportSection("By product", List.of("Product", "Pieces", "Payment"),
                        rows3(productionDetailRepository.productionByProductBetween(p.from(), p.to()))),
                new ReportSection("By employee", List.of("Employee", "Pieces", "Payment"),
                        rows3(productionDetailRepository.topEmployeesBetween(p.from(), p.to(), TOP))));
        return ReportExcelWriter.build(workbook("Production report", p, summary, sections));
    }

    private Map<String, String> productionValues(ReportPeriod p) {
        Object[] t = productionDetailRepository.aggregateProductionBetween(p.from(), p.to()).get(0);
        Map<String, String> v = base(p);
        v.put("totalEntries", support.num(asLong(t[0])));
        v.put("totalQuantity", support.num(asLong(t[1])));
        v.put("totalAmount", support.money(asBig(t[2])));
        v.put("productRows", htmlRows3(productionDetailRepository.productionByProductBetween(p.from(), p.to())));
        v.put("employeeRows", htmlRows3(productionDetailRepository.topEmployeesBetween(p.from(), p.to(), TOP)));
        return v;
    }

    // ── Sales ───────────────────────────────────────────
    public byte[] salesPdf(LocalDate from, LocalDate to) {
        return support.renderPdf(SALES_TEMPLATE, salesValues(ReportPeriod.of(from, to)));
    }

    public byte[] salesExcel(LocalDate from, LocalDate to) {
        ReportPeriod p = ReportPeriod.of(from, to);
        Object[] t = salesOrderRepository.reportSummaryBetween(p.start(), p.endExclusive()).get(0);
        BigDecimal mrp = asBig(t[1]), discount = asBig(t[2]);
        BigDecimal due = asBig(t[6]);
        List<String[]> summary = List.of(
                new String[]{"Total Sales", support.num(asLong(t[0]))},
                new String[]{"MRP Total", support.money(mrp)},
                new String[]{"Sale", support.money(mrp.subtract(discount))},
                new String[]{"Discount", support.money(discount)},
                new String[]{"Due", support.money(due)},
                new String[]{"GST", support.money(asBig(t[3]))},
                new String[]{"Net Profit", support.money(salesOrderItemRepository.grossProfitBetween(p.start(), p.endExclusive()))});
        List<ReportSection> sections = List.of(
                new ReportSection("Invoice details",
                        List.of("Inv No", "Date", "Customer", "Mobile", "Sale Amount (incl GST)", "Received", "Due", "Status"),
                        invoiceExcelRows(salesOrderRepository.salesInvoicesBetween(p.start(), p.endExclusive(), Pageable.unpaged()))),
                new ReportSection("By product", List.of("Product", "Qty sold", "Sale Amount", "Profit"),
                        rows4(salesOrderItemRepository.salesByProductBetween(p.start(), p.endExclusive(), TOP))));
        return ReportExcelWriter.build(workbook("Sales report", p, summary, sections));
    }

    private Map<String, String> salesValues(ReportPeriod p) {
        Object[] t = salesOrderRepository.reportSummaryBetween(p.start(), p.endExclusive()).get(0);
        BigDecimal mrp = asBig(t[1]), discount = asBig(t[2]);
        BigDecimal received = asBig(t[5]), due = asBig(t[6]);
        Map<String, String> v = base(p);
        v.put("totalSales", support.num(asLong(t[0])));
        v.put("mrpTotal", support.money(mrp));
        v.put("saleAmount", support.money(mrp.subtract(discount)));
        v.put("totalDiscount", support.money(discount));
        v.put("totalDue", support.money(due));
        v.put("totalTax", support.money(asBig(t[3])));
        v.put("totalProfit", support.money(salesOrderItemRepository.grossProfitBetween(p.start(), p.endExclusive())));
        // PDF lists the latest INVOICE_PDF_ROWS invoices (a full list of thousands would be unprintable); Excel has all.
        v.put("invoiceRows", htmlInvoiceRows(salesOrderRepository.salesInvoicesBetween(p.start(), p.endExclusive(), INVOICE_PDF_ROWS), asLong(t[0])));
        v.put("invTotalSale", support.money(asBig(t[4])));
        v.put("invTotalReceived", support.money(received));
        v.put("invTotalDue", support.money(due));
        v.put("productRows", htmlRows4(salesOrderItemRepository.salesByProductBetween(p.start(), p.endExclusive(), TOP)));
        return v;
    }

    // ── Employee ────────────────────────────────────────
    public byte[] employeePdf(UUID employeeId, LocalDate from, LocalDate to) {
        ReportPeriod p = ReportPeriod.of(from, to);
        return support.renderPdf(EMPLOYEE_TEMPLATE, employeeValues(employeeId, p));
    }

    public byte[] employeeExcel(UUID employeeId, LocalDate from, LocalDate to) {
        ReportPeriod p = ReportPeriod.of(from, to);
        EmployeeDetailsResponse d = employeeService.getDetails(employeeId, p.from(), p.to());
        List<String[]> summary = List.of(
                new String[]{"Employee", d.getEmployee().getFullName()},
                new String[]{"Total pieces", support.num(d.getSummary().getTotalProductionQty())},
                new String[]{"Products", support.num(d.getSummary().getProductsWorkedOn())},
                new String[]{"Total payment", support.money(d.getSummary().getTotalEarnings())},
                new String[]{"Avg rate / pc", support.money(avgRate(d.getSummary().getTotalEarnings(), d.getSummary().getTotalProductionQty()))});
        List<ReportSection> sections = List.of(
                new ReportSection("Daily production", List.of("Date", "Product", "Pieces", "Rate", "Payment"), employeeHistoryRows(d.getProductionHistory())),
                new ReportSection("By product", List.of("Product", "Pieces", "Payment"), employeeProductRows(d.getProductSummary())));
        return ReportExcelWriter.build(workbook("Employee report", p, summary, sections));
    }

    private Map<String, String> employeeValues(UUID employeeId, ReportPeriod p) {
        EmployeeDetailsResponse d = employeeService.getDetails(employeeId, p.from(), p.to());
        Map<String, String> v = base(p);
        v.put("employeeName", support.esc(d.getEmployee().getFullName()));
        v.put("employeeCode", support.esc(d.getEmployee().getEmployeeCode() == null ? "-" : d.getEmployee().getEmployeeCode()));
        v.put("totalQuantity", support.num(d.getSummary().getTotalProductionQty()));
        v.put("totalEarnings", support.money(d.getSummary().getTotalEarnings()));
        v.put("productsWorkedOn", support.num(d.getSummary().getProductsWorkedOn()));
        v.put("avgRate", support.money(avgRate(d.getSummary().getTotalEarnings(), d.getSummary().getTotalProductionQty())));
        v.put("productRows", htmlEmployeeProducts(d.getProductSummary()));
        v.put("historyRows", htmlEmployeeHistory(d.getProductionHistory()));
        return v;
    }

    // ── Shared builders ─────────────────────────────────
    private Map<String, String> base(ReportPeriod p) {
        Map<String, String> v = new HashMap<>();
        v.put("fromDate", p.fromLabel());
        v.put("toDate", p.toLabel());
        return v;
    }

    private ReportWorkbookData workbook(String title, ReportPeriod p, List<String[]> summary, List<ReportSection> sections) {
        return new ReportWorkbookData(title, p.label(), support.companyName(), support.companyLine(), summary, sections);
    }

    private String htmlRows3(List<Object[]> rows) {
        if (rows.isEmpty()) return emptyRow(3);
        StringBuilder sb = new StringBuilder();
        for (Object[] r : rows) {
            sb.append("<tr><td>").append(support.esc(str(r[0]))).append("</td>")
                    .append("<td class=\"num\">").append(support.num(asLong(r[1]))).append("</td>")
                    .append("<td class=\"num\">").append(support.money(asBig(r[2]))).append("</td></tr>");
        }
        return sb.toString();
    }

    private String htmlRows4(List<Object[]> rows) {
        if (rows.isEmpty()) return emptyRow(4);
        StringBuilder sb = new StringBuilder();
        for (Object[] r : rows) {
            sb.append("<tr><td>").append(support.esc(str(r[0]))).append("</td>")
                    .append("<td class=\"num\">").append(support.num(asLong(r[1]))).append("</td>")
                    .append("<td class=\"num\">").append(support.money(asBig(r[2]))).append("</td>")
                    .append("<td class=\"num\">").append(support.money(asBig(r[3]))).append("</td></tr>");
        }
        return sb.toString();
    }

    // One HTML row per invoice (latest first); amount/status derived from grand total vs received.
    // Only the first `total`-capped rows are rendered in the PDF; a note points to Excel for the rest.
    private String htmlInvoiceRows(List<Object[]> rows, long total) {
        if (rows.isEmpty()) return emptyRow(8);
        StringBuilder sb = new StringBuilder();
        for (Object[] r : rows) {
            BigDecimal grand = asBig(r[4]), received = asBig(r[5]), paid = asBig(r[6]);
            BigDecimal due = grand.subtract(paid).max(BigDecimal.ZERO);
            sb.append("<tr><td>").append(support.esc(str(r[0]))).append("</td>")
                    .append("<td>").append(dayMonth(r[1])).append("</td>")
                    .append("<td>").append(customer(r[2])).append("</td>")
                    .append("<td>").append(mobile(r[3])).append("</td>")
                    .append("<td class=\"num\">").append(support.money(grand)).append("</td>")
                    .append("<td class=\"num\">").append(received.signum() == 0 ? DASH : support.money(received)).append("</td>")
                    .append("<td class=\"num\">").append(due.signum() == 0 ? DASH : support.money(due)).append("</td>")
                    .append("<td class=\"ctr\">").append(statusPill(paid, due)).append("</td></tr>");
        }
        if (total > rows.size()) {
            sb.append("<tr><td colspan=\"8\" style=\"text-align:center;color:#666666;font-style:italic;\">Showing latest ")
                    .append(rows.size()).append(" of ").append(total)
                    .append(" invoices. Download the Excel report for the complete list.</td></tr>");
        }
        return sb.toString();
    }

    // Status pill from the amount applied to this invoice: Paid (no due), Partial (some paid), Unpaid.
    private String statusPill(BigDecimal paid, BigDecimal due) {
        if (due.signum() <= 0) return "<span class=\"st\">Paid</span>";
        if (paid.signum() > 0) return "<span class=\"st partial\">Partial</span>";
        return "<span class=\"st unpaid\">Unpaid</span>";
    }

    // Invoice rows for Excel (same data, status as plain text).
    private List<List<String>> invoiceExcelRows(List<Object[]> rows) {
        List<List<String>> out = new ArrayList<>();
        for (Object[] r : rows) {
            BigDecimal grand = asBig(r[4]), received = asBig(r[5]), paid = asBig(r[6]);
            BigDecimal due = grand.subtract(paid).max(BigDecimal.ZERO);
            String status = due.signum() <= 0 ? "Paid" : paid.signum() > 0 ? "Partial" : "Unpaid";
            out.add(List.of(str(r[0]), dayMonth(r[1]), customerText(r[2]), mobileText(r[3]),
                    support.money(grand), support.money(received), support.money(due), status));
        }
        return out;
    }

    private String dayMonth(Object o) {
        if (o instanceof java.time.LocalDateTime dt) return dt.format(DAY);
        if (o instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().format(DAY);
        return DASH;
    }

    private String customerText(Object o) {
        String s = o == null ? "" : o.toString().trim();
        return s.isEmpty() ? "Walk-in" : s;
    }

    private String mobileText(Object o) {
        String s = o == null ? "" : o.toString().trim();
        return s.isEmpty() ? DASH : s;
    }

    private String customer(Object o) {
        return support.esc(customerText(o));
    }

    private String mobile(Object o) {
        String s = mobileText(o);
        return DASH.equals(s) ? s : support.esc(s);
    }

    private String htmlEmployeeProducts(List<EmployeeProductSummaryResponse> list) {
        if (list == null || list.isEmpty()) return emptyRow(3);
        StringBuilder sb = new StringBuilder();
        for (EmployeeProductSummaryResponse x : list) {
            sb.append("<tr><td>").append(support.esc(x.getProductName())).append("</td>")
                    .append("<td class=\"num\">").append(support.num(x.getQuantity())).append("</td>")
                    .append("<td class=\"num\">").append(support.money(x.getEarnings())).append("</td></tr>");
        }
        return sb.toString();
    }

    private String htmlEmployeeHistory(List<EmployeeProductionHistoryResponse> list) {
        if (list == null || list.isEmpty()) return emptyRow(5);
        StringBuilder sb = new StringBuilder();
        for (EmployeeProductionHistoryResponse x : list) {
            sb.append("<tr><td>").append(x.getProductionDate() == null ? "-" : x.getProductionDate().format(DAY)).append("</td>")
                    .append("<td>").append(support.esc(x.getProductName())).append("</td>")
                    .append("<td class=\"num\">").append(support.num(x.getQuantity())).append("</td>")
                    .append("<td class=\"num\">").append(support.money(x.getRate())).append("</td>")
                    .append("<td class=\"num\">").append(support.money(x.getEarnings())).append("</td></tr>");
        }
        return sb.toString();
    }

    private List<List<String>> rows3(List<Object[]> rows) {
        List<List<String>> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(List.of(str(r[0]), support.num(asLong(r[1])), support.money(asBig(r[2]))));
        }
        return out;
    }

    private List<List<String>> rows4(List<Object[]> rows) {
        List<List<String>> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(List.of(str(r[0]), support.num(asLong(r[1])), support.money(asBig(r[2])), support.money(asBig(r[3]))));
        }
        return out;
    }

    private List<List<String>> employeeProductRows(List<EmployeeProductSummaryResponse> list) {
        List<List<String>> out = new ArrayList<>();
        if (list != null) {
            for (EmployeeProductSummaryResponse x : list) {
                out.add(List.of(nz(x.getProductName()), support.num(x.getQuantity()), support.money(x.getEarnings())));
            }
        }
        return out;
    }

    private List<List<String>> employeeHistoryRows(List<EmployeeProductionHistoryResponse> list) {
        List<List<String>> out = new ArrayList<>();
        if (list != null) {
            for (EmployeeProductionHistoryResponse x : list) {
                out.add(List.of(x.getProductionDate() == null ? "-" : x.getProductionDate().format(DAY),
                        nz(x.getProductName()), support.num(x.getQuantity()), support.money(x.getRate()), support.money(x.getEarnings())));
            }
        }
        return out;
    }

    private String emptyRow(int cols) {
        return "<tr><td colspan=\"" + cols + "\">No data for this period</td></tr>";
    }

    // Average piece-rate = total payment / total pieces (0 when no pieces).
    private BigDecimal avgRate(BigDecimal earnings, Number qty) {
        long pieces = qty == null ? 0L : qty.longValue();
        if (pieces <= 0 || earnings == null) return BigDecimal.ZERO;
        return earnings.divide(BigDecimal.valueOf(pieces), 2, java.math.RoundingMode.HALF_UP);
    }

    private long asLong(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private BigDecimal asBig(Object o) {
        if (o == null) return BigDecimal.ZERO;
        return o instanceof BigDecimal b ? b : new BigDecimal(o.toString());
    }

    private String str(Object o) {
        return o == null ? "-" : o.toString();
    }

    private String nz(String s) {
        return s == null ? "-" : s;
    }
}
