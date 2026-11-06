package com.skr.erp.service;

import com.skr.erp.entity.ProductImage;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.ProductImageRepository;
import com.skr.erp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

// Upload / fetch / delete the single optional image for a product.
@Service
@RequiredArgsConstructor
@Transactional
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;

    public void upload(UUID productId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("No image file provided");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("Only image files are allowed");
        }
        productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Product not found"));

        // One image per product: update the existing row or create a new one.
        ProductImage image = productImageRepository.findByProductId(productId)
                .orElseGet(ProductImage::new);
        image.setProductId(productId);
        image.setFileName(file.getOriginalFilename());
        image.setContentType(contentType);
        image.setSizeBytes(file.getSize());
        try {
            image.setData(file.getBytes());
        } catch (IOException e) {
            throw new BusinessException("Could not read the uploaded image");
        }
        productImageRepository.save(image);
    }

    @Transactional(readOnly = true)
    public ProductImage get(UUID productId) {
        return productImageRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException("No image for this product"));
    }

    public void delete(UUID productId) {
        productImageRepository.deleteByProductId(productId);
    }
}
