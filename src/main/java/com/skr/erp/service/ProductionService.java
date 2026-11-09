package com.skr.erp.service;

import com.skr.erp.dto.request.CreateProductionRequest;
import com.skr.erp.dto.request.ProductionItemRequest;
import com.skr.erp.dto.response.ProductionDetailsResponse;
import com.skr.erp.dto.response.ProductionItemResponse;
import com.skr.erp.dto.response.ProductionResponse;
import com.skr.erp.dto.response.ProductionStatsResponse;
import com.skr.erp.common.response.PageResponse;
import com.skr.erp.entity.*;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.pdf.HtmlToPdfGenerator;
import com.skr.erp.pdf.ProductionExcelGenerator;
import com.skr.erp.repository.*;
import com.skr.erp.specification.ProductionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductionService {

    private static final String PRODUCTION_PDF_TEMPLATE_KEY = "PRODUCTION_PDF_TEMPLATE";
    private final ProductionEntryRepository productionEntryRepository;
    private final ProductionEntryDetailRepository productionEntryDetailRepository;
    private final EmployeeRepository employeeRepository;
    private final ProductRepository productRepository;
    private final SystemSettingRepository systemSettingRepository;

    // Distinct past work names — powers the Work field's type-to-suggest list.
    public java.util.List<String> workNames() {
        return productionEntryDetailRepository.findDistinctWorkNames();
    }

    public ProductionResponse create(CreateProductionRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new BusinessException("Employee not found"));

        if (!employee.getActive()) {
            throw new BusinessException("Employee is inactive");
        }

        ProductionEntry productionEntry = new ProductionEntry();

        productionEntry.setEmployee(employee);
        productionEntry.setProductionDate(request.getProductionDate());

        for (ProductionItemRequest item : request.getItems()) {
            productionEntry.getDetails().add(buildDetail(productionEntry, item));
        }

        productionEntry = productionEntryRepository.save(productionEntry);
        return map(productionEntry);
    }

    // Build one piece-work detail: free-text work + qty + locked rate; product is optional (reporting).
    private ProductionEntryDetail buildDetail(ProductionEntry entry, ProductionItemRequest item) {
        ProductionEntryDetail detail = new ProductionEntryDetail();
        detail.setProductionEntry(entry);
        detail.setWork(item.getWork());
        if (item.getProductId() != null) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException("Product not found"));
            detail.setProduct(product);
            detail.setProductNameSnapshot(product.getName());
        }
        detail.setQuantity(item.getQuantity());
        BigDecimal rate = item.getRate() != null ? item.getRate() : BigDecimal.ZERO;
        detail.setRateSnapshot(rate);
        detail.setAmountSnapshot(rate.multiply(BigDecimal.valueOf(item.getQuantity())));
        return detail;
    }

    @Transactional(readOnly = true)
    public List<ProductionResponse> getAll() {
        return productionEntryRepository.findAll().stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public ProductionDetailsResponse getById(UUID productionId) {
        ProductionEntry productionEntry = productionEntryRepository.findById(productionId).orElseThrow(() -> new BusinessException("Production entry not found"));
        int totalQuantity = productionEntry.getDetails().stream()
                            .mapToInt(ProductionEntryDetail::getQuantity).sum();

        BigDecimal totalAmount = productionEntry.getDetails().stream()
                .map(ProductionEntryDetail::getAmountSnapshot)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ProductionDetailsResponse.builder()
                .id(productionEntry.getId())
                .employeeName(productionEntry.getEmployee().getFullName())
                .productionDate(productionEntry.getProductionDate())
                .productCount(productionEntry.getDetails().size())
                .totalQuantity(totalQuantity).totalAmount(totalAmount)
                .items(productionEntry.getDetails().stream().map(this::toItemResponse).toList())
                .build();
    }

    public ProductionResponse update(UUID productionId, CreateProductionRequest request) {

        ProductionEntry productionEntry = productionEntryRepository.findById(productionId).orElseThrow(() -> new BusinessException("Production entry not found"));
        Employee employee = employeeRepository.findById(request.getEmployeeId()).orElseThrow(() -> new BusinessException("Employee not found"));

        if (!employee.getActive()) {
            throw new BusinessException("Employee is inactive");
        }

        productionEntry.setEmployee(employee);
        productionEntry.setProductionDate(request.getProductionDate());
        productionEntry.getDetails().clear();

        for (ProductionItemRequest item : request.getItems()) {
            productionEntry.getDetails().add(buildDetail(productionEntry, item));
        }
        productionEntry = productionEntryRepository.save(productionEntry);
        return map(productionEntry);

    }

    public void delete(UUID productionId) {
        // Piece-work entries hold no inventory, so deleting just removes the record.
        ProductionEntry productionEntry = productionEntryRepository.findById(productionId)
                .orElseThrow(() -> new BusinessException("Production entry not found"));
        productionEntryRepository.delete(productionEntry);
    }


    @Transactional(readOnly = true)
    public PageResponse<ProductionResponse> search(LocalDate fromDate, LocalDate toDate,
                                                   UUID employeeId, UUID productId, Pageable pageable) {

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException("fromDate cannot be after toDate");
        }

        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "productionDate").and(Sort.by(Sort.Direction.DESC, "createdAt")));

        Page<ProductionEntry> page = productionEntryRepository.findAll(
                ProductionSpecification.filter(fromDate, toDate, employeeId, productId), sorted);

        List<ProductionResponse> content = page.getContent().stream().map(this::map).toList();

        return PageResponse.<ProductionResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    // Summary-card KPIs so the list can paginate server-side without loading every row.
    @Transactional(readOnly = true)
    public ProductionStatsResponse getStats(LocalDate fromDate, LocalDate toDate, UUID employeeId, UUID productId) {
        Object[] totals = productionEntryDetailRepository.productionStats(fromDate, toDate, employeeId, productId).get(0);
        LocalDate today = LocalDate.now();
        Object[] todayRow = productionEntryDetailRepository.productionStats(today, today, employeeId, productId).get(0);
        YearMonth month = YearMonth.now();
        Object[] monthRow = productionEntryDetailRepository.productionStats(month.atDay(1), month.atEndOfMonth(), employeeId, productId).get(0);

        return ProductionStatsResponse.builder()
                .totalEntries(asLong(totals[0]))
                .totalQuantity(asLong(totals[1]))
                .totalAmount(asBig(totals[2]))
                .todayQuantity(asLong(todayRow[1]))
                .todayAmount(asBig(todayRow[2]))
                .monthQuantity(asLong(monthRow[1]))
                .monthAmount(asBig(monthRow[2]))
                .build();
    }

    private long asLong(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private BigDecimal asBig(Object o) {
        return o == null ? BigDecimal.ZERO : (BigDecimal) o;
    }

    public byte[] exportPdf(UUID productionId) {

        ProductionDetailsResponse response = getById(productionId);
        // Fetch the HTML template from system settings (by name), fill placeholders, render to PDF.
        String template = systemSettingRepository.findBySettingKey(PRODUCTION_PDF_TEMPLATE_KEY).map(SystemSetting::getSettingValue).orElseThrow(() -> new BusinessException("PDF template not configured: " + PRODUCTION_PDF_TEMPLATE_KEY));
        String html = fillProductionTemplate(template, response);
        return HtmlToPdfGenerator.render(html);
    }

    public byte[] exportExcel(UUID productionId) {
        ProductionDetailsResponse response = getById(productionId);
        return ProductionExcelGenerator.generate(response);
    }

    private String fillProductionTemplate(String template, ProductionDetailsResponse d) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));

        String cur = setting("CURRENCY_SYMBOL", "₹");
        StringBuilder rows = new StringBuilder();
        if (d.getItems() != null) {
            for (ProductionItemResponse item : d.getItems()) {
                // Work Name = the free-text work; fall back to the (optional) product link.
                String workName = item.getWork() != null && !item.getWork().isBlank()
                        ? item.getWork() : (item.getProductName() != null ? item.getProductName() : "-");
                rows.append("<tr>")
                        .append("<td>").append(esc(workName)).append("</td>")
                        .append("<td class=\"right\">").append(item.getQuantity() != null ? item.getQuantity() : 0).append("</td>")
                        .append("<td class=\"right\">").append(cur).append(" ").append(item.getRate() != null ? nf.format(item.getRate()) : "0").append("</td>")
                        .append("<td class=\"right\">").append(cur).append(" ").append(item.getAmount() != null ? nf.format(item.getAmount()) : "0").append("</td>")
                        .append("</tr>");
            }
        }

        String generatedOn = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a"));

        return template.replace("{{companyName}}", esc(setting("COMPANY_NAME", "SKR Garment")))
                .replace("{{companyAddress}}", esc(setting("COMPANY_ADDRESS", "")))
                .replace("{{companyContact}}", esc(setting("COMPANY_CONTACT", "")))
                .replace("{{companyGstin}}", esc(setting("COMPANY_GSTIN", "-")))
                .replace("{{refId}}", esc(d.getId() != null ? d.getId().toString() : "-"))
                .replace("{{workDate}}", d.getProductionDate() != null ? d.getProductionDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")) : "-")
                .replace("{{employeeName}}", esc(d.getEmployeeName()))
                .replace("{{totalWorkItems}}", String.valueOf(d.getProductCount() != null ? d.getProductCount() : 0))
                .replace("{{totalQuantity}}", nf.format(d.getTotalQuantity() != null ? d.getTotalQuantity() : 0))
                .replace("{{totalAmount}}", cur + " " + (d.getTotalAmount() != null ? nf.format(d.getTotalAmount()) : "0"))
                .replace("{{itemRows}}", rows.toString()).replace("{{generatedOn}}", generatedOn);
    }

    // Escape dynamic values so the filled template stays valid XHTML for the renderer.
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * Live system-setting value, or the fallback when missing/blank.
     */
    private String setting(String key, String fallback) {
        return systemSettingRepository.findBySettingKey(key).map(SystemSetting::getSettingValue).filter(s -> s != null && !s.isBlank()).orElse(fallback);
    }


    private ProductionResponse map(ProductionEntry productionEntry) {

        int totalQuantity = productionEntry.getDetails().stream().mapToInt(ProductionEntryDetail::getQuantity).sum();
        BigDecimal totalAmount = productionEntry.getDetails().stream()
                .map(detail -> detail.getAmountSnapshot() == null ? BigDecimal.ZERO : detail.getAmountSnapshot())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ProductionResponse.builder()
                .id(productionEntry.getId())
                .employeeId(productionEntry.getEmployee().getId())
                .employeeName(productionEntry.getEmployee().getFullName())
                .productionDate(productionEntry.getProductionDate())
                .productCount(productionEntry.getDetails().size())
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .items(productionEntry.getDetails().stream().map(this::toItemResponse).toList())
                .build();
    }

    private ProductionItemResponse toItemResponse(ProductionEntryDetail detail) {
        return ProductionItemResponse.builder()
                .work(detail.getWork())
                .productId(detail.getProduct() != null ? detail.getProduct().getId() : null)
                .productName(detail.getProductNameSnapshot())
                .quantity(detail.getQuantity())
                .rate(detail.getRateSnapshot())
                .amount(detail.getAmountSnapshot())
                .build();
    }


}