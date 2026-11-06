package com.skr.erp.qr;

import com.skr.erp.common.constants.ProductUnitStatus;
import com.skr.erp.common.response.PageResponse;
import com.skr.erp.entity.InventoryBatch;
import com.skr.erp.entity.Product;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.qr.dto.BatchLabelSummary;
import com.skr.erp.qr.dto.GenerateUnitsRequest;
import com.skr.erp.qr.dto.GenerateUnitsResponse;
import com.skr.erp.qr.dto.ProductUnitResponse;
import com.skr.erp.repository.InventoryBatchRepository;
import com.skr.erp.repository.ProductMaterialCostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class QrUnitServiceImpl implements QrUnitService {

    // Labels are generated/printed in bounded runs, so one request never handles more than this many.
    private static final int MAX_LABELS_PER_REQUEST = 2000;

    private final ProductUnitRepository productUnitRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final ProductMaterialCostRepository productMaterialCostRepository;

    @Override
    public GenerateUnitsResponse generateUnits(String batchNumber, GenerateUnitsRequest request) {
        InventoryBatch batch = inventoryBatchRepository.findByBatchNumber(batchNumber)
                               .orElseThrow(() -> new BusinessException("Batch " + batchNumber + " not found."));

        Product product = batch.getProduct();
        // Prefer this batch's own selling price (set from the purchase MRP); fall back to the product's price.
        BigDecimal salePrice = batch.getSellingPrice() != null ? batch.getSellingPrice() : currentSalePrice(product.getId());
        if (salePrice == null) {
            throw new BusinessException("Set a sale price for " + product.getName() + " before generating QR labels.");
        }

        // Only unsold (available) stock can be labelled — never the full received quantity.
        int stock = batch.getQuantityAvailable().intValue();
        long availableLabels = productUnitRepository.countByBatchNumberAndStatus(batchNumber, ProductUnitStatus.AVAILABLE);
        int remaining = (int) Math.max(0, stock - availableLabels);

        // Blank count means "a full run", capped — not the whole stock at once.
        int toGenerate = request != null && request.getCount() != null ? request.getCount() : Math.min(remaining, MAX_LABELS_PER_REQUEST);

        if (remaining <= 0) {
            throw new BusinessException("All available pieces of this batch are already labelled.");
        }
        if (toGenerate <= 0) {
            throw new BusinessException("Enter how many labels to generate.");
        }
        if (toGenerate > MAX_LABELS_PER_REQUEST) {
            throw new BusinessException("You can generate at most " + MAX_LABELS_PER_REQUEST + " labels at a time. Enter a smaller number and print in runs.");
        }
        if (toGenerate > remaining) {
            throw new BusinessException("Only " + remaining + " available piece(s) left to label in this batch.");
        }

        int startIndex = productUnitRepository.maxSerialIndex(batchNumber) + 1;

        List<ProductUnit> created = new ArrayList<>();
        for (int i = 0; i < toGenerate; i++) {
            int index = startIndex + i;
            ProductUnit unit = new ProductUnit();
            unit.setSerial(batchNumber + "-" + String.format("%03d", index));
            unit.setBatchNumber(batchNumber);
            unit.setProduct(product);
            unit.setPrintedPrice(salePrice);
            unit.setStatus(ProductUnitStatus.AVAILABLE);
            created.add(unit);
        }
        productUnitRepository.saveAll(created);

        return GenerateUnitsResponse.builder().batchNumber(batchNumber)
                .productName(product.getName()).generatedCount(created.size())
                .totalUnits(productUnitRepository.countByBatchNumber(batchNumber))
                .units(created.stream().map(this::toResponse).toList()).build();
    }

    @Override
    @Transactional(readOnly = true)
    public BatchLabelSummary summary(String batchNumber) {
        InventoryBatch batch = inventoryBatchRepository.findByBatchNumber(batchNumber)
                               .orElseThrow(() -> new BusinessException("Batch " + batchNumber + " not found."));
        int stock = batch.getQuantityAvailable().intValue();
        long availableLabels = productUnitRepository.countByBatchNumberAndStatus(batchNumber, ProductUnitStatus.AVAILABLE);
        return BatchLabelSummary.builder()
                .batchNumber(batchNumber)
                .received(batch.getQuantityReceived().intValue())
                .stock(stock)
                .totalLabelled(productUnitRepository.countByBatchNumber(batchNumber))
                .availableLabels(availableLabels)
                .sold(productUnitRepository.countByBatchNumberAndStatus(batchNumber, ProductUnitStatus.SOLD))
                .voidCount(productUnitRepository.countByBatchNumberAndStatus(batchNumber, ProductUnitStatus.VOID))
                .remaining(Math.max(0, stock - availableLabels))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductUnitResponse> rangeUnits(String batchNumber, int from, int to) {
        int start = Math.max(1, from);
        if (to < start) {
            throw new BusinessException("'To' must be greater than or equal to 'From'.");
        }
        if (to - start + 1 > MAX_LABELS_PER_REQUEST) {
            throw new BusinessException("You can print at most " + MAX_LABELS_PER_REQUEST + " labels at a time.");
        }
        return productUnitRepository.findByBatchNumberAndSerialIndexBetween(batchNumber, start, to)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductUnitResponse> listUnits(String batchNumber) {
        return productUnitRepository.findByBatchNumberOrderBySerialAsc(batchNumber)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductUnitResponse> listUnits(String batchNumber, Pageable pageable) {
        return toPageResponse(productUnitRepository.findByBatchNumberOrderBySerialAsc(batchNumber, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductUnitResponse> searchUnits(String batchNumber, String serial, Pageable pageable) {
        String term = serial == null ? "" : serial.trim();
        if (term.isEmpty()) {
            return listUnits(batchNumber, pageable);
        }
        return toPageResponse(productUnitRepository.findByBatchNumberAndSerialContainingIgnoreCaseOrderBySerialAsc(batchNumber, term, pageable));
    }

    private PageResponse<ProductUnitResponse> toPageResponse(Page<ProductUnit> page) {
        return PageResponse.<ProductUnitResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductUnitResponse scanLookup(String code) {
        String serial = QrPayload.extractSerial(code);
        ProductUnit unit = productUnitRepository.findBySerial(serial)
                .orElseThrow(() -> new BusinessException("Unknown QR code: " + serial));

        // Include the batch cost so the sale form can show profit for a scanned line.
        BigDecimal unitCost = inventoryBatchRepository.findByBatchNumber(unit.getBatchNumber())
                .map(InventoryBatch::getUnitCost).orElse(null);

        return toResponse(unit, unitCost);
    }

    @Override
    public ProductUnitResponse voidUnit(String serial) {
        ProductUnit unit = productUnitRepository.findBySerial(serial).orElseThrow(() -> new BusinessException("Unit " + serial + " not found."));
        if (unit.getStatus() == ProductUnitStatus.SOLD) {
            throw new BusinessException("Sold units cannot be voided.");
        }
        unit.setStatus(ProductUnitStatus.VOID);
        productUnitRepository.save(unit);
        return toResponse(unit);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateUnitsForSale(String batchNumber, List<String> serials) {
        if (serials == null || serials.isEmpty()) {
            return;
        }
        for (String serial : serials) {
            ProductUnit unit = productUnitRepository.findBySerial(serial).orElseThrow(() -> new BusinessException("Unknown unit: " + serial));
            if (!unit.getBatchNumber().equals(batchNumber)) {
                throw new BusinessException("Unit " + serial + " does not belong to batch " + batchNumber + ".");
            }
            if (unit.getStatus() != ProductUnitStatus.AVAILABLE) {
                throw new BusinessException("Unit " + serial + " is already " + unit.getStatus() + ".");
            }
        }
    }

    @Override
    public void consumeForSale(String batchNumber, List<String> serials, UUID saleOrderId) {
        if (serials == null || serials.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (String serial : serials) {
            ProductUnit unit = productUnitRepository.findBySerial(serial).orElseThrow(() -> new BusinessException("Unknown unit: " + serial));

            if (!unit.getBatchNumber().equals(batchNumber)) {
                throw new BusinessException("Unit " + serial + " does not belong to batch " + batchNumber + ".");
            }
            if (unit.getStatus() != ProductUnitStatus.AVAILABLE) {
                throw new BusinessException("Unit " + serial + " is already " + unit.getStatus() + ".");
            }

            unit.setStatus(ProductUnitStatus.SOLD);
            unit.setSaleOrderId(saleOrderId);
            unit.setSoldAt(now);
            productUnitRepository.save(unit);
        }
    }

    @Override
    public int removeAvailableUnits(String batchNumber, int count) {
        if (count <= 0) {
            return 0;
        }
        List<ProductUnit> available = productUnitRepository.findByBatchNumberAndStatusOrderBySerialDesc(batchNumber, ProductUnitStatus.AVAILABLE);
        if (available.isEmpty()) {
            return 0;
        }
        // Drop the newest AVAILABLE labels first; never touch SOLD/VOID units.
        List<ProductUnit> toRemove = available.subList(0, Math.min(count, available.size()));
        productUnitRepository.deleteAll(toRemove);
        return toRemove.size();
    }

    private BigDecimal currentSalePrice(UUID productId) {
        return productMaterialCostRepository.findTopByProductIdAndSalePriceNotNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(productId, LocalDate.now()).map(mc -> mc.getSalePrice()).orElse(null);
    }

    private ProductUnitResponse toResponse(ProductUnit unit) {
        return toResponse(unit, null);
    }

    private ProductUnitResponse toResponse(ProductUnit unit, BigDecimal unitCost) {
        return ProductUnitResponse.builder()
                .id(unit.getId())
                .serial(unit.getSerial())
                .batchNumber(unit.getBatchNumber())
                .productId(unit.getProduct().getId())
                .productName(unit.getProduct().getName())
                .printedPrice(unit.getPrintedPrice())
                .unitCost(unitCost).status(unit.getStatus().name())
                .qrPayload(QrPayload.build(unit.getSerial(), unit.getProduct().getName(), unit.getPrintedPrice()))
                .build();
    }
}
