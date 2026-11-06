package com.skr.erp.service;

import com.skr.erp.common.constants.ProductSource;
import com.skr.erp.common.constants.RateStatus;
import com.skr.erp.dto.request.CreatePieceCodeRequest;
import com.skr.erp.dto.request.CreateProductRateRequest;
import com.skr.erp.dto.request.CreateProductRequest;
import com.skr.erp.dto.request.EnableManufacturingRequest;
import com.skr.erp.dto.response.*;
import com.skr.erp.entity.Product;
import com.skr.erp.entity.ProductMaterialCost;
import com.skr.erp.entity.ProductPieceCode;
import com.skr.erp.entity.ProductRate;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductRateRepository productRateRepository;
    private final ProductionEntryDetailRepository productionEntryDetailRepository;
    private final ProductPieceCodeRepository productPieceCodeRepository;
    private final InvestmentItemRepository investmentItemRepository;
    private final ProductMaterialCostRepository productMaterialCostRepository;
    private final ProductImageRepository productImageRepository;
    private final InventoryBatchRepository inventoryBatchRepository;

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        productRepository.findByNameIgnoreCase(request.getName())
                .ifPresent(product -> {throw new RuntimeException("Product already exists");});

        Product product = new Product();

        product.setName(request.getName());
        product.setIconName(request.getIconName());
        product.setSource(ProductSource.MANUFACTURED);
        product.setDescription(request.getDescription());
        product.setActive(true);

        product = productRepository.save(product);

        // Piece codes + initial cost/selling price (effective today) are saved with the product.
        for (CreatePieceCodeRequest pieceCode : request.getPieceCodes()) {
            savePieceCode(product, pieceCode);
        }
        saveInitialPricing(product, request.getCost(), request.getSellingPrice());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .iconName(product.getIconName())
                .description(product.getDescription())
                .active(product.getActive()).build();
    }

    // Create one piece code for a product (shared by product create + add-piece-code).
    private ProductPieceCode savePieceCode(Product product, CreatePieceCodeRequest request) {
        if (productPieceCodeRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new BusinessException("Piece code already exists: " + request.getCode());
        }
        ProductPieceCode pieceCode = new ProductPieceCode();
        pieceCode.setProduct(product);
        pieceCode.setCode(request.getCode().trim().toUpperCase());
        pieceCode.setRate(request.getRate());
        pieceCode.setActive(true);
        return productPieceCodeRepository.save(pieceCode);
    }

    // Pricing row for today — cost + selling price (updates the row if one already exists for today).
    private void saveInitialPricing(Product product, BigDecimal cost, BigDecimal sellingPrice) {
        LocalDate today = LocalDate.now();
        ProductMaterialCost materialCost = productMaterialCostRepository
                .findByProductIdAndEffectiveFrom(product.getId(), today)
                .orElseGet(ProductMaterialCost::new);
        materialCost.setProduct(product);
        materialCost.setCost(cost);
        materialCost.setSalePrice(sellingPrice);
        materialCost.setEffectiveFrom(today);
        productMaterialCostRepository.save(materialCost);
    }

    // Turn a purchased-only product into BOTH by adding its manufacturing piece codes + pricing.
    @Transactional
    public ProductResponse enableManufacturing(UUID productId, EnableManufacturingRequest request) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new BusinessException("Product not found"));
        if (product.getSource() != ProductSource.PURCHASED) {
            throw new BusinessException("Manufacturing can be enabled only for purchased-only products.");
        }
        product.setSource(ProductSource.BOTH);
        productRepository.save(product);

        for (CreatePieceCodeRequest pieceCode : request.getPieceCodes()) {
            savePieceCode(product, pieceCode);
        }
        saveInitialPricing(product, request.getCost(), request.getSellingPrice());

        return ProductResponse.builder()
                .id(product.getId()).name(product.getName())
                .iconName(product.getIconName()).description(product.getDescription())
                .source(product.getSource()).active(product.getActive()).build();
    }

    public List<ProductResponse> getAll() {

        // One query → current sale price per product (avoids N+1).
        Map<UUID, BigDecimal> salePriceByProduct = new HashMap<>();
        for (Object[] row : productMaterialCostRepository.currentSalePrices()) {
            if (row[0] != null && row[1] != null) {
                salePriceByProduct.put((UUID) row[0], (BigDecimal) row[1]);
            }
        }

        // One query → which products have an image (avoids N+1).
        Set<UUID> productsWithImage = new HashSet<>(productImageRepository.findAllProductIds());

        // One query → weighted-avg batch price (fallback for purchased products with no configured price).
        Map<UUID, BigDecimal> avgBatchSalePrice = new HashMap<>();
        for (Object[] row : inventoryBatchRepository.weightedAvgSellingPrices()) {
            long qty = ((Number) row[2]).longValue();
            if (qty > 0) {
                avgBatchSalePrice.put((UUID) row[0], ((BigDecimal) row[1]).divide(BigDecimal.valueOf(qty), 0, RoundingMode.HALF_UP));
            }
        }

        // One query → total in-stock qty per product (all available batches).
        Map<UUID, Integer> availableQtyByProduct = new HashMap<>();
        for (Object[] row : inventoryBatchRepository.availableQtyByProduct()) {
            availableQtyByProduct.put((UUID) row[0], ((Number) row[1]).intValue());
        }

        // One query → distinct in-stock batch prices per product (for the card price tooltip).
        Map<UUID, List<BigDecimal>> batchPricesByProduct = new HashMap<>();
        for (Object[] row : inventoryBatchRepository.distinctInStockSalePrices()) {
            batchPricesByProduct.computeIfAbsent((UUID) row[0], k -> new ArrayList<>()).add((BigDecimal) row[1]);
        }

        // One query → piece-code total/active counts per product (avoids 3 counts per product).
        Map<UUID, int[]> codeCountsByProduct = new HashMap<>();
        for (Object[] row : productPieceCodeRepository.codeCountsByProduct()) {
            codeCountsByProduct.put((UUID) row[0], new int[]{((Number) row[1]).intValue(), ((Number) row[2]).intValue()});
        }

        return productRepository.findAll().stream().map(product -> {

            int[] codeCounts = codeCountsByProduct.getOrDefault(product.getId(), new int[]{0, 0});
            int totalCodes = codeCounts[0];
            int activeCodes = codeCounts[1];
            int inactiveCodes = totalCodes - activeCodes;

            return ProductResponse.builder()
                    .id(product.getId()).name(product.getName())
                    .iconName(product.getIconName())
                    .description(product.getDescription())
                    .active(product.getActive())
                    .source(product.getSource())
                    .totalPieceCodes(totalCodes)
                    .activePieceCodes(activeCodes)
                    .inactivePieceCodes(inactiveCodes)
                    // Configured material-cost price first; else the weighted-avg batch price (purchased stock).
                    .sellingPrice(salePriceByProduct.getOrDefault(product.getId(), avgBatchSalePrice.get(product.getId())))
                    .availableQty(availableQtyByProduct.getOrDefault(product.getId(), 0))
                    .batchSalePrices(batchPricesByProduct.getOrDefault(product.getId(), List.of()))
                    .hasImage(productsWithImage.contains(product.getId()))
                    .createdAt(product.getCreatedAt())
                    .updatedAt(product.getUpdatedAt()).build();
        }).toList();
    }

    public ProductRateResponse addRate(UUID productId, CreateProductRateRequest request) {

        Product product = productRepository.findById(productId).orElseThrow(() -> new BusinessException("Product not found"));
        if (!product.getActive()) {
            throw new BusinessException("Cannot add rate for inactive product");
        }

        productRateRepository.findByProductIdAndEffectiveFrom(productId, request.getEffectiveFrom())
                .ifPresent(rate -> {throw new BusinessException("Rate already exists for this date");});

        ProductRate rate = new ProductRate();

        rate.setProduct(product);
        rate.setRate(request.getRate());
        rate.setEffectiveFrom(request.getEffectiveFrom());
        rate = productRateRepository.save(rate);

        return ProductRateResponse.builder()
                .id(rate.getId()).productId(product.getId())
                .productName(product.getName()).rate(rate.getRate())
                .effectiveFrom(rate.getEffectiveFrom()).build();
    }

    public List<ProductRateResponse> getRates(UUID productId) {

        List<ProductRate> rates = productRateRepository.findByProductIdOrderByEffectiveFromDesc(productId);
        LocalDate today = LocalDate.now();

        ProductRate activeRate = productRateRepository.findTopByProductIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(productId, today).orElse(null);
        LocalDate activeDate = activeRate != null ? activeRate.getEffectiveFrom() : null;

        return rates.stream()
                .map(rate -> ProductRateResponse.builder()
                        .id(rate.getId()).productId(rate.getProduct().getId())
                        .productName(rate.getProduct().getName())
                        .rate(rate.getRate()).effectiveFrom(rate.getEffectiveFrom())
                        .status(activeDate == null ? RateStatus.SCHEDULED : determineStatus(rate, today, activeDate))
                        .build()).toList();
    }

    public BigDecimal getRateByDate(UUID productId, LocalDate date) {
        return productRateRepository.findTopByProductIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(productId, date)
                .map(ProductRate::getRate).orElse(BigDecimal.ZERO);
    }

    private RateStatus determineStatus(ProductRate rate, LocalDate currentDate, LocalDate activeDate) {
        if (rate.getEffectiveFrom().isAfter(currentDate)) {
            return RateStatus.SCHEDULED;
        }
        if (rate.getEffectiveFrom().equals(activeDate)) {
            return RateStatus.ACTIVE;
        }
        return RateStatus.EXPIRED;
    }


    public ProductResponse toggleStatus(UUID productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new BusinessException("Product not found"));
        product.setActive(!product.getActive());
        product = productRepository.save(product);
        BigDecimal currentRate = productRateRepository.findTopByProductIdOrderByEffectiveFromDesc(product.getId()).map(ProductRate::getRate).orElse(BigDecimal.ZERO);
        return ProductResponse.builder().id(product.getId())
                .name(product.getName()).description(product.getDescription())
                .iconName(product.getIconName()).active(product.getActive()).build();
    }

    public void delete(UUID productId) {

        Product product = productRepository.findById(productId).orElseThrow(() -> new BusinessException("Product not found"));
        boolean used = productionEntryDetailRepository.existsByProductId(productId);

        if (used) {
            throw new BusinessException("Product has production history and cannot be deleted. Please deactivate it instead.");
        }
        if (investmentItemRepository.existsByProductId(productId)) {
            throw new BusinessException("Product cannot be deleted because it has already been used in purchase invoices.");
        }
        productRepository.delete(product);
    }

    public ProductDetailsResponse getById(UUID productId) {

        Product product = productRepository.findById(productId).orElseThrow(() -> new BusinessException("Product not found"));
        List<ProductPieceCodeResponse> pieceCodes = productPieceCodeRepository.findByProductId(productId).stream().map(pieceCode -> {
            boolean used = productionEntryDetailRepository.existsByPieceCodeId(pieceCode.getId());
            return ProductPieceCodeResponse.builder()
                    .id(pieceCode.getId()).code(pieceCode.getCode())
                    .rate(pieceCode.getRate()).active(pieceCode.getActive())
                    .used(used).createdAt(pieceCode.getCreatedAt()).build();
        }).toList();

        return ProductDetailsResponse.builder()
                .id(product.getId()).name(product.getName())
                .source(product.getSource()).iconName(product.getIconName())
                .description(product.getDescription()).active(product.getActive())
                .totalPieceCodes((int) productPieceCodeRepository.countByProductId(productId))
                .activePieceCodes((int) productPieceCodeRepository.countByProductIdAndActiveTrue(productId))
                .inactivePieceCodes((int) productPieceCodeRepository.countByProductIdAndActiveFalse(productId))
                .hasImage(productImageRepository.existsByProductId(productId))
                .pieceCodes(pieceCodes).createdAt(product.getCreatedAt()).updatedAt(product.getUpdatedAt())
                .build();
    }

    public PieceCodeResponse createPieceCode(UUID productId, CreatePieceCodeRequest request) {

        Product product = productRepository.findById(productId).orElseThrow(() -> new BusinessException("Product not found"));
        ProductPieceCode pieceCode = savePieceCode(product, request);
        return PieceCodeResponse.builder()
                .id(pieceCode.getId())
                .productId(product.getId())
                .productName(product.getName())
                .code(pieceCode.getCode())
                .rate(pieceCode.getRate())
                .active(pieceCode.getActive())
                .used(false)
                .createdAt(pieceCode.getCreatedAt()).build();
    }

    public List<PieceCodeResponse> getPieceCodes(UUID productId) {
        return productPieceCodeRepository.findByProduct_Id(productId).stream().map(pieceCode -> {
            boolean used = productionEntryDetailRepository.existsByPieceCodeId(pieceCode.getId());
            return PieceCodeResponse.builder()
                    .id(pieceCode.getId())
                    .productId(pieceCode.getProduct().getId())
                    .productName(pieceCode.getProduct().getName())
                    .code(pieceCode.getCode()).rate(pieceCode.getRate())
                    .active(pieceCode.getActive()).used(used)
                    .createdAt(pieceCode.getCreatedAt()).build();
        }).toList();
    }

    public PieceCodeResponse togglePieceCodeStatus(UUID pieceCodeId) {

        ProductPieceCode pieceCode = productPieceCodeRepository.findById(pieceCodeId).orElseThrow(() -> new BusinessException("Piece code not found"));
        pieceCode.setActive(!pieceCode.getActive());
        pieceCode = productPieceCodeRepository.save(pieceCode);
        boolean used = productionEntryDetailRepository.existsByPieceCodeId(pieceCode.getId());
        return PieceCodeResponse.builder()
                .id(pieceCode.getId()).productId(pieceCode.getProduct().getId())
                .productName(pieceCode.getProduct().getName()).code(pieceCode.getCode())
                .rate(pieceCode.getRate()).active(pieceCode.getActive()).used(used)
                .createdAt(pieceCode.getCreatedAt()).build();
    }

    public void deletePieceCode(UUID pieceCodeId) {

        ProductPieceCode pieceCode = productPieceCodeRepository.findById(pieceCodeId).orElseThrow(() -> new BusinessException("Piece code not found"));
        boolean used = productionEntryDetailRepository.existsByPieceCodeId(pieceCodeId);

        if (used) {
            throw new BusinessException("Piece code has production history. Deactivate it instead.");
        }
        productPieceCodeRepository.delete(pieceCode);
    }

    public List<PieceCodeDropdownResponse> getActivePieceCodes(UUID productId) {
        return productPieceCodeRepository.findByProductIdAndActiveTrueOrderByCodeAsc(productId)
                .stream().map(pc -> PieceCodeDropdownResponse.builder()
                        .id(pc.getId()).code(pc.getCode())
                        .rate(pc.getRate()).build())
                .toList();
    }
}