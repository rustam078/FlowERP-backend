package com.skr.erp.entity;

import com.skr.erp.common.constants.ProductSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "product")
@Getter
@Setter
public class Product extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductSource source;
    private String description;
    @Column(name = "icon_name")
    private String iconName;
    private Boolean active = true;
}