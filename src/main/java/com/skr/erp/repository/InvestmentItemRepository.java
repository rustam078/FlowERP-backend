package com.skr.erp.repository;

import com.skr.erp.entity.InvestmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface InvestmentItemRepository extends JpaRepository<InvestmentItem, UUID> {
    boolean existsByProductId(UUID productId);

    // Item count per investment in one query (avoids a lazy-collection load per row in the list).
    @Query("SELECT i.investment.id, COUNT(i) FROM InvestmentItem i GROUP BY i.investment.id")
    List<Object[]> countByInvestment();
}