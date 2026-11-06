package com.skr.erp.repository;

import com.skr.erp.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID>, JpaSpecificationExecutor<Vendor> {

    Optional<Vendor> findByNameIgnoreCase(String name);

    Optional<Vendor> findByMobile(String mobile);

    Optional<Vendor> findByEmailIgnoreCase(String email);

    Optional<Vendor> findByGstNumberIgnoreCase(String gstNumber);
}