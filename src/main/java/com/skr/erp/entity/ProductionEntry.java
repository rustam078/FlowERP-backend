package com.skr.erp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "production_entry")
@Getter
@Setter
public class ProductionEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "production_date", nullable = false)
    private LocalDate productionDate;

    // Batch-fetched so a page of N entries loads details in IN(...) batches, not N queries.
    @OneToMany(mappedBy = "productionEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<ProductionEntryDetail> details = new ArrayList<>();
}