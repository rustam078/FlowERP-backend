package com.skr.erp.service;

import com.skr.erp.dto.request.CreateProductMaterialCostRequest;
import com.skr.erp.dto.request.UpdateMaterialCostRequest;
import com.skr.erp.dto.response.ProductMaterialCostResponse;
import com.skr.erp.entity.Product;
import com.skr.erp.entity.ProductMaterialCost;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.ProductMaterialCostRepository;
import com.skr.erp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductMaterialCostService {

    private final ProductRepository productRepository;
    private final ProductMaterialCostRepository productMaterialCostRepository;

    public ProductMaterialCostResponse addMaterialCost(UUID productId, CreateProductMaterialCostRequest request) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new BusinessException("Product not found"));

        if (!product.getActive()) {
            throw new BusinessException("Cannot add material cost for inactive product");
        }

        productMaterialCostRepository.findByProductIdAndEffectiveFrom(productId, request.getEffectiveFrom())
                .ifPresent(cost -> {throw new BusinessException("Material cost already exists for this date");});

        ProductMaterialCost materialCost = new ProductMaterialCost();
        materialCost.setProduct(product);
        materialCost.setCost(request.getCost());
        materialCost.setSalePrice(request.getSalePrice());
        materialCost.setEffectiveFrom(request.getEffectiveFrom());
        materialCost.setRemarks(request.getRemarks());
        materialCost = productMaterialCostRepository.save(materialCost);
        return toResponse(materialCost);
    }

    @Transactional(readOnly = true)
    public List<ProductMaterialCostResponse> getMaterialCosts(UUID productId) {
        return productMaterialCostRepository.findAllByProduct(productId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getMaterialCostByDate(UUID productId, LocalDate date) {
        return productMaterialCostRepository.findTopByProductIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(productId, date)
                .map(ProductMaterialCost::getCost).orElse(BigDecimal.ZERO);
    }

    public void delete(UUID id) {
        ProductMaterialCost materialCost = productMaterialCostRepository.findById(id).orElseThrow(() -> new BusinessException("Material cost not found"));
        productMaterialCostRepository.delete(materialCost);
    }

    @Transactional
    public ProductMaterialCostResponse update(UUID id, UpdateMaterialCostRequest request) {
        ProductMaterialCost materialCost = productMaterialCostRepository.findById(id).orElseThrow(() -> new BusinessException("Material cost not found"));

        materialCost.setCost(request.getCost());
        materialCost.setSalePrice(request.getSalePrice());
        materialCost.setEffectiveFrom(request.getEffectiveFrom());
        materialCost = productMaterialCostRepository.save(materialCost);
        return toResponse(materialCost);
    }

    private ProductMaterialCostResponse toResponse(ProductMaterialCost materialCost) {
        return ProductMaterialCostResponse.builder()
                .id(materialCost.getId())
                .productId(materialCost.getProduct().getId())
                .productName(materialCost.getProduct().getName())
                .cost(materialCost.getCost())
                .salePrice(materialCost.getSalePrice())
                .effectiveFrom(materialCost.getEffectiveFrom())
                .remarks(materialCost.getRemarks())
                .createdAt(materialCost.getCreatedAt()).build();
    }
}