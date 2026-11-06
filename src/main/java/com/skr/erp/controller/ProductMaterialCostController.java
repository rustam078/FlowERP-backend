package com.skr.erp.controller;

import com.skr.erp.common.response.CommonResponse;
import com.skr.erp.dto.request.CreateProductMaterialCostRequest;
import com.skr.erp.dto.request.UpdateMaterialCostRequest;
import com.skr.erp.dto.response.ProductMaterialCostResponse;
import com.skr.erp.service.ProductMaterialCostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/product-material-cost")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductMaterialCostController {

    private final ProductMaterialCostService productMaterialCostService;

    @PostMapping("/{productId}")
    public CommonResponse<ProductMaterialCostResponse> addMaterialCost(@PathVariable UUID productId, @Valid @RequestBody CreateProductMaterialCostRequest request) {
        return CommonResponse.<ProductMaterialCostResponse>builder().success(true).message("Material cost added successfully")
                .data(productMaterialCostService.addMaterialCost(productId, request)).build();
    }

    @GetMapping("/{productId}")
    public CommonResponse<List<ProductMaterialCostResponse>> getMaterialCosts(@PathVariable UUID productId) {
        return CommonResponse.<List<ProductMaterialCostResponse>>builder().success(true)
                .message("Material costs fetched successfully")
                .data(productMaterialCostService.getMaterialCosts(productId)).build();
    }

    @PutMapping("/{id}")
    public CommonResponse<ProductMaterialCostResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateMaterialCostRequest request) {
        return CommonResponse.<ProductMaterialCostResponse>builder().success(true).message("Material cost updated successfully")
                .data(productMaterialCostService.update(id, request)).build();
    }

    @GetMapping("/{productId}/current")
    public CommonResponse<BigDecimal> getMaterialCostByDate(@PathVariable UUID productId, @RequestParam LocalDate date) {
        return CommonResponse.<BigDecimal>builder().success(true)
                .message("Material cost fetched successfully")
                .data(productMaterialCostService.getMaterialCostByDate(productId, date)).build();
    }

    @DeleteMapping("/{id}")
    public CommonResponse<Void> delete(@PathVariable UUID id) {
        productMaterialCostService.delete(id);
        return CommonResponse.<Void>builder().success(true)
                .message("Material cost deleted successfully").build();
    }
}