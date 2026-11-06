package com.skr.erp.specification;

import com.skr.erp.entity.ProductionEntry;
import com.skr.erp.entity.ProductionEntryDetail;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ProductionSpecification {

    private ProductionSpecification() {
    }

    public static Specification<ProductionEntry> filter(LocalDate fromDate, LocalDate toDate,
                                                        UUID employeeId, UUID productId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("productionDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("productionDate"), toDate));
            }
            if (employeeId != null) {
                predicates.add(cb.equal(root.get("employee").get("id"), employeeId));
            }
            if (productId != null) {
                Subquery<UUID> sub = query.subquery(UUID.class);
                Root<ProductionEntryDetail> d = sub.from(ProductionEntryDetail.class);
                sub.select(d.get("productionEntry").get("id"))
                        .where(cb.equal(d.get("product").get("id"), productId));
                predicates.add(root.get("id").in(sub));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
