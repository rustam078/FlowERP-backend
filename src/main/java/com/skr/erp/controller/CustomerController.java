package com.skr.erp.controller;

import com.skr.erp.common.response.PageResponse;
import com.skr.erp.dto.response.CustomerResponse;
import com.skr.erp.dto.response.CustomerSaleResponse;
import com.skr.erp.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CustomerController {

    private final SalesService salesService;

    @GetMapping("/{customerId}/sales")
    public ResponseEntity<PageResponse<CustomerSaleResponse>> getCustomerSales(@PathVariable UUID customerId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(salesService.getCustomerSales(customerId, pageable));
    }

    @GetMapping
    public ResponseEntity<PageResponse<CustomerResponse>> getCustomers(
            @RequestParam(required = false) String mobile,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(salesService.getCustomers(mobile, pageable));
    }
}
