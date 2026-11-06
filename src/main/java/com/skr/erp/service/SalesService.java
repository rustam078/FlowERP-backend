package com.skr.erp.service;

import com.skr.erp.common.constants.InventoryBatchStatus;
import com.skr.erp.common.constants.InventoryTransactionType;
import com.skr.erp.common.constants.PaymentMode;
import com.skr.erp.common.constants.PaymentStatus;
import com.skr.erp.common.response.PageResponse;
import com.skr.erp.dto.request.CreateSaleOrderItemRequest;
import com.skr.erp.dto.request.CreateSaleOrderRequest;
import com.skr.erp.dto.response.*;
import com.skr.erp.entity.*;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.exception.ReconciliationRequiredException;
import com.skr.erp.pdf.HtmlToPdfGenerator;
import com.skr.erp.qr.QrUnitService;
import com.skr.erp.repository.*;
import com.skr.erp.specification.SalesSpecification;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final QrUnitService qrUnitService;
    private final InventoryService inventoryService;
    private final CustomerPaymentRepository customerPaymentRepository;

    private static final String SALES_INVOICE_TEMPLATE_KEY = "SALES_INVOICE_TEMPLATE";
    private static final String SALES_GST_ENABLED_KEY = "SALES_GST_ENABLED";
    private static final String DEFAULT_GST_PERCENT_KEY = "DEFAULT_GST_PERCENT";

    /**
     * GST on the net amount (subtotal - discount) when SALES_GST_ENABLED is on;
     * zero otherwise. The percent is read live from DEFAULT_GST_PERCENT.
     */
    private BigDecimal computeGst(BigDecimal net) {
        boolean enabled = systemSettingRepository.findBySettingKey(SALES_GST_ENABLED_KEY)
                .map(s -> "true".equalsIgnoreCase(s.getSettingValue()))
                .orElse(false);
        if (!enabled || net.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal percent = systemSettingRepository.findBySettingKey(DEFAULT_GST_PERCENT_KEY)
                .map(s -> {
                    try {
                        return new BigDecimal(s.getSettingValue());
                    } catch (NumberFormatException e) {
                        return BigDecimal.ZERO;
                    }
                })
                .orElse(BigDecimal.ZERO);
        return net.multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }


    @Transactional
    public CreateSaleOrderResponse createSale(CreateSaleOrderRequest request)
            throws BadRequestException {

        // ==========================
        // Customer
        // ==========================

        Customer customer = customerRepository
                .findByMobile(request.getCustomerMobile())
                .orElseGet(() -> customerRepository.save(
                        Customer.builder()
                                .name(request.getCustomerName())
                                .mobile(request.getCustomerMobile())
                                .email(request.getCustomerEmail())
                                .build()
                ));

        // Optional customer update
        customer.setName(request.getCustomerName());
        customer.setEmail(request.getCustomerEmail());

        // ==========================
        // Validate duplicate products
        // ==========================

        // A line is unique per (product + batch). Same product from two different
        // scanned batches is allowed (two lines); the same product+batch twice is not.
        Set<String> lineKeys = new HashSet<>();

        for (CreateSaleOrderItemRequest item : request.getItems()) {

            String lineKey = item.getProductId() + "|" + (item.getBatchNumber() == null ? "" : item.getBatchNumber());
            if (!lineKeys.add(lineKey)) {
                throw new BadRequestException("Duplicate product selected.");
            }

            if (item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Quantity must be greater than zero.");
            }

            // Scanned lines carry a frozen price per unit and one serial per unit.
            if (item.getBatchNumber() != null) {
                if (item.getSellingPrice() == null || item.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {
                    throw new BadRequestException("Selling price is required for scanned items.");
                }
                int serialCount = item.getSerials() == null ? 0 : item.getSerials().size();
                if (serialCount != item.getQuantity().intValueExact()) {
                    throw new BadRequestException(
                            "Scanned line for batch " + item.getBatchNumber()
                                    + " must have one serial per unit.");
                }
            }
        }

        // ==========================
        // Reconciliation pre-check (scanned lines)
        // ==========================
        // Validate every scanned QR up front (unknown / wrong-batch / already SOLD-VOID fail hard —
        // NO reconciliation for used tags). Then, batch-wise, find batches whose system stock is
        // short of the scanned quantity (FIFO already consumed it). Any such batch not yet approved
        // by the user aborts with ReconciliationRequiredException so the UI can ask once per batch.
        Set<String> approvedBatches = request.getReconcileBatches() != null
                ? new HashSet<>(request.getReconcileBatches())
                : Collections.emptySet();
        List<ReconcileBatchInfo> needsReconcile = new ArrayList<>();
        for (CreateSaleOrderItemRequest item : request.getItems()) {
            if (item.getBatchNumber() == null) continue;
            InventoryBatch batch = inventoryBatchRepository.findByBatchNumber(item.getBatchNumber())
                    .orElseThrow(() -> new BusinessException("Batch " + item.getBatchNumber() + " not found."));
            qrUnitService.validateUnitsForSale(item.getBatchNumber(), item.getSerials());
            BigDecimal available = batch.getQuantityAvailable();
            if (available.compareTo(item.getQuantity()) < 0 && !approvedBatches.contains(item.getBatchNumber())) {
                needsReconcile.add(ReconcileBatchInfo.builder()
                        .batchNumber(item.getBatchNumber())
                        .productName(batch.getProduct() != null ? batch.getProduct().getName() : "")
                        .available(available)
                        .requested(item.getQuantity())
                        .deficit(item.getQuantity().subtract(available))
                        .build());
            }
        }
        if (!needsReconcile.isEmpty()) {
            throw new ReconciliationRequiredException(needsReconcile);
        }

        // ==========================
        // Create Order
        // ==========================

        SalesOrder order = SalesOrder.builder()
                .invoiceNo(nextInvoiceNumber())
                .customerName(customer.getName())
                .customerMobile(customer.getMobile())
                .customerEmail(customer.getEmail())
                .paymentMode(request.getPaymentMode())
                .paymentProvider(request.getPaymentProvider())
                .paymentStatus(PaymentStatus.DUE) // placeholder; final status set after payment allocation
                .tax(BigDecimal.ZERO)
                .subtotal(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .grandTotal(BigDecimal.ZERO)
                .amountPaid(BigDecimal.ZERO)
                .amountReceived(BigDecimal.ZERO)
                .build();

        salesOrderRepository.save(order);

        // Plan every line first (FIFO batch pricing for typed, frozen price for scanned) — no writes yet.
        List<LinePlan> plans = new ArrayList<>();
        for (CreateSaleOrderItemRequest item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException("Product not found."));
            plans.add(item.getBatchNumber() != null
                    ? planScannedLine(product, item)
                    : planTypedLine(product, item));
        }

        BigDecimal subtotal = plans.stream()
                .map(p -> p.gross).reduce(BigDecimal.ZERO, BigDecimal::add);

        // One order-level discount, clamped to the subtotal and spread across lines by gross.
        BigDecimal orderDiscount = nz(request.getDiscount())
                .max(BigDecimal.ZERO).min(subtotal).setScale(2, RoundingMode.HALF_UP);
        allocateDiscount(plans, orderDiscount);

        // Persist each line: deduct stock and save the order item with its price + allocated discount.
        for (LinePlan plan : plans) {
            persistLine(order, plan);
        }

        // Net amount after discount, then optional GST on top (controlled by settings).
        BigDecimal net = subtotal.subtract(orderDiscount);
        BigDecimal tax = computeGst(net);

        // Round the grand total to the nearest whole unit (retail round-off).
        BigDecimal grandTotal = net.add(tax).setScale(0, RoundingMode.HALF_UP);

        order.setSubtotal(subtotal);
        order.setDiscount(orderDiscount);
        order.setTax(tax);
        order.setGrandTotal(grandTotal);
        salesOrderRepository.save(order);

        // Apply the customer's payment (oldest dues first, then this sale); the rest stays due.
        allocateReceivedPayment(order, request);

        return CreateSaleOrderResponse.builder()
                .saleOrderId(order.getId())
                .invoiceNumber(order.getInvoiceNo())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .grandTotal(order.getGrandTotal())
                .amountPaid(order.getAmountPaid())
                .amountDue(dueOf(order))
                .paymentStatus(order.getPaymentStatus())
                .build();
    }

    // Apply the received amount to THIS sale first, then older dues; store what was received here.
    private void allocateReceivedPayment(SalesOrder order, CreateSaleOrderRequest request) {
        if (request.getAmountReceived() == null) {
            order.setAmountPaid(order.getGrandTotal()); // no credit intent → treat as fully paid
            order.setAmountReceived(order.getGrandTotal());
            order.setPaymentStatus(PaymentStatus.PAID);
            salesOrderRepository.save(order);
            return;
        }
        BigDecimal received = request.getAmountReceived();
        if (received.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Amount received cannot be negative.");
        }
        order.setAmountReceived(received);
        // Nothing paid now → it is a pure credit sale, so the mode is CREDIT (no real payment).
        if (received.compareTo(BigDecimal.ZERO) == 0) {
            order.setPaymentMode(PaymentMode.CREDIT);
        }
        // Pay THIS sale first, then the older dues oldest-first.
        List<SalesOrder> ledger = new ArrayList<>();
        ledger.add(order);
        for (SalesOrder s : salesOrderRepository.findOutstandingByMobile(order.getCustomerMobile())) {
            if (!s.getId().equals(order.getId())) ledger.add(s);
        }
        BigDecimal totalDue = ledger.stream().map(this::dueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (received.compareTo(totalDue) > 0) {
            throw new BusinessException("Amount received exceeds the total due.");
        }
        PaymentContext ctx = new PaymentContext(request.getPaymentMode(), UUID.randomUUID(), order.getInvoiceNo());
        BigDecimal remaining = received;
        for (SalesOrder sale : ledger) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            remaining = payOneSale(sale, remaining, ctx);
        }
        order.setPaymentStatus(statusOf(order)); // ensure a status even if payment didn't reach it
        salesOrderRepository.save(order);
    }

    // Mode, group id and the checkout invoice no shared across one payment event.
    private record PaymentContext(PaymentMode mode, UUID groupId, String sourceInvoiceNo) {}

    // Pay one sale from the running amount; records the payment and returns the leftover.
    private BigDecimal payOneSale(SalesOrder sale, BigDecimal available, PaymentContext ctx) {
        BigDecimal due = dueOf(sale);
        if (due.compareTo(BigDecimal.ZERO) <= 0) return available;
        BigDecimal pay = available.min(due);
        CustomerPayment payment = new CustomerPayment();
        payment.setSalesOrder(sale);
        payment.setPaymentDate(LocalDate.now());
        payment.setMode(ctx.mode() != null ? ctx.mode() : PaymentMode.CASH);
        payment.setAmount(pay);
        payment.setPaymentGroupId(ctx.groupId());
        // Stamp the checkout invoice only when this payment cleared a different (older) invoice.
        if (!sale.getInvoiceNo().equals(ctx.sourceInvoiceNo())) {
            payment.setReferenceInvoiceNo(ctx.sourceInvoiceNo());
        }
        customerPaymentRepository.save(payment);
        sale.setAmountPaid(nz(sale.getAmountPaid()).add(pay));
        sale.setPaymentStatus(statusOf(sale));
        salesOrderRepository.save(sale);
        return available.subtract(pay);
    }

    private BigDecimal dueOf(SalesOrder sale) {
        return sale.getGrandTotal().subtract(nz(sale.getAmountPaid()));
    }

    // Of the amount received at this sale's checkout, how much went to older invoices.
    private BigDecimal prevDuesOf(SalesOrder order) {
        return nz(order.getAmountReceived()).subtract(order.getGrandTotal()).max(BigDecimal.ZERO);
    }

    // The customer's remaining balance across every sale (used as "balance due").
    private BigDecimal customerBalance(String mobile) {
        return salesOrderRepository.findOutstandingByMobile(mobile).stream()
                .map(this::dueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Two states only: fully cleared is PAID, anything still owed is DUE.
    private PaymentStatus statusOf(SalesOrder sale) {
        return nz(sale.getAmountPaid()).compareTo(sale.getGrandTotal()) >= 0
                ? PaymentStatus.PAID : PaymentStatus.DUE;
    }

    // A customer's unpaid balance across all their sales (for the sale form).
    @Transactional(readOnly = true)
    public CustomerOutstandingResponse getCustomerOutstanding(String mobile) {
        List<SalesOrder> unpaid = salesOrderRepository.findOutstandingByMobile(mobile);
        BigDecimal total = unpaid.stream().map(this::dueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
        return CustomerOutstandingResponse.builder()
                .totalOutstanding(total)
                .unpaidCount(unpaid.size())
                .build();
    }

    // A customer's payments grouped by payment event, each split across the invoices it cleared.
    @Transactional(readOnly = true)
    public List<CustomerPaymentGroupResponse> getCustomerPaymentHistory(String mobile) {
        Map<UUID, List<CustomerPayment>> groups = new LinkedHashMap<>();
        for (CustomerPayment p : customerPaymentRepository.findByCustomerMobile(mobile)) {
            UUID key = p.getPaymentGroupId() != null ? p.getPaymentGroupId() : p.getId();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }
        return groups.values().stream().map(this::toPaymentGroup).toList();
    }

    private CustomerPaymentGroupResponse toPaymentGroup(List<CustomerPayment> group) {
        CustomerPayment first = group.get(0);
        BigDecimal total = group.stream().map(CustomerPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CustomerPaymentGroupResponse.Allocation> allocations = group.stream()
                .map(p -> CustomerPaymentGroupResponse.Allocation.builder()
                        .invoiceNumber(p.getSalesOrder().getInvoiceNo())
                        .amount(p.getAmount()).build())
                .toList();
        return CustomerPaymentGroupResponse.builder()
                .paymentDate(first.getPaymentDate()).mode(first.getMode())
                .totalAmount(total).allocations(allocations).build();
    }

    // A customer's recent sales (newest first) for the history tab.
    @Transactional(readOnly = true)
    public PageResponse<CustomerSaleResponse> getCustomerHistory(String mobile, Pageable pageable) {
        Page<SalesOrder> page = salesOrderRepository.findByCustomerMobileOrderByCreatedAtDesc(mobile, pageable);
        List<CustomerSaleResponse> content = page.getContent().stream()
                .map(this::toCustomerSaleResponse).toList();
        return PageResponse.<CustomerSaleResponse>builder()
                .content(content).page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .last(page.isLast()).build();
    }

    @Transactional(readOnly = true)
    public PageResponse<SalesListResponse> getSales(
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            PaymentMode paymentMode,
            PaymentStatus paymentStatus,
            Pageable pageable) {

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException("fromDate cannot be after toDate");
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<SalesOrder> salesPage = salesOrderRepository.findAll(
                SalesSpecification.filter(
                        search, fromDate, toDate, paymentMode, paymentStatus),
                sortedPageable
        );

        List<SalesListResponse> content = salesPage.getContent().stream()
                .map(this::toSalesListResponse)
                .toList();

        return PageResponse.<SalesListResponse>builder()
                .content(content)
                .page(salesPage.getNumber())
                .size(salesPage.getSize())
                .totalElements(salesPage.getTotalElements())
                .totalPages(salesPage.getTotalPages())
                .last(salesPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse getSalesSummary(
            String search, LocalDate fromDate, LocalDate toDate,
            PaymentMode paymentMode, PaymentStatus paymentStatus) {

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException("fromDate cannot be after toDate");
        }

        // Same filter as the list, but totals are aggregated in SQL (no loading all rows into memory).
        String pattern = (search != null && !search.isBlank()) ? "%" + search.toLowerCase() + "%" : null;
        java.time.LocalDateTime start = fromDate != null ? fromDate.atStartOfDay() : null;
        java.time.LocalDateTime end = toDate != null ? toDate.plusDays(1).atStartOfDay() : null;
        Object[] t = salesOrderRepository.summaryFiltered(pattern, start, end, paymentMode, paymentStatus).get(0);

        long count = ((Number) t[0]).longValue();
        BigDecimal revenue = (BigDecimal) t[1];
        BigDecimal discount = (BigDecimal) t[2];
        BigDecimal received = (BigDecimal) t[3]; // actual cash tendered (may exceed billed, clears old dues)
        BigDecimal due = (BigDecimal) t[4];      // per-invoice due, floored at 0 in SQL
        return SalesSummaryResponse.builder()
                .count(count)
                .revenue(revenue)
                .discount(discount)
                .received(received)
                .due(due)
                .build();
    }

    @Transactional(readOnly = true)
    public SalesDetailsResponse getSaleDetails(UUID saleId) {
        SalesOrder order = getSalesOrderOrThrow(saleId);
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderIdWithProduct(saleId);
        return toSalesDetailsResponse(order, items);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID saleId) {
        SalesOrder order = getSalesOrderOrThrow(saleId);
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderIdWithProduct(saleId);
        return toInvoiceResponse(order, items);
    }

    @Transactional(readOnly = true)
    public byte[] exportInvoicePdf(UUID saleId) {
        InvoiceResponse inv = getInvoice(saleId);
        String template = systemSettingRepository
                .findBySettingKey(SALES_INVOICE_TEMPLATE_KEY)
                .map(SystemSetting::getSettingValue)
                .orElseThrow(() -> new BusinessException(
                        "Invoice template not configured: " + SALES_INVOICE_TEMPLATE_KEY));
        String html = fillInvoiceTemplate(template, inv);
        return HtmlToPdfGenerator.render(html);
    }

    private String fillInvoiceTemplate(String template, InvoiceResponse inv) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        String cur = setting("CURRENCY_SYMBOL", "Rs.");

        StringBuilder rows = new StringBuilder();
        if (inv.getItems() != null) {
            for (InvoiceItemResponse it : inv.getItems()) {
                rows.append("<tr>")
                        .append("<td>").append(esc(it.getProductName())).append("</td>")
                        .append("<td class=\"right\">").append(fmtQty(it.getQuantity())).append("</td>")
                        .append("<td class=\"right\">").append(cur).append(" ").append(money(nf, it.getSellingPrice())).append("</td>")
                        .append("<td class=\"right\">").append(cur).append(" ").append(money(nf, it.getDiscount())).append("</td>")
                        .append("<td class=\"right\">").append(cur).append(" ").append(money(nf, it.getLineTotal())).append("</td>")
                        .append("</tr>");
            }
        }

        String status = inv.getPaymentStatus() != null ? inv.getPaymentStatus().name() : "-";
        String statusClass = "PAID".equals(status) ? "badge-paid" : "badge-other";
        String generatedOn = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a"));
        String email = (inv.getCustomerEmail() == null || inv.getCustomerEmail().isBlank()) ? "" : inv.getCustomerEmail();
        String provider = (inv.getPaymentProvider() == null || inv.getPaymentProvider().isBlank()) ? "" : inv.getPaymentProvider();

        return template
                .replace("{{paymentRows}}", paymentRows(nf, inv, cur))
                .replace("{{cur}}", cur)
                .replace("{{companyName}}", esc(setting("COMPANY_NAME", "SKR Garment")))
                .replace("{{companyAddress}}", esc(setting("COMPANY_ADDRESS", "")))
                .replace("{{companyContact}}", esc(setting("COMPANY_CONTACT", "")))
                .replace("{{companyGstin}}", esc(setting("COMPANY_GSTIN", "-")))
                .replace("{{invoiceNumber}}", esc(inv.getInvoiceNumber()))
                .replace("{{date}}", inv.getDate() != null
                        ? inv.getDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")) : "-")
                .replace("{{customerName}}", esc(inv.getCustomerName()))
                .replace("{{customerMobile}}", esc(inv.getCustomerMobile()))
                .replace("{{customerEmail}}", esc(email))
                .replace("{{paymentMode}}", inv.getPaymentMode() != null ? inv.getPaymentMode().name() : "-")
                .replace("{{statusClass}}", statusClass)
                .replace("{{paymentStatus}}", status)
                .replace("{{paymentProvider}}", esc(provider))
                .replace("{{itemRows}}", rows.toString())
                .replace("{{subtotal}}", money(nf, inv.getSubtotal()))
                .replace("{{discount}}", money(nf, inv.getDiscount()))
                .replace("{{tax}}", money(nf, inv.getTax()))
                .replace("{{grandTotal}}", money(nf, inv.getGrandTotal()))
                .replace("{{generatedOn}}", generatedOn);
    }

    // Invoice payment rows: amount received, any part applied to older dues, and balance due.
    private String paymentRows(NumberFormat nf, InvoiceResponse inv, String cur) {
        StringBuilder sb = new StringBuilder();
        sb.append("<tr><td>Amount Received</td><td class=\"right\">").append(cur).append(" ")
                .append(money(nf, inv.getAmountReceived())).append("</td></tr>");
        if (inv.getPaidToPreviousDues() != null && inv.getPaidToPreviousDues().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("<tr><td>Towards Previous Dues</td><td class=\"right\">").append(cur).append(" ")
                    .append(money(nf, inv.getPaidToPreviousDues())).append("</td></tr>");
            previousDueRows(nf, inv.getPreviousDuesAllocation(), sb, cur);
        }
        if (inv.getBalanceDue() != null && inv.getBalanceDue().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("<tr class=\"grand\"><td>Balance Due</td><td class=\"right\">").append(cur).append(" ")
                    .append(money(nf, inv.getBalanceDue())).append("</td></tr>");
        }
        clearanceRows(nf, inv.getClearances(), sb, cur);
        return sb.toString();
    }

    // Sub-lines under "Towards Previous Dues": each older invoice cleared + amount (sums to the total).
    private void previousDueRows(NumberFormat nf, List<SalesDetailsResponse.PreviousDue> dues, StringBuilder sb, String cur) {
        if (dues == null || dues.isEmpty()) return;
        for (SalesDetailsResponse.PreviousDue d : dues) {
            sb.append("<tr><td style=\"padding-left:16px;color:#666666;font-size:11px;\">&#8226; ")
                    .append(esc(d.getInvoiceNumber())).append("</td><td class=\"right\" style=\"color:#666666;font-size:11px;\">")
                    .append(cur).append(" ").append(money(nf, d.getAmount())).append("</td></tr>");
        }
    }

    // Lines showing this invoice's due later cleared from another invoice's checkout.
    private void clearanceRows(NumberFormat nf, List<SalesDetailsResponse.Clearance> clearances, StringBuilder sb, String cur) {
        if (clearances == null || clearances.isEmpty()) return;
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        for (SalesDetailsResponse.Clearance c : clearances) {
            sb.append("<tr><td>Due cleared ").append(c.getDate().format(df))
                    .append(" (").append(esc(c.getMode().name())).append(", Ref: ").append(esc(c.getReferenceInvoiceNo()))
                    .append(")</td><td class=\"right\">").append(cur).append(" ").append(money(nf, c.getAmount())).append("</td></tr>");
        }
    }

    private static String money(NumberFormat nf, BigDecimal b) {
        return b != null ? nf.format(b) : "0";
    }

    private static String fmtQty(BigDecimal q) {
        return q != null ? q.stripTrailingZeros().toPlainString() : "0";
    }

    /** Null-safe BigDecimal — treats a missing value as zero. */
    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /** Live system-setting value, or the fallback when missing/blank. */
    private String setting(String key, String fallback) {
        return systemSettingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .filter(s -> s != null && !s.isBlank())
                .orElse(fallback);
    }

    @Transactional(readOnly = true)
    public SalesDashboardResponse getDashboard() {

        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        SalesSummaryProjection todayStats =
                salesOrderRepository.countAndRevenueBetween(
                        today.atStartOfDay(),
                        today.plusDays(1).atStartOfDay()
                );

        SalesSummaryProjection monthStats =
                salesOrderRepository.countAndRevenueBetween(
                        firstDayOfMonth.atStartOfDay(),
                        firstDayOfMonth.plusMonths(1).atStartOfDay()
                );

        SalesSummaryProjection totalStats =
                salesOrderRepository.countAndTotalRevenue();

        BigDecimal totalProfit =
                Optional.ofNullable(
                                salesOrderItemRepository.calculateTotalProfit())
                        .orElse(BigDecimal.ZERO);

        return SalesDashboardResponse.builder()
                .todaySales(todayStats.getTotalSales())
                .todayRevenue(todayStats.getTotalRevenue())
                .monthSales(monthStats.getTotalSales())
                .monthRevenue(monthStats.getTotalRevenue())
                .totalSales(totalStats.getTotalSales())
                .totalRevenue(totalStats.getTotalRevenue())
                .totalProfit(totalProfit)
                .build();
    }
    @Transactional(readOnly = true)
    public List<TopSellingProductResponse> getTopProducts() {
        return salesOrderItemRepository
                .findTopSellingProducts(PageRequest.of(0, 10))
                .stream()
                .map(row -> TopSellingProductResponse.builder()
                        .productId((UUID) row[0])
                        .productName((String) row[1])
                        .totalQuantitySold((BigDecimal) row[2])
                        .totalRevenue((BigDecimal) row[3])
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecentSaleResponse> getRecentSales() {
        return salesOrderRepository
                .findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .map(this::toRecentSaleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerSaleResponse> getCustomerSales(
            UUID customerId,
            Pageable pageable) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("Customer not found."));

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<SalesOrder> salesPage = salesOrderRepository
                .findByCustomerMobileOrderByCreatedAtDesc(
                        customer.getMobile(),
                        sortedPageable
                );

        List<CustomerSaleResponse> content = salesPage.getContent().stream()
                .map(this::toCustomerSaleResponse)
                .toList();

        return PageResponse.<CustomerSaleResponse>builder()
                .content(content)
                .page(salesPage.getNumber())
                .size(salesPage.getSize())
                .totalElements(salesPage.getTotalElements())
                .totalPages(salesPage.getTotalPages())
                .last(salesPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSaleHistoryResponse> getProductSales(
            UUID productId,
            Pageable pageable) {

        productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Product not found."));

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<SalesOrderItem> itemsPage = salesOrderItemRepository
                .findByProductId(productId, sortedPageable);

        List<ProductSaleHistoryResponse> content = itemsPage.getContent().stream()
                .map(this::toProductSaleHistoryResponse)
                .toList();

        return PageResponse.<ProductSaleHistoryResponse>builder()
                .content(content)
                .page(itemsPage.getNumber())
                .size(itemsPage.getSize())
                .totalElements(itemsPage.getTotalElements())
                .totalPages(itemsPage.getTotalPages())
                .last(itemsPage.isLast())
                .build();
    }

    private SalesOrder getSalesOrderOrThrow(UUID saleId) {
        return salesOrderRepository.findById(saleId)
                .orElseThrow(() -> new BusinessException("Sale not found."));
    }

    private SalesListResponse toSalesListResponse(SalesOrder order) {
        return SalesListResponse.builder()
                .saleId(order.getId())
                .invoiceNo(order.getInvoiceNo())
                .customerName(order.getCustomerName())
                .customerMobile(order.getCustomerMobile())
                .grandTotal(order.getGrandTotal())
                .amountPaid(nz(order.getAmountPaid()))
                .amountDue(dueOf(order))
                .amountReceived(nz(order.getAmountReceived()))
                .paymentMode(order.getPaymentMode())
                .paymentStatus(order.getPaymentStatus())
                .saleDate(order.getCreatedAt().toLocalDate())
                .build();
    }

    private SalesDetailsResponse toSalesDetailsResponse(
            SalesOrder order,
            List<SalesOrderItem> items) {

        List<SalesItemResponse> itemResponses = items.stream()
                .map(this::toSalesItemResponse)
                .toList();

        BigDecimal totalProfit = itemResponses.stream()
                .map(SalesItemResponse::getProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SalesDetailsResponse.builder()
                .saleId(order.getId())
                .invoiceNo(order.getInvoiceNo())
                .saleDate(order.getCreatedAt().toLocalDate())
                .customerName(order.getCustomerName())
                .customerMobile(order.getCustomerMobile())
                .customerEmail(order.getCustomerEmail())
                .paymentMode(order.getPaymentMode())
                .paymentProvider(order.getPaymentProvider())
                .paymentStatus(order.getPaymentStatus())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .tax(order.getTax())
                .grandTotal(order.getGrandTotal())
                .amountPaid(nz(order.getAmountPaid()))
                .amountDue(dueOf(order))
                .amountReceived(nz(order.getAmountReceived()))
                .paidToPreviousDues(prevDuesOf(order))
                .customerBalanceDue(customerBalance(order.getCustomerMobile()))
                .totalProfit(totalProfit)
                .items(itemResponses)
                .clearances(clearancesOf(order.getId()))
                .previousDuesAllocation(previousDuesAllocationOf(order.getInvoiceNo()))
                .build();
    }

    // Payments that cleared this invoice from a later sale's checkout (newest first).
    private List<SalesDetailsResponse.Clearance> clearancesOf(UUID saleId) {
        return customerPaymentRepository.findBySalesOrderIdOrderByCreatedAtDesc(saleId).stream()
                .filter(p -> p.getReferenceInvoiceNo() != null)
                .map(p -> SalesDetailsResponse.Clearance.builder()
                        .date(p.getPaymentDate()).mode(p.getMode())
                        .amount(p.getAmount()).referenceInvoiceNo(p.getReferenceInvoiceNo()).build())
                .toList();
    }

    // Older invoices that this checkout's payment cleared (invoice no + amount) — sums to paidToPreviousDues.
    private List<SalesDetailsResponse.PreviousDue> previousDuesAllocationOf(String invoiceNo) {
        return customerPaymentRepository.findByReferenceInvoiceNo(invoiceNo).stream()
                .map(p -> SalesDetailsResponse.PreviousDue.builder()
                        .invoiceNumber(p.getSalesOrder().getInvoiceNo())
                        .amount(p.getAmount()).build())
                .toList();
    }

    private SalesItemResponse toSalesItemResponse(SalesOrderItem item) {
        return SalesItemResponse.builder()
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .sellingPrice(item.getSellingPrice())
                .unitCost(item.getUnitPrice())
                .discount(item.getDiscount())
                .lineTotal(item.getLineTotal())
                .profit(calculateItemProfit(item))
                .build();
    }

    private InvoiceResponse toInvoiceResponse(
            SalesOrder order,
            List<SalesOrderItem> items) {

        List<InvoiceItemResponse> invoiceItems = items.stream()
                .map(item -> InvoiceItemResponse.builder()
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .sellingPrice(item.getSellingPrice())
                        .discount(item.getDiscount())
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();

        return InvoiceResponse.builder()
                .invoiceNumber(order.getInvoiceNo())
                .date(order.getCreatedAt().toLocalDate())
                .customerName(order.getCustomerName())
                .customerMobile(order.getCustomerMobile())
                .customerEmail(order.getCustomerEmail())
                .paymentMode(order.getPaymentMode())
                .paymentProvider(order.getPaymentProvider())
                .paymentStatus(order.getPaymentStatus())
                .items(invoiceItems)
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .tax(order.getTax())
                .grandTotal(order.getGrandTotal())
                .amountReceived(nz(order.getAmountReceived()))
                .paidToPreviousDues(prevDuesOf(order))
                .balanceDue(dueOf(order)) // this invoice's own due only (not the customer's overall balance)
                .clearances(clearancesOf(order.getId()))
                .previousDuesAllocation(previousDuesAllocationOf(order.getInvoiceNo()))
                .build();
    }

    private RecentSaleResponse toRecentSaleResponse(SalesOrder order) {
        return RecentSaleResponse.builder()
                .invoiceNo(order.getInvoiceNo())
                .customerName(order.getCustomerName())
                .grandTotal(order.getGrandTotal())
                .paymentStatus(order.getPaymentStatus())
                .saleDate(order.getCreatedAt().toLocalDate())
                .build();
    }

    private CustomerSaleResponse toCustomerSaleResponse(SalesOrder order) {
        return CustomerSaleResponse.builder()
                .invoiceNo(order.getInvoiceNo())
                .saleDate(order.getCreatedAt().toLocalDate())
                .grandTotal(order.getGrandTotal())
                .amountPaid(nz(order.getAmountPaid()))
                .amountDue(dueOf(order))
                .paymentStatus(order.getPaymentStatus())
                .build();
    }

    private ProductSaleHistoryResponse toProductSaleHistoryResponse(SalesOrderItem item) {
        SalesOrder order = item.getSalesOrder();

        return ProductSaleHistoryResponse.builder()
                .invoiceNo(order.getInvoiceNo())
                .saleDate(order.getCreatedAt().toLocalDate())
                .customerName(order.getCustomerName())
                .quantitySold(item.getQuantity())
                .sellingPrice(item.getSellingPrice())
                .profit(calculateItemProfit(item))
                .build();
    }

    private BigDecimal calculateItemProfit(SalesOrderItem item) {
        BigDecimal totalCost = item.getUnitPrice().multiply(item.getQuantity());
        return item.getLineTotal().subtract(totalCost);
    }

    // One FIFO draw: units taken from a batch, at its cost and effective selling price.
    private record BatchDraw(InventoryBatch batch, BigDecimal qty, BigDecimal unitCost, BigDecimal price) {}

    // A planned line before persistence; the allocated discount is filled in later.
    private static final class LinePlan {
        private final Product product;
        private final CreateSaleOrderItemRequest item;
        private final List<BatchDraw> draws;
        private final BigDecimal gross;
        private final BigDecimal avgPrice;
        private final BigDecimal totalCost;
        private BigDecimal discount = BigDecimal.ZERO;

        private LinePlan(Product product, CreateSaleOrderItemRequest item, List<BatchDraw> draws,
                         BigDecimal gross, BigDecimal avgPrice, BigDecimal totalCost) {
            this.product = product;
            this.item = item;
            this.draws = draws;
            this.gross = gross;
            this.avgPrice = avgPrice;
            this.totalCost = totalCost;
        }
    }

    // Plan a typed line: FIFO across available batches, each drawn at its own selling price.
    private LinePlan planTypedLine(Product product, CreateSaleOrderItemRequest item) {
        List<InventoryBatch> batches = inventoryBatchRepository.findAvailableBatchesForSale(product.getId());
        if (batches.isEmpty()) {
            throw new BusinessException(product.getName() + " is out of stock.");
        }
        BigDecimal totalAvailable = batches.stream()
                .map(InventoryBatch::getQuantityAvailable).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalAvailable.compareTo(item.getQuantity()) < 0) {
            throw new BusinessException(product.getName() + " has only " + totalAvailable + " available.");
        }
        return toLinePlan(product, item, drawFifo(product, item.getQuantity(), batches));
    }

    // Pull the requested quantity from FIFO batches, capturing each draw's price and cost.
    private List<BatchDraw> drawFifo(Product product, BigDecimal quantity, List<InventoryBatch> batches) {
        List<BatchDraw> draws = new ArrayList<>();
        BigDecimal remaining = quantity;
        for (InventoryBatch batch : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal available = batch.getQuantityAvailable();
            if (available.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal take = remaining.min(available);
            draws.add(new BatchDraw(batch, take, batch.getUnitCost(), batchPrice(product, batch)));
            remaining = remaining.subtract(take);
        }
        return draws;
    }

    // A batch's selling price: its own, else the product's current sale price (must exist).
    private BigDecimal batchPrice(Product product, InventoryBatch batch) {
        BigDecimal price = batch.getSellingPrice() != null
                ? batch.getSellingPrice() : inventoryService.getCurrentSalePrice(product.getId());
        if (price == null) {
            throw new BusinessException("Set a sale price for " + product.getName() + " before selling.");
        }
        return price;
    }

    // Fold draws into one line: exact gross, weighted-average price and total cost.
    private LinePlan toLinePlan(Product product, CreateSaleOrderItemRequest item, List<BatchDraw> draws) {
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        for (BatchDraw d : draws) {
            gross = gross.add(d.price().multiply(d.qty()));
            cost = cost.add(d.unitCost().multiply(d.qty()));
        }
        gross = gross.setScale(2, RoundingMode.HALF_UP);
        BigDecimal avg = gross.divide(item.getQuantity(), 2, RoundingMode.HALF_UP);
        return new LinePlan(product, item, draws, gross, avg, cost.setScale(2, RoundingMode.HALF_UP));
    }

    // Plan a scanned line: single batch, frozen printed price, cost from the batch.
    private LinePlan planScannedLine(Product product, CreateSaleOrderItemRequest item) {
        InventoryBatch batch = inventoryBatchRepository.findByBatchNumber(item.getBatchNumber())
                .orElseThrow(() -> new BusinessException("Batch " + item.getBatchNumber() + " not found."));
        BigDecimal gross = item.getSellingPrice().multiply(item.getQuantity()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cost = batch.getUnitCost().multiply(item.getQuantity()).setScale(2, RoundingMode.HALF_UP);
        return new LinePlan(product, item, List.of(), gross, item.getSellingPrice(), cost);
    }

    // Spread the order discount across lines proportional to gross; last line absorbs the rounding.
    private void allocateDiscount(List<LinePlan> plans, BigDecimal discount) {
        if (discount.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal totalGross = plans.stream().map(p -> p.gross).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalGross.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal allocated = BigDecimal.ZERO;
        LinePlan last = null;
        for (LinePlan plan : plans) {
            plan.discount = discount.multiply(plan.gross)
                    .divide(totalGross, 2, RoundingMode.HALF_UP).min(plan.gross);
            allocated = allocated.add(plan.discount);
            last = plan;
        }
        BigDecimal remainder = discount.subtract(allocated);
        if (last != null && remainder.signum() != 0) {
            last.discount = last.discount.add(remainder).max(BigDecimal.ZERO).min(last.gross);
        }
    }

    private void persistLine(SalesOrder order, LinePlan plan) {
        if (plan.item.getBatchNumber() != null) {
            persistScannedLine(order, plan);
        } else {
            persistTypedLine(order, plan);
        }
    }

    // Save a typed line and deduct each of its FIFO batch draws.
    private void persistTypedLine(SalesOrder order, LinePlan plan) {
        SalesOrderItem orderItem = SalesOrderItem.builder()
                .salesOrder(order)
                .product(plan.product)
                .quantity(plan.item.getQuantity())
                .discount(plan.discount)
                .sellingPrice(plan.avgPrice)
                .unitPrice(avgUnitCost(plan))
                .lineTotal(plan.gross.subtract(plan.discount).setScale(2, RoundingMode.HALF_UP))
                .build();
        orderItem = salesOrderItemRepository.save(orderItem);
        for (BatchDraw draw : plan.draws) {
            consumeBatch(orderItem, draw);
        }
    }

    // Weighted-average unit cost across the line's draws (0 when quantity is 0).
    private BigDecimal avgUnitCost(LinePlan plan) {
        BigDecimal qty = plan.item.getQuantity();
        if (qty.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return plan.totalCost.divide(qty, 2, RoundingMode.HALF_UP);
    }

    // Deduct one batch draw, mark it SOLD when emptied, and record the SALE transaction.
    private void consumeBatch(SalesOrderItem orderItem, BatchDraw draw) {
        InventoryBatch batch = draw.batch();
        BigDecimal remaining = batch.getQuantityAvailable().subtract(draw.qty());
        batch.setQuantityAvailable(remaining);
        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            batch.setStatus(InventoryBatchStatus.SOLD);
        }
        inventoryTransactionRepository.save(saleTx(orderItem, batch, InventoryTransactionType.SALE, draw.qty()));
    }

    // Save a scanned line (single batch, frozen price) and consume its serials.
    private void persistScannedLine(SalesOrder order, LinePlan plan) {
        CreateSaleOrderItemRequest item = plan.item;
        InventoryBatch batch = inventoryBatchRepository.findByBatchNumber(item.getBatchNumber())
                .orElseThrow(() -> new BusinessException("Batch " + item.getBatchNumber() + " not found."));
        if (!batch.getProduct().getId().equals(plan.product.getId())) {
            throw new BusinessException(
                    "Batch " + item.getBatchNumber() + " does not belong to " + plan.product.getName() + ".");
        }
        SalesOrderItem orderItem = SalesOrderItem.builder()
                .salesOrder(order).product(plan.product)
                .quantity(item.getQuantity()).discount(plan.discount)
                .unitPrice(batch.getUnitCost()).sellingPrice(item.getSellingPrice())
                .batchNumber(item.getBatchNumber())
                .lineTotal(plan.gross.subtract(plan.discount).setScale(2, RoundingMode.HALF_UP))
                .build();
        orderItem = salesOrderItemRepository.save(orderItem);
        reconcileAndConsume(orderItem, item, batch);
    }

    // Top up any FIFO-drained deficit (reconciliation pre-approved), mark units SOLD, deduct & record.
    private void reconcileAndConsume(SalesOrderItem orderItem, CreateSaleOrderItemRequest item, InventoryBatch batch) {
        SalesOrder order = orderItem.getSalesOrder();
        BigDecimal needed = item.getQuantity();
        BigDecimal available = batch.getQuantityAvailable();
        if (available.compareTo(needed) < 0) {
            BigDecimal deficit = needed.subtract(available);
            inventoryTransactionRepository.save(InventoryTransaction.builder()
                    .salesOrderItem(orderItem).batch(batch).product(orderItem.getProduct())
                    .transactionType(InventoryTransactionType.RECONCILIATION)
                    .quantity(deficit).unitCost(batch.getUnitCost())
                    .reason("Physical QR stock reconciliation").build());
            batch.setQuantityAvailable(available.add(deficit));
            available = batch.getQuantityAvailable();
        }
        qrUnitService.consumeForSale(item.getBatchNumber(), item.getSerials(), order.getId());
        BigDecimal remainingInBatch = available.subtract(needed);
        batch.setQuantityAvailable(remainingInBatch);
        batch.setStatus(remainingInBatch.compareTo(BigDecimal.ZERO) == 0
                ? InventoryBatchStatus.SOLD : InventoryBatchStatus.ACTIVE);
        inventoryTransactionRepository.save(saleTx(orderItem, batch, InventoryTransactionType.SALE, needed));
    }

    // Build an inventory transaction for a sold line against a batch.
    private InventoryTransaction saleTx(SalesOrderItem orderItem, InventoryBatch batch,
                                        InventoryTransactionType type, BigDecimal qty) {
        return InventoryTransaction.builder()
                .salesOrderItem(orderItem).batch(batch).product(orderItem.getProduct())
                .transactionType(type).quantity(qty).unitCost(batch.getUnitCost()).build();
    }

    private String nextInvoiceNumber() {
        long count = salesOrderRepository.count() + 1;
        return "INV-" + String.format("%06d", count);
    }


    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getCustomers(
            String mobile,
            Pageable pageable) {

        Page<Customer> customers;

        if (mobile != null && !mobile.isBlank()) {
            customers = customerRepository.findByMobileContainingIgnoreCase(
                    mobile,
                    pageable
            );
        } else {
            customers = customerRepository.findAll(pageable);
        }

        List<CustomerResponse> content = customers.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<CustomerResponse>builder()
                .content(content)
                .page(customers.getNumber())
                .size(customers.getSize())
                .totalElements(customers.getTotalElements())
                .totalPages(customers.getTotalPages())
                .last(customers.isLast())
                .build();
    }

    private CustomerResponse toResponse(Customer customer) {

        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .mobile(customer.getMobile())
                .email(customer.getEmail())
                .build();
    }
}
