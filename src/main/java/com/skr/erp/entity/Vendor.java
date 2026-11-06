package com.skr.erp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vendor")
@Getter
@Setter
public class Vendor extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Column(nullable = false, length = 20)
    private String mobile;

    @Column(length = 150)
    private String email;

    @Column(name = "gst_number", length = 30)
    private String gstNumber;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false)
    private Boolean active = true;
}