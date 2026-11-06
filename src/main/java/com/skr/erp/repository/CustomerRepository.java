package com.skr.erp.repository;

import com.skr.erp.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByMobile(String mobile);
    Page<Customer> findByMobileContainingIgnoreCase(String mobile, Pageable pageable);
}