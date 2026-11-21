package com.skr.erp.controller;

import com.skr.erp.dto.request.CreatePortfolioRequest;
import com.skr.erp.dto.response.PortfolioResponse;
import com.skr.erp.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// SUPER_ADMIN only (enforced in PermissionInterceptor): manage portfolios.
@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> list(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(portfolioService.list(search));
    }

    // Is this 2-letter batch prefix still free?
    @GetMapping("/prefix-available")
    public ResponseEntity<Map<String, Boolean>> prefixAvailable(@RequestParam String prefix) {
        return ResponseEntity.ok(Map.of("available", portfolioService.isPrefixAvailable(prefix)));
    }

    @PostMapping
    public ResponseEntity<PortfolioResponse> create(@Valid @RequestBody CreatePortfolioRequest request) {
        return ResponseEntity.ok(portfolioService.create(request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PortfolioResponse> setStatus(@PathVariable UUID id, @RequestParam boolean active) {
        return ResponseEntity.ok(portfolioService.setActive(id, active));
    }

    // Soft delete; confirmName must equal the portfolio's name.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam String confirmName) {
        portfolioService.delete(id, confirmName);
        return ResponseEntity.noContent().build();
    }
}
