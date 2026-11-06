package com.skr.erp.repository;

import com.skr.erp.entity.ProductPieceCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProductPieceCodeRepository extends JpaRepository<ProductPieceCode, UUID> {

    List<ProductPieceCode> findByProductIdAndActiveTrue(UUID productId);

    // [productId, total, activeCount] per product in one query (avoids 3 counts per product in the list).
    @Query("SELECT pc.product.id, COUNT(pc), SUM(CASE WHEN pc.active = true THEN 1 ELSE 0 END) FROM ProductPieceCode pc GROUP BY pc.product.id")
    List<Object[]> codeCountsByProduct();

    boolean existsByCode(String code);

    long countByProductId(UUID productId);

    long countByProductIdAndActiveTrue(UUID productId);

    long countByProductIdAndActiveFalse(UUID productId);

    List<ProductPieceCode> findByProductId(UUID productId);

    boolean existsByCodeIgnoreCase(String code);

    List<ProductPieceCode> findByProduct_Id(UUID productId);

    List<ProductPieceCode> findByProductIdAndActiveTrueOrderByCodeAsc(UUID productId);

}