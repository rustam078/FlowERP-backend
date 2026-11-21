package com.skr.erp.repository;

import com.skr.erp.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    Optional<ProductImage> findByProductId(UUID productId);

    // Public <img src> lookup: no session (so no portfolio filter); the unguessable product id is the key.
    @Query(value = "SELECT * FROM product_image WHERE product_id = :productId", nativeQuery = true)
    Optional<ProductImage> findForPublicView(@Param("productId") UUID productId);

    boolean existsByProductId(UUID productId);

    void deleteByProductId(UUID productId);

    // Product ids that have an image — one query to flag many products (avoids N+1).
    @Query("select pi.productId from ProductImage pi")
    List<UUID> findAllProductIds();
}
