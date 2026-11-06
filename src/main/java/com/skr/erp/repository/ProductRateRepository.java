package com.skr.erp.repository;

import com.skr.erp.entity.ProductRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRateRepository extends JpaRepository<ProductRate, UUID> {

    List<ProductRate> findByProductIdOrderByEffectiveFromDesc(UUID productId);

    Optional<ProductRate> findTopByProductIdOrderByEffectiveFromDesc(UUID productId);

    Optional<ProductRate> findTopByProductIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(UUID productId, LocalDate date);


    Optional<ProductRate> findByProductIdAndEffectiveFrom(UUID productId, LocalDate effectiveFrom);
}