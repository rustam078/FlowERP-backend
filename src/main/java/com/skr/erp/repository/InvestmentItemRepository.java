package com.skr.erp.repository;

import com.skr.erp.common.constants.InvestmentItemType;
import com.skr.erp.entity.InvestmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InvestmentItemRepository extends JpaRepository<InvestmentItem, UUID> {
    boolean existsByProductId(UUID productId);

    // Item count per investment in one query (avoids a lazy-collection load per row in the list).
    @Query("SELECT i.investment.id, COUNT(i) FROM InvestmentItem i GROUP BY i.investment.id")
    List<Object[]> countByInvestment();

    // Distinct past item names of a type (e.g. MATERIAL) — for the Item Name type-to-suggest list.
    @Query("SELECT DISTINCT i.itemName FROM InvestmentItem i WHERE i.itemType = :type AND i.itemName IS NOT NULL AND i.itemName <> '' ORDER BY i.itemName")
    List<String> findDistinctItemNamesByType(@Param("type") InvestmentItemType type);
}