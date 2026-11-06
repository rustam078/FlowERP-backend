package com.skr.erp.controller;

import com.skr.erp.common.response.CommonResponse;
import com.skr.erp.dto.request.CreateProductRateRequest;
import com.skr.erp.dto.request.CreateProductRequest;
import com.skr.erp.dto.request.EnableManufacturingRequest;
import com.skr.erp.dto.response.PieceCodeDropdownResponse;
import com.skr.erp.dto.response.ProductDetailsResponse;
import com.skr.erp.dto.response.ProductRateResponse;
import com.skr.erp.dto.response.ProductResponse;
import com.skr.erp.dto.response.ProductSaleHistoryResponse;
import com.skr.erp.common.response.PageResponse;
import com.skr.erp.service.ProductService;
import com.skr.erp.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final SalesService salesService;

    @PostMapping
    public CommonResponse<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return CommonResponse.<ProductResponse>builder().success(true)
                .message("Product created successfully")
                .data(productService.create(request)).build();
    }

    @GetMapping
    public CommonResponse<List<ProductResponse>> getAll() {
        return CommonResponse.<List<ProductResponse>>builder().success(true)
                .message("Products fetched successfully")
                .data(productService.getAll()).build();
    }

    @PostMapping("/{productId}/enable-manufacturing")
    public CommonResponse<ProductResponse> enableManufacturing(@PathVariable UUID productId, @Valid @RequestBody EnableManufacturingRequest request) {
        return CommonResponse.<ProductResponse>builder().success(true)
                .message("Manufacturing enabled")
                .data(productService.enableManufacturing(productId, request)).build();
    }

    @PostMapping("/{productId}/rates")
    public CommonResponse<ProductRateResponse> addRate(@PathVariable UUID productId, @Valid @RequestBody CreateProductRateRequest request) {
        return CommonResponse.<ProductRateResponse>builder().success(true)
                .message("Rate added successfully")
                .data(productService.addRate(productId, request)).build();
    }

    @GetMapping("/{productId}/rates")
    public CommonResponse<List<ProductRateResponse>> getRates(@PathVariable UUID productId) {
        return CommonResponse.<List<ProductRateResponse>>builder().success(true)
                .message("Rates fetched successfully")
                .data(productService.getRates(productId)).build();
    }

    @GetMapping("/{productId}/rate")
    public CommonResponse<BigDecimal> getRateByDate(@PathVariable UUID productId, @RequestParam LocalDate date) {
        return CommonResponse.<BigDecimal>builder().success(true)
                .message("Rate fetched successfully")
                .data(productService.getRateByDate(productId, date)).build();
    }

    @PatchMapping("/{id}/status")
    public CommonResponse<ProductResponse> toggleStatus(@PathVariable UUID id) {
        ProductResponse response = productService.toggleStatus(id);
        return CommonResponse.<ProductResponse>builder().success(true)
                .message(response.getActive() ? "Product activated successfully" : "Product deactivated successfully")
                .data(response).build();
    }

    @DeleteMapping("/{id}")
    public CommonResponse<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return CommonResponse.<Void>builder().success(true)
                .message("Product deleted successfully").build();
    }

    @GetMapping("/{id}")
    public CommonResponse<ProductDetailsResponse> getById(@PathVariable UUID id) {
        return CommonResponse.<ProductDetailsResponse>builder().success(true)
                .message("Product details fetched successfully")
                .data(productService.getById(id)).build();
    }

    @GetMapping("/{productId}/piece-codes/active")
    public CommonResponse<List<PieceCodeDropdownResponse>> getActivePieceCodes(@PathVariable UUID productId) {
        return CommonResponse.<List<PieceCodeDropdownResponse>>builder().success(true)
                .message("Active piece codes fetched successfully")
                .data(productService.getActivePieceCodes(productId)).build();
    }

    @GetMapping("/{productId}/sales")
    public ResponseEntity<PageResponse<ProductSaleHistoryResponse>> getProductSales(@PathVariable UUID productId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(salesService.getProductSales(productId, pageable));
    }
}