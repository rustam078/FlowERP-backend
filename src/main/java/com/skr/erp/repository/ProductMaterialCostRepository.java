package com.skr.erp.repository;

import com.skr.erp.entity.ProductMaterialCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductMaterialCostRepository extends JpaRepository<ProductMaterialCost, UUID> {

    List<ProductMaterialCost> findByProductIdOrderByEffectiveFromDesc(UUID productId);

    Optional<ProductMaterialCost> findByProductIdAndEffectiveFrom(UUID productId, LocalDate effectiveFrom);

    Optional<ProductMaterialCost> findTopByProductIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(UUID productId, LocalDate effectiveFrom);

    // Latest row that actually carries a sale price (older rows may have none),
    // so the selling price is used even if the newest row only set a cost.
    Optional<ProductMaterialCost> findTopByProductIdAndSalePriceNotNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(UUID productId, LocalDate effectiveFrom);

    boolean existsByProductId(UUID productId);

    @Query(value = """
            SELECT *
            FROM product_material_cost
            WHERE product_id = :productId
            ORDER BY effective_from DESC
            """, nativeQuery = true)
    List<ProductMaterialCost> findAllByProduct(@Param("productId") UUID productId);

    // Current effective sale price for EVERY product in one query (latest row per
    // product that has a sale price, effective on or before today). Returns [product_id, sale_price].
    @Query(value = """
            SELECT DISTINCT ON (product_id) product_id, sale_price
            FROM product_material_cost
            WHERE effective_from <= CURRENT_DATE AND sale_price IS NOT NULL
            ORDER BY product_id, effective_from DESC
            """, nativeQuery = true)
    List<Object[]> currentSalePrices();
}