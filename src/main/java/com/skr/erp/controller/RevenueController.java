package com.skr.erp.controller;

import com.skr.erp.dto.request.RevenueFilter;
import com.skr.erp.dto.response.RevenueResponse;
import com.skr.erp.service.RevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Revenue module (read-only). Access is gated by the "revenue" permission in PermissionInterceptor.
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/revenue")
@RequiredArgsConstructor
public class RevenueController {

    private final RevenueService revenueService;

    // Paged transactions + totals; all filters optional (module, type, paymentMode, search, fromDate, toDate).
    @GetMapping("/transactions")
    public ResponseEntity<RevenueResponse> getTransactions(@ModelAttribute RevenueFilter filter,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(revenueService.getTransactions(filter, page, size));
    }
}
