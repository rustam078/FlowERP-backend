package com.skr.erp.controller;

import com.skr.erp.common.response.CommonResponse;
import com.skr.erp.common.response.PageResponse;
import com.skr.erp.dto.request.CreateProductionRequest;
import com.skr.erp.dto.response.ProductionDetailsResponse;
import com.skr.erp.dto.response.ProductionResponse;
import com.skr.erp.dto.response.ProductionStatsResponse;
import com.skr.erp.report.ReportDownload;
import com.skr.erp.service.ProductionService;
import com.skr.erp.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/production")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", exposedHeaders = "Content-Disposition")
public class ProductionController {

    private final ProductionService productionService;
    private final ReportService reportService;

    @PostMapping
    public CommonResponse<ProductionResponse> create(@Valid @RequestBody CreateProductionRequest request) {
        return CommonResponse.<ProductionResponse>builder().success(true)
                .message("Production entry created successfully")
                .data(productionService.create(request)).build();
    }


    @GetMapping
    public CommonResponse<PageResponse<ProductionResponse>> search(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return CommonResponse.<PageResponse<ProductionResponse>>builder().success(true)
                .message("Production entries fetched successfully")
                .data(productionService.search(fromDate, toDate, employeeId, productId, pageable))
                .build();
    }

    @GetMapping("/stats")
    public CommonResponse<ProductionStatsResponse> getStats(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) UUID productId) {
        return CommonResponse.<ProductionStatsResponse>builder().success(true)
                .message("Production stats fetched successfully")
                .data(productionService.getStats(fromDate, toDate, employeeId, productId)).build();
    }

    @GetMapping("/{id}")
    public CommonResponse<ProductionDetailsResponse> getById(@PathVariable UUID id) {
        return CommonResponse.<ProductionDetailsResponse>builder().success(true)
                .message("Production details fetched successfully")
                .data(productionService.getById(id)).build();
    }

    @PutMapping("/{id}")
    public CommonResponse<ProductionResponse> update(@PathVariable UUID id, @Valid @RequestBody CreateProductionRequest request) {
        return CommonResponse.<ProductionResponse>builder().success(true).message("Production updated successfully")
                .data(productionService.update(id, request)).build();
    }

    @DeleteMapping("/{id}")
    public CommonResponse<Void> delete(@PathVariable UUID id) {
        productionService.delete(id);
        return CommonResponse.<Void>builder().success(true)
                .message("Production deleted successfully").build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable UUID id) {
        byte[] pdf = productionService.exportPdf(id);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=production-report.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(pdf);
    }

    @GetMapping("/{id}/excel")
    public ResponseEntity<byte[]> exportExcel(@PathVariable UUID id) {
        byte[] excel = productionService.exportExcel(id);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=production-report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @GetMapping("/report/pdf")
    public ResponseEntity<byte[]> reportPdf(@RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        return ReportDownload.pdf(reportService.productionPdf(fromDate, toDate),
                "production_" + reportService.fileToken(fromDate, toDate) + ".pdf");
    }

    @GetMapping("/report/excel")
    public ResponseEntity<byte[]> reportExcel(@RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        return ReportDownload.excel(reportService.productionExcel(fromDate, toDate),
                "production_" + reportService.fileToken(fromDate, toDate) + ".xlsx");
    }
}