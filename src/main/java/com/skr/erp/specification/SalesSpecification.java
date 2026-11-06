package com.skr.erp.specification;

import com.skr.erp.common.constants.PaymentMode;
import com.skr.erp.common.constants.PaymentStatus;
import com.skr.erp.entity.SalesOrder;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SalesSpecification {

    private SalesSpecification() {
    }

    public static Specification<SalesOrder> filter(String search, LocalDate fromDate, LocalDate toDate, PaymentMode paymentMode, PaymentStatus paymentStatus) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("invoiceNo")), pattern), cb.like(cb.lower(root.get("customerName")), pattern), cb.like(cb.lower(root.get("customerMobile")), pattern)));
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }

            if (toDate != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), toDate.plusDays(1).atStartOfDay()));
            }

            if (paymentMode != null) {
                predicates.add(cb.equal(root.get("paymentMode"), paymentMode));
            }

            if (paymentStatus != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }

            query.orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
