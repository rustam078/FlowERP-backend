package com.skr.erp.qr;

import com.skr.erp.common.response.CommonResponse;
import com.skr.erp.common.response.PageResponse;
import com.skr.erp.qr.dto.BatchLabelSummary;
import com.skr.erp.qr.dto.GenerateUnitsRequest;
import com.skr.erp.qr.dto.GenerateUnitsResponse;
import com.skr.erp.qr.dto.ProductUnitResponse;
import com.skr.erp.qr.dto.ScanRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qr")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QrUnitController {

    private final QrUnitService qrUnitService;

    /** Generate (label) units for a batch. */
    @PostMapping("/batches/{batchNumber}/units/generate")
    public CommonResponse<GenerateUnitsResponse> generate(
            @PathVariable String batchNumber,
            @Valid @RequestBody(required = false) GenerateUnitsRequest request) {

        return CommonResponse.<GenerateUnitsResponse>builder()
                .success(true)
                .message("QR labels generated successfully")
                .data(qrUnitService.generateUnits(batchNumber, request))
                .build();
    }

    /** Cheap label counts for a batch (so the dialog never loads every unit). */
    @GetMapping("/batches/{batchNumber}/units/summary")
    public CommonResponse<BatchLabelSummary> summary(@PathVariable String batchNumber) {
        return CommonResponse.<BatchLabelSummary>builder()
                .success(true)
                .message("Summary fetched successfully")
                .data(qrUnitService.summary(batchNumber))
                .build();
    }

    /** All units for a batch (for the print sheet / reprint). */
    @GetMapping("/batches/{batchNumber}/units")
    public CommonResponse<List<ProductUnitResponse>> list(@PathVariable String batchNumber) {
        return CommonResponse.<List<ProductUnitResponse>>builder()
                .success(true)
                .message("Units fetched successfully")
                .data(qrUnitService.listUnits(batchNumber))
                .build();
    }

    /** Existing labels in the serial range [from, to], to print/reprint a specific chunk. */
    @GetMapping("/batches/{batchNumber}/units/range")
    public CommonResponse<List<ProductUnitResponse>> range(
            @PathVariable String batchNumber,
            @RequestParam int from,
            @RequestParam int to) {
        return CommonResponse.<List<ProductUnitResponse>>builder()
                .success(true)
                .message("Labels fetched successfully")
                .data(qrUnitService.rangeUnits(batchNumber, from, to))
                .build();
    }

    /** A page of units, optionally filtered by serial text (for the reprint browser). */
    @GetMapping("/batches/{batchNumber}/units/page")
    public CommonResponse<PageResponse<ProductUnitResponse>> listPaged(
            @PathVariable String batchNumber,
            @RequestParam(required = false) String serial,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return CommonResponse.<PageResponse<ProductUnitResponse>>builder()
                .success(true)
                .message("Units fetched successfully")
                .data(qrUnitService.searchUnits(batchNumber, serial, PageRequest.of(page, size)))
                .build();
    }

    /** Scan lookup — accepts the full QR payload or a bare serial. */
    @PostMapping("/scan")
    public CommonResponse<ProductUnitResponse> scan(@Valid @RequestBody ScanRequest request) {
        return CommonResponse.<ProductUnitResponse>builder()
                .success(true)
                .message("Unit found")
                .data(qrUnitService.scanLookup(request.getCode()))
                .build();
    }

    /** Void a damaged/lost unit. */
    @PatchMapping("/units/{serial}/void")
    public CommonResponse<ProductUnitResponse> voidUnit(@PathVariable String serial) {
        return CommonResponse.<ProductUnitResponse>builder()
                .success(true)
                .message("Unit voided")
                .data(qrUnitService.voidUnit(serial))
                .build();
    }
}
