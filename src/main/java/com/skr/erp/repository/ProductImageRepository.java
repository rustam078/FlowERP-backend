package com.skr.erp.repository;

import com.skr.erp.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    Optional<ProductImage> findByProductId(UUID productId);

    boolean existsByProductId(UUID productId);

    void deleteByProductId(UUID productId);

    // Product ids that have an image — one query to flag many products (avoids N+1).
    @Query("select pi.productId from ProductImage pi")
    List<UUID> findAllProductIds();
}
