package com.skr.erp.qr;

import com.skr.erp.common.constants.ProductUnitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductUnitRepository extends JpaRepository<ProductUnit, UUID> {

    Optional<ProductUnit> findBySerial(String serial);

    List<ProductUnit> findByBatchNumberOrderBySerialAsc(String batchNumber);

    Page<ProductUnit> findByBatchNumberOrderBySerialAsc(String batchNumber, Pageable pageable);

    Page<ProductUnit> findByBatchNumberAndSerialContainingIgnoreCaseOrderBySerialAsc(String batchNumber, String serial, Pageable pageable);

    /** AVAILABLE units of a batch, newest serial first — used to trim labels when stock is reduced. */
    List<ProductUnit> findByBatchNumberAndStatusOrderBySerialDesc(String batchNumber, ProductUnitStatus status);

    /** Units whose serial index falls in [fromIndex, toIndex], in order — used to print a specific chunk. */
    @Query(value = """
        SELECT * FROM product_unit
        WHERE batch_number = :bn
          AND CAST(split_part(serial, '-', 2) AS integer) BETWEEN :fromIndex AND :toIndex
        ORDER BY CAST(split_part(serial, '-', 2) AS integer)
        """, nativeQuery = true)
    List<ProductUnit> findByBatchNumberAndSerialIndexBetween(@Param("bn") String bn,
                                                             @Param("fromIndex") int fromIndex,
                                                             @Param("toIndex") int toIndex);

    long countByBatchNumber(String batchNumber);

    long countByBatchNumberAndStatus(String batchNumber, ProductUnitStatus status);

    /** Highest serial suffix already used for a batch, so generation never collides. */
    @Query(value = """
        SELECT COALESCE(MAX(CAST(split_part(serial, '-', 2) AS INTEGER)), 0)
        FROM product_unit
        WHERE batch_number = :batchNumber
        """, nativeQuery = true)
    int maxSerialIndex(@Param("batchNumber") String batchNumber);
}
