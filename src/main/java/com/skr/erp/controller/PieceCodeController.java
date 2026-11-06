package com.skr.erp.controller;

import com.skr.erp.common.response.CommonResponse;
import com.skr.erp.dto.request.CreatePieceCodeRequest;
import com.skr.erp.dto.response.PieceCodeResponse;
import com.skr.erp.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PieceCodeController {

    private final ProductService productService;

    @PostMapping("/products/{productId}/piece-codes")
    public CommonResponse<PieceCodeResponse> create(@PathVariable UUID productId, @Valid @RequestBody CreatePieceCodeRequest request) {
        return CommonResponse.<PieceCodeResponse>builder().success(true)
                .message("Piece code created successfully")
                .data(productService.createPieceCode(productId, request)).build();
    }

    @GetMapping("/products/{productId}/piece-codes")
    public CommonResponse<List<PieceCodeResponse>> getAll(@PathVariable UUID productId) {
        return CommonResponse.<List<PieceCodeResponse>>builder().success(true)
                .message("Piece codes fetched successfully")
                .data(productService.getPieceCodes(productId)).build();
    }

    @PatchMapping("/piece-codes/{id}/status")
    public CommonResponse<PieceCodeResponse> toggleStatus(@PathVariable UUID id) {
        return CommonResponse.<PieceCodeResponse>builder().success(true)
                .message("Piece code status updated")
                .data(productService.togglePieceCodeStatus(id)).build();
    }

    @DeleteMapping("/piece-codes/{id}")
    public CommonResponse<Void> delete(@PathVariable UUID id) {
        productService.deletePieceCode(id);
        return CommonResponse.<Void>builder().success(true)
                .message("Piece code deleted successfully").build();
    }
}