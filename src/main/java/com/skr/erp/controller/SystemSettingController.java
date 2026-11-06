package com.skr.erp.controller;

import com.skr.erp.dto.request.SystemSettingRequest;
import com.skr.erp.dto.response.SystemSettingResponse;
import com.skr.erp.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SystemSettingController {

    private final SystemSettingService service;

    @GetMapping
    public ResponseEntity<List<SystemSettingResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{key}")
    public ResponseEntity<SystemSettingResponse> get(@PathVariable String key) {
        return ResponseEntity.ok(service.get(key));
    }

    @PutMapping("/{key}")
    public ResponseEntity<SystemSettingResponse> update(@PathVariable String key, @RequestBody SystemSettingRequest request) {
        return ResponseEntity.ok(service.update(key, request.getThreshold()));
    }
}