package com.skr.erp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// One optional image per product, stored as bytes in the DB (bytea).
@Entity
@Table(name = "product_image")
@Getter
@Setter
public class ProductImage extends BaseEntity {

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "data", nullable = false)
    private byte[] data;
}
