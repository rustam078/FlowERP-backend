package com.skr.erp.service;

import com.skr.erp.common.constants.*;
import com.skr.erp.dto.request.AddPaymentRequest;
import com.skr.erp.dto.request.CreateInvestmentItemRequest;
import com.skr.erp.dto.request.CreateInvestmentRequest;
import com.skr.erp.dto.response.InvestmentDetailsResponse;
import com.skr.erp.dto.response.InvestmentItemResponse;
import com.skr.erp.dto.response.InvestmentPaymentResponse;
import com.skr.erp.dto.response.InvestmentResponse;
import com.skr.erp.dto.response.VendorPaymentGroupResponse;
import com.skr.erp.entity.*;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.pdf.HtmlToPdfGenerator;
import com.skr.erp.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
@Transactional
public class InvestmentService {

    private static final String PURCHASE_INVOICE_TEMPLATE_KEY = "PURCHASE_INVOICE_TEMPLATE";
    private static final DateTimeFormatter DTF_DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private final InvestmentRepository investmentRepository;
    private final InvestmentItemRepository investmentItemRepository;
    private final VendorRepository vendorRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final InvestmentPaymentRepository investmentPaymentRepository;
    private final SystemSettingRepository systemSettingRepository;

    private static String money(NumberFormat nf, BigDecimal b) {
        return b != null ? nf.format(b) : nf.format(BigDecimal.ZERO);
    }

    private static String fmtQty(BigDecimal q) {
        return q != null ? q.stripTrailingZeros().toPlainString() : "0";
    }

    private static String titleCase(String s) {
        if (s == null || s.isBlank()) return "-";
        String[] parts = s.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public InvestmentResponse create(CreateInvestmentRequest request) {
        validateRequest(request);
        Investment investment = new Investment();

        if (request.getInvoiceNumber() != null && !request.getInvoiceNumber().isBlank()) {
            investment.setInvoiceNumber(request.getInvoiceNumber());

        } else {
            investment.setInvoiceNumber(generateInvoiceNumber());
        }
        investment.setInvestmentType(request.getInvestmentType());
        investment.setPurchaseDate(request.getPurchaseDate());
        investment.setGstAmount(request.getGstAmount());
        investment.setDiscountAmount(request.getDiscountAmount());
        investment.setOtherCharge(request.getOtherCharge());
        if (request.getVendorId() != null && request.getInvestmentType() != InvestmentType.OVERHEAD) {
            Vendor vendor = vendorRepository.findById(request.getVendorId()).orElseThrow(() -> new BusinessException("Vendor not found"));
            investment.setVendor(vendor);
        }

        List<InvestmentItem> items = buildInvestmentItems(investment, request.getItems());
        investment.setItems(items);
        calculateTotals(investment);
        investment = investmentRepository.save(investment);

        createInventoryBatches(investment);
        recordCreationPayment(investment, request);
        return toResponse(investment);
    }

    // Optional payment captured on the Create Investment form.
    // This invoice is paid first, then the vendor's older dues oldest-first; overhead pays only itself.
    private void recordCreationPayment(Investment investment, CreateInvestmentRequest request) {
        PaymentMode mode = request.getPaymentMode() != null ? request.getPaymentMode() : PaymentMode.CASH;
        LocalDate date = request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now();
        String ref = request.getPaymentReference();

        if (investment.getVendor() == null) {
            BigDecimal amount = Boolean.TRUE.equals(request.getFullPayment())
                    ? investment.getGrandTotal() : request.getPaymentAmount();
            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                recordPayment(investment, new PaymentEntry(date, mode, amount, UUID.randomUUID(), ref));
            }
            return;
        }

        // This new invoice first, then the vendor's other dues oldest-first.
        List<Investment> ledger = new ArrayList<>();
        ledger.add(investment);
        for (Investment inv : investmentRepository.findByVendorIdOrderByPurchaseDateAscCreatedAtAsc(investment.getVendor().getId())) {
            if (!inv.getId().equals(investment.getId())) ledger.add(inv);
        }
        BigDecimal amount = Boolean.TRUE.equals(request.getFullPayment())
                ? totalOutstanding(ledger) : request.getPaymentAmount();
        allocateAcrossLedger(ledger, amount, date, mode, ref);
    }

    /** Remaining due of one invoice (grand total minus what is already paid). */
    private BigDecimal dueOf(Investment inv) {
        BigDecimal paid = investmentPaymentRepository.sumByInvestment(inv.getId());
        return inv.getGrandTotal().subtract(paid == null ? BigDecimal.ZERO : paid).max(BigDecimal.ZERO);
    }

    private BigDecimal totalOutstanding(List<Investment> ledger) {
        return ledger.stream().map(this::dueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Apply the amount to invoices in ledger order (this invoice first, then older dues), capping each at its due. */
    private void allocateAcrossLedger(List<Investment> ledger, BigDecimal amount, LocalDate date, PaymentMode mode, String reference) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        UUID groupId = UUID.randomUUID();
        BigDecimal remaining = amount;
        for (Investment inv : ledger) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal due = dueOf(inv);
            if (due.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal pay = remaining.min(due);
            recordPayment(inv, new PaymentEntry(date, mode, pay, groupId, reference));
            remaining = remaining.subtract(pay);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Payment exceeds the vendor's total outstanding by " + remaining + ".");
        }
    }

    private void createInventoryBatches(Investment investment) {

        for (InvestmentItem item : investment.getItems()) {
            if (item.getItemType() != InvestmentItemType.PRODUCT) {
                continue;
            }
            inventoryService.createPurchaseBatch(item);
        }
    }

    private void validateRequest(CreateInvestmentRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("At least one item is required");
        }

        if (request.getInvestmentType() != InvestmentType.OVERHEAD && request.getVendorId() == null) {
            throw new BusinessException("Vendor is required");
        }

        if (request.getInvestmentType() == InvestmentType.OVERHEAD && request.getVendorId() != null) {
            throw new BusinessException("Vendor should not be selected for overhead");
        }

        for (CreateInvestmentItemRequest item : request.getItems()) {
            if (item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Quantity must be greater than zero");
            }

            if (item.getRate().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Rate cannot be negative");
            }

            if (request.getInvestmentType() == InvestmentType.OVERHEAD) {
                item.setItemType(InvestmentItemType.OVERHEAD);
            }

        }
    }

    private String generateInvoiceNumber() {
        long count = investmentRepository.count() + 1;
        return String.format("INV-%06d", count);
    }


    private List<InvestmentItem> buildInvestmentItems(Investment investment, List<CreateInvestmentItemRequest> requests) {

        List<InvestmentItem> items = new ArrayList<>();
        for (CreateInvestmentItemRequest request : requests) {
            InvestmentItem item = new InvestmentItem();
            item.setInvestment(investment);
            item.setItemType(request.getItemType());
            item.setItemName(request.getItemName());
            item.setQuantity(request.getQuantity());
            item.setUnit(request.getUnit());
            item.setRate(request.getRate());
            item.setSellingPrice(request.getSellingPrice());
            item.setTotalAmount(request.getQuantity().multiply(request.getRate()));
            if (request.getItemType() == InvestmentItemType.PRODUCT) {
                Product product = createOrGetProduct(request);
                item.setProduct(product);
            }
            items.add(item);
        }

        return items;
    }

    private Product createOrGetProduct(CreateInvestmentItemRequest request) {
        Product product = productRepository.findByNameIgnoreCase(request.getItemName()).orElse(null);
        if (product != null) {
            updateProductSource(product);
            return product;
        }

        product = new Product();
        product.setName(request.getItemName());
        product.setDescription(null);
        product.setIconName(null);
        product.setActive(true);
        product.setSource(ProductSource.PURCHASED);
        return productRepository.save(product);
    }

    private void updateProductSource(Product product) {
        if (product.getSource() == ProductSource.MANUFACTURED) {
            product.setSource(ProductSource.BOTH);
            productRepository.save(product);
        }
    }

    private void calculateTotals(Investment investment) {
        BigDecimal subTotal = investment.getItems()
                .stream().map(InvestmentItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        investment.setSubTotal(subTotal);
        // Final amount is always rounded to whole rupees so it matches the UI and a full payment clears it.
        BigDecimal grandTotal = subTotal.add(investment.getGstAmount())
                .add(investment.getOtherCharge())
                .subtract(investment.getDiscountAmount())
                .setScale(0, RoundingMode.HALF_UP);

        investment.setGrandTotal(grandTotal);
    }

    @Transactional(readOnly = true)
    public List<InvestmentResponse> search(UUID vendorId, LocalDate fromDate, LocalDate toDate, InvestmentType type, String search) {

        Specification<Investment> specification = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Partial, case-insensitive match on invoice number.
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("invoiceNumber")), "%" + search.trim().toLowerCase() + "%"));
            }

            if (type != null) {
                predicates.add(cb.equal(root.get("investmentType"), type));
            }

            if (vendorId != null) {
                predicates.add(cb.equal(root.get("vendor").get("id"), vendorId));
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("purchaseDate"), fromDate));
            }

            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("purchaseDate"), toDate));
            }

            query.orderBy(cb.desc(root.get("purchaseDate")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // One query each for paid total, item count and vendor name → avoids per-row N+1 in the list.
        Map<UUID, BigDecimal> paidByInvestment = new HashMap<>();
        for (Object[] row : investmentPaymentRepository.paidSums()) {
            paidByInvestment.put((UUID) row[0], (BigDecimal) row[1]);
        }
        Map<UUID, Integer> itemCountByInvestment = new HashMap<>();
        for (Object[] row : investmentItemRepository.countByInvestment()) {
            itemCountByInvestment.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        Map<UUID, String> vendorNameById = new HashMap<>();
        for (Vendor v : vendorRepository.findAll()) {
            vendorNameById.put(v.getId(), v.getName());
        }
        Map<UUID, BigDecimal> receivedByInvestment = new HashMap<>();
        for (Object[] row : investmentPaymentRepository.receivedByOriginInvoice()) {
            UUID invId = row[0] instanceof UUID u ? u : UUID.fromString(String.valueOf(row[0]));
            receivedByInvestment.merge(invId, new BigDecimal(String.valueOf(row[1])), BigDecimal::add);
        }

        // Latest purchase date on top; same date → latest created time first.
        Sort sort = Sort.by(Sort.Order.desc("purchaseDate"), Sort.Order.desc("createdAt"));
        ListAggregates agg = new ListAggregates(paidByInvestment, itemCountByInvestment, vendorNameById, receivedByInvestment);
        return investmentRepository.findAll(specification, sort).stream()
                .map(inv -> toListResponse(inv, agg))
                .toList();
    }

    // Pre-built per-invoice lookups for the list view (keeps the row mapper free of lazy loads).
    private record ListAggregates(Map<UUID, BigDecimal> paid, Map<UUID, Integer> itemCount,
                                  Map<UUID, String> vendorName, Map<UUID, BigDecimal> received) {
    }

    // List mapping that reads every per-row value from pre-built maps (no lazy loads per row).
    private InvestmentResponse toListResponse(Investment investment, ListAggregates agg) {
        BigDecimal safePaid = agg.paid().getOrDefault(investment.getId(), BigDecimal.ZERO);
        UUID vendorId = investment.getVendor() != null ? investment.getVendor().getId() : null;
        return InvestmentResponse.builder()
                .id(investment.getId()).invoiceNumber(investment.getInvoiceNumber())
                .vendorId(vendorId)
                .vendorName(vendorId != null ? agg.vendorName().get(vendorId) : null)
                .investmentType(investment.getInvestmentType()).purchaseDate(investment.getPurchaseDate())
                .createdAt(investment.getCreatedAt())
                .itemCount(agg.itemCount().getOrDefault(investment.getId(), 0)).grandTotal(investment.getGrandTotal())
                .amountReceived(agg.received().getOrDefault(investment.getId(), BigDecimal.ZERO))
                .amountPaid(safePaid).paymentStatus(computeStatus(safePaid, investment.getGrandTotal())).build();
    }

    @Transactional(readOnly = true)
    public InvestmentDetailsResponse getById(UUID id) {
        Investment investment = investmentRepository.findById(id).orElseThrow(() -> new BusinessException("Investment not found"));
        return toDetailsResponse(investment);
    }

    public void delete(UUID id) {
        Investment investment = investmentRepository.findById(id).orElseThrow(() -> new BusinessException("Investment not found"));
        investmentRepository.delete(investment);
    }

    private InvestmentResponse toResponse(Investment investment) {
        return toResponse(investment, investmentPaymentRepository.sumByInvestment(investment.getId()));
    }

    private InvestmentResponse toResponse(Investment investment, BigDecimal paid) {
        BigDecimal safePaid = paid == null ? BigDecimal.ZERO : paid;
        return InvestmentResponse.builder()
                .id(investment.getId()).invoiceNumber(investment.getInvoiceNumber())
                .vendorId(investment.getVendor() != null ? investment.getVendor().getId() : null)
                .vendorName(investment.getVendor() != null ? investment.getVendor().getName() : null)
                .investmentType(investment.getInvestmentType()).purchaseDate(investment.getPurchaseDate())
                .createdAt(investment.getCreatedAt())
                .itemCount(investment.getItems().size()).grandTotal(investment.getGrandTotal())
                .amountPaid(safePaid).paymentStatus(computeStatus(safePaid, investment.getGrandTotal())).build();
    }

    private PaymentStatus computeStatus(BigDecimal paid, BigDecimal grandTotal) {
        BigDecimal p = paid == null ? BigDecimal.ZERO : paid;
        BigDecimal g = grandTotal == null ? BigDecimal.ZERO : grandTotal;
        if (p.compareTo(BigDecimal.ZERO) <= 0) return PaymentStatus.PENDING;
        if (p.compareTo(g) >= 0) return PaymentStatus.PAID;
        return PaymentStatus.PARTIALLY_PAID;
    }

    private InvestmentDetailsResponse toDetailsResponse(Investment investment) {
        List<InvestmentPayment> payments = investmentPaymentRepository.findByInvestmentIdOrderByPaymentDateDescCreatedAtDesc(investment.getId());
        BigDecimal paid = payments.stream().map(InvestmentPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return InvestmentDetailsResponse.builder()
                .id(investment.getId()).invoiceNumber(investment.getInvoiceNumber())
                .vendorId(investment.getVendor() != null ? investment.getVendor().getId() : null)
                .vendorName(investment.getVendor() != null ? investment.getVendor().getName() : null)
                .investmentType(investment.getInvestmentType()).purchaseDate(investment.getPurchaseDate())
                .subTotal(investment.getSubTotal()).gstAmount(investment.getGstAmount())
                .discountAmount(investment.getDiscountAmount()).otherCharge(investment.getOtherCharge())
                .grandTotal(investment.getGrandTotal()).amountPaid(paid).amountDue(investment.getGrandTotal()
                        .subtract(paid).max(BigDecimal.ZERO)).paymentStatus(computeStatus(paid, investment.getGrandTotal()))
                .payments(toPaymentResponsesWithAllocations(investment.getId(), payments))
                .items(investment.getItems().stream().map(this::toItemResponse).toList()).build();
    }

    private InvestmentItemResponse toItemResponse(InvestmentItem item) {
        return InvestmentItemResponse.builder()
                .id(item.getId())
                .itemType(item.getItemType())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .itemName(item.getItemName()).quantity(item.getQuantity())
                .unit(item.getUnit()).rate(item.getRate())
                .sellingPrice(item.getSellingPrice())
                .totalAmount(item.getTotalAmount()).build();
    }

    // One payment's details; groupId ties together the rows made by a single payment.
    private record PaymentEntry(LocalDate date, PaymentMode mode, BigDecimal amount, UUID groupId, String reference) {
    }

    // ── Payments ──────────────────────────────────
    public InvestmentDetailsResponse addPayment(UUID investmentId, AddPaymentRequest request) {
        Investment investment = investmentRepository.findById(investmentId).orElseThrow(() -> new BusinessException("Investment not found"));
        recordPayment(investment, new PaymentEntry(request.getPaymentDate(), request.getMode(), request.getAmount(), UUID.randomUUID(), request.getReference()));
        return toDetailsResponse(investment);
    }

    /**
     * Validate and persist one payment. Rules: date not before the invoice date and
     * not in the future; amount must not exceed the remaining due. Payments are
     * immutable once recorded (no delete).
     */
    private void recordPayment(Investment investment, PaymentEntry entry) {
        if (entry.amount() == null || entry.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be greater than zero.");
        }
        if (entry.date() == null) {
            throw new BusinessException("Payment date is required.");
        }
        if (entry.date().isBefore(investment.getPurchaseDate())) {
            throw new BusinessException("Payment date cannot be before the invoice date (" + investment.getPurchaseDate() + ").");
        }
        if (entry.date().isAfter(LocalDate.now())) {
            throw new BusinessException("Payment date cannot be in the future.");
        }

        BigDecimal alreadyPaid = investmentPaymentRepository.sumByInvestment(investment.getId());
        BigDecimal due = investment.getGrandTotal().subtract(alreadyPaid);
        if (entry.amount().compareTo(due) > 0) {
            throw new BusinessException("Payment (₹" + entry.amount() + ") exceeds the remaining due of ₹" + due + ".");
        }

        InvestmentPayment payment = new InvestmentPayment();
        payment.setInvestment(investment);
        payment.setPaymentDate(entry.date());
        payment.setMode(entry.mode());
        payment.setAmount(entry.amount());
        payment.setPaymentGroupId(entry.groupId());
        payment.setReference(entry.reference());
        investmentPaymentRepository.save(payment);
    }

    /** All of a vendor's payments, grouped by payment event, newest first. */
    @Transactional(readOnly = true)
    public List<VendorPaymentGroupResponse> getVendorPaymentHistory(UUID vendorId) {
        Map<UUID, List<InvestmentPayment>> groups = new LinkedHashMap<>();
        for (InvestmentPayment p : investmentPaymentRepository.findByVendorId(vendorId)) {
            UUID key = p.getPaymentGroupId() != null ? p.getPaymentGroupId() : p.getId();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }
        return groups.values().stream().map(this::toGroupResponse).toList();
    }

    private VendorPaymentGroupResponse toGroupResponse(List<InvestmentPayment> group) {
        InvestmentPayment first = group.get(0);
        BigDecimal total = group.stream().map(InvestmentPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<VendorPaymentGroupResponse.Allocation> allocations = group.stream()
                .map(p -> VendorPaymentGroupResponse.Allocation.builder()
                        .invoiceNumber(p.getInvestment().getInvoiceNumber())
                        .amount(p.getAmount()).build())
                .toList();
        return VendorPaymentGroupResponse.builder()
                .paymentDate(first.getPaymentDate()).mode(first.getMode())
                .totalAmount(total).allocations(allocations).build();
    }

    @Transactional(readOnly = true)
    public List<InvestmentPaymentResponse> getPayments(UUID investmentId) {
        return investmentPaymentRepository.findByInvestmentIdOrderByPaymentDateDescCreatedAtDesc(investmentId).stream().map(this::toPaymentResponse).toList();
    }

    private InvestmentPaymentResponse toPaymentResponse(InvestmentPayment p) {
        return paymentBuilder(p).build();
    }

    private InvestmentPaymentResponse.InvestmentPaymentResponseBuilder paymentBuilder(InvestmentPayment p) {
        return InvestmentPaymentResponse.builder()
                .id(p.getId()).paymentDate(p.getPaymentDate()).mode(p.getMode())
                .amount(p.getAmount()).reference(p.getReference()).createdAt(p.getCreatedAt());
    }

    // Attach the cross-invoice allocation to any payment that was split across several invoices.
    private List<InvestmentPaymentResponse> toPaymentResponsesWithAllocations(UUID investmentId, List<InvestmentPayment> payments) {
        Map<UUID, List<InvestmentPayment>> byGroup = paymentGroups(payments);
        return payments.stream().map(p -> withAllocation(p, byGroup.get(p.getPaymentGroupId()), investmentId)).toList();
    }

    private InvestmentPaymentResponse withAllocation(InvestmentPayment p, List<InvestmentPayment> group, UUID thisInvoiceId) {
        InvestmentPaymentResponse.InvestmentPaymentResponseBuilder b = paymentBuilder(p);
        // Nothing extra for a plain single-invoice payment.
        if (group == null || group.stream().map(x -> x.getInvestment().getId()).distinct().count() <= 1) {
            return b.build();
        }
        Investment origin = originInvoice(group);
        if (origin.getId().equals(thisInvoiceId)) {
            // Origin invoice: show the whole payment and where every rupee went.
            BigDecimal total = group.stream().map(InvestmentPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            b.groupOrigin(true).groupTotal(total).allocations(toAllocations(group, thisInvoiceId));
        } else {
            // Other cleared invoice: just a reference to the invoice it was paid from.
            b.clearedFromInvoice(origin.getInvoiceNumber());
        }
        return b.build();
    }

    // The invoice the split payment was entered from = the newest one in the group.
    private Investment originInvoice(List<InvestmentPayment> group) {
        return group.stream().map(InvestmentPayment::getInvestment)
                .max(Comparator.comparing(Investment::getCreatedAt)).orElseGet(() -> group.get(0).getInvestment());
    }

    private List<InvestmentPaymentResponse.Allocation> toAllocations(List<InvestmentPayment> group, UUID thisInvoiceId) {
        return group.stream()
                .map(x -> InvestmentPaymentResponse.Allocation.builder()
                        .invoiceNumber(x.getInvestment().getInvoiceNumber())
                        .amount(x.getAmount())
                        .thisInvoice(x.getInvestment().getId().equals(thisInvoiceId)).build())
                .toList();
    }

    // Group a set of payments by their payment group (loads every sibling row with its invoice).
    private Map<UUID, List<InvestmentPayment>> paymentGroups(List<InvestmentPayment> payments) {
        List<UUID> groupIds = payments.stream().map(InvestmentPayment::getPaymentGroupId).filter(Objects::nonNull).distinct().toList();
        Map<UUID, List<InvestmentPayment>> byGroup = new HashMap<>();
        if (!groupIds.isEmpty()) {
            for (InvestmentPayment gp : investmentPaymentRepository.findByGroupIds(groupIds)) {
                byGroup.computeIfAbsent(gp.getPaymentGroupId(), k -> new ArrayList<>()).add(gp);
            }
        }
        return byGroup;
    }

    private record PdfFmt(NumberFormat nf, String cur) {
    }

    // One invoice PDF payment row, with the same split-payment handling as the details drawer.
    private String paymentPdfRow(InvestmentPayment p, List<InvestmentPayment> group, UUID thisInvoiceId, PdfFmt fmt) {
        String date = p.getPaymentDate() != null ? p.getPaymentDate().format(DTF_DATE) : "-";
        String mode = p.getMode() != null ? titleCase(p.getMode().name()) : "-";
        boolean cross = group != null && group.stream().map(x -> x.getInvestment().getId()).distinct().count() > 1;
        if (cross) {
            Investment origin = originInvoice(group);
            if (!origin.getId().equals(thisInvoiceId)) {
                return pdfRow3(date, mode, fmt.cur() + money(fmt.nf(), p.getAmount()))
                        + pdfNoteRow("Cleared via payment on " + date + " &#183; " + esc(origin.getInvoiceNumber()));
            }
            BigDecimal total = group.stream().map(InvestmentPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            return pdfRow3(date, mode, fmt.cur() + money(fmt.nf(), total)) + pdfAllocationRow(group, thisInvoiceId, fmt);
        }
        return pdfRow3(date, mode, fmt.cur() + money(fmt.nf(), p.getAmount()));
    }

    private String pdfRow3(String date, String mode, String amount) {
        return "<tr><td>" + date + "</td><td>" + mode + "</td><td class=\"right\">" + amount + "</td></tr>";
    }

    private String pdfNoteRow(String text) {
        return "<tr><td colspan=\"3\" style=\"color:#666666;font-size:10px;padding-left:12px;\">" + text + "</td></tr>";
    }

    // Under the origin row: "This invoice ₹X; INV-00 (previous due) ₹Y" (no leading label).
    private String pdfAllocationRow(List<InvestmentPayment> group, UUID thisInvoiceId, PdfFmt fmt) {
        List<String> parts = new ArrayList<>();
        for (InvestmentPayment x : group) {
            boolean self = x.getInvestment().getId().equals(thisInvoiceId);
            String label = self ? "This invoice" : esc(x.getInvestment().getInvoiceNumber()) + " (previous due)";
            parts.add(label + " " + fmt.cur() + money(fmt.nf(), x.getAmount()));
        }
        return pdfNoteRow(String.join("; ", parts));
    }

    // ── Invoice PDF ───────────────────────────────
    @Transactional(readOnly = true)
    public byte[] exportInvoicePdf(UUID id) {

        Investment investment = investmentRepository.findById(id).orElseThrow(() -> new BusinessException("Investment not found"));
        String template = systemSettingRepository.findBySettingKey(PURCHASE_INVOICE_TEMPLATE_KEY).map(SystemSetting::getSettingValue)
                .orElseThrow(() -> new BusinessException("Invoice template not configured: " + PURCHASE_INVOICE_TEMPLATE_KEY));
        return HtmlToPdfGenerator.render(fillInvoiceTemplate(template, investment));
    }

    private String fillInvoiceTemplate(String template, Investment inv) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        String cur = currencyPrefix();

        List<InvestmentPayment> payments = investmentPaymentRepository.findByInvestmentIdOrderByPaymentDateDescCreatedAtDesc(inv.getId());
        BigDecimal paid = payments.stream().map(InvestmentPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grand = inv.getGrandTotal() != null ? inv.getGrandTotal() : BigDecimal.ZERO;
        BigDecimal due = grand.subtract(paid).max(BigDecimal.ZERO);
        PaymentStatus status = computeStatus(paid, grand);

        StringBuilder items = new StringBuilder();
        int idx = 1;
        if (inv.getItems() != null) {
            for (InvestmentItem it : inv.getItems()) {
                items.append("<tr>").append("<td>").append(idx++).append("</td>").append("<td><b>").append(esc(it.getItemName())).append("</b>").append("<div class=\"itype\">").append(it.getItemType() != null ? titleCase(it.getItemType().name()) : "").append("</div></td>").append("<td class=\"right\">").append(fmtQty(it.getQuantity())).append("</td>").append("<td>").append(esc(it.getUnit() != null ? String.valueOf(it.getUnit()) : "")).append("</td>").append("<td class=\"right\">").append(cur).append(money(nf, it.getRate())).append("</td>").append("<td class=\"right\">").append(cur).append(money(nf, it.getTotalAmount())).append("</td>").append("</tr>");
            }
        }
        if (items.length() == 0) {
            items.append("<tr><td colspan=\"6\" style=\"text-align:center;color:#999999;\">No items</td></tr>");
        }

        StringBuilder prows = new StringBuilder();
        Map<UUID, List<InvestmentPayment>> byGroup = paymentGroups(payments);
        PdfFmt fmt = new PdfFmt(nf, cur);
        for (InvestmentPayment p : payments) {
            prows.append(paymentPdfRow(p, byGroup.get(p.getPaymentGroupId()), inv.getId(), fmt));
        }
        if (prows.length() == 0) {
            prows.append("<tr><td colspan=\"3\" style=\"text-align:center;color:#999999;\">No payments recorded</td></tr>");
        }

        Vendor v = inv.getVendor();
        String generatedOn = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a"));

        return template.replace("{{companyName}}", esc(setting("COMPANY_NAME", "SKR Garment")))
                .replace("{{companyAddress}}", esc(setting("COMPANY_ADDRESS", "")))
                .replace("{{companyContact}}", esc(setting("COMPANY_CONTACT", "")))
                .replace("{{companyGstin}}", esc(setting("COMPANY_GSTIN", "-")))
                .replace("{{vendorName}}", esc(v != null ? v.getName() : "-"))
                .replace("{{vendorAddress}}", esc(v != null && v.getAddress() != null ? v.getAddress() : ""))
                .replace("{{vendorContact}}", esc(vendorContact(v)))
                .replace("{{vendorGstin}}", esc(v != null && v.getGstNumber() != null ? v.getGstNumber() : "-"))
                .replace("{{invoiceNumber}}", esc(inv.getInvoiceNumber()))
                .replace("{{invoiceDate}}", inv.getPurchaseDate() != null ? inv.getPurchaseDate().format(DTF_DATE) : "-")
                .replace("{{invoiceType}}", inv.getInvestmentType() != null ? titleCase(inv.getInvestmentType().name()) : "-")
                .replace("{{itemRows}}", items.toString()).replace("{{subtotal}}", cur + money(nf, inv.getSubTotal()))
                .replace("{{gst}}", cur + money(nf, inv.getGstAmount()))
                .replace("{{discount}}", cur + money(nf, inv.getDiscountAmount()))
                .replace("{{otherCharge}}", cur + money(nf, inv.getOtherCharge()))
                .replace("{{grandTotal}}", cur + money(nf, grand))
                .replace("{{amountPaid}}", cur + money(nf, paid))
                .replace("{{amountDue}}", cur + money(nf, due))
                .replace("{{paymentStatus}}", status != null ? titleCase(status.name()) : "-")
                .replace("{{paymentRows}}", prows.toString())
                .replace("{{generatedOn}}", generatedOn);
    }

    /**
     * Currency prefix for the PDF (symbol + space). The embedded Noto Sans font renders the
     * rupee glyph, so the configured symbol is shown as-is.
     */
    private String currencyPrefix() {
        return setting("CURRENCY_SYMBOL", "₹") + " ";
    }

    private String setting(String key, String fallback) {
        return systemSettingRepository.findBySettingKey(key).map(SystemSetting::getSettingValue)
                .filter(s -> s != null && !s.isBlank()).orElse(fallback);
    }

    private String vendorContact(Vendor v) {
        if (v == null) return "-";
        String mobile = v.getMobile() != null ? v.getMobile() : "";
        String email = (v.getEmail() != null && !v.getEmail().isBlank()) ? v.getEmail() : "";
        if (!mobile.isEmpty() && !email.isEmpty()) return mobile + " / " + email;
        String one = (mobile + email).trim();
        return one.isEmpty() ? "-" : one;
    }
}