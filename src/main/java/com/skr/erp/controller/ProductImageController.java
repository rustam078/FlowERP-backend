package com.skr.erp.controller;

import com.skr.erp.common.response.CommonResponse;
import com.skr.erp.entity.ProductImage;
import com.skr.erp.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

// Optional single product image. GET is public (so <img> tags load without the auth header);
// upload/delete go through the products module permission via the interceptor.
@RestController
@RequestMapping("/api/products/{productId}/image")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductImageController {

    private final ProductImageService productImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<Void> upload(@PathVariable UUID productId,
                                       @RequestParam("file") MultipartFile file) {
        productImageService.upload(productId, file);
        return CommonResponse.<Void>builder().success(true).message("Image uploaded").build();
    }

    @GetMapping
    public ResponseEntity<byte[]> get(@PathVariable UUID productId) {
        ProductImage image = productImageService.get(productId);
        MediaType type = image.getContentType() != null
                ? MediaType.parseMediaType(image.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(type)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .body(image.getData());
    }

    @DeleteMapping
    public CommonResponse<Void> delete(@PathVariable UUID productId) {
        productImageService.delete(productId);
        return CommonResponse.<Void>builder().success(true).message("Image removed").build();
    }
}
