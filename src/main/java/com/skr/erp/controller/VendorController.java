package com.skr.erp.controller;

import com.skr.erp.common.response.CommonResponse;
import com.skr.erp.dto.request.CreateVendorRequest;
import com.skr.erp.dto.request.UpdateVendorRequest;
import com.skr.erp.dto.response.VendorDetailsResponse;
import com.skr.erp.dto.response.VendorResponse;
import com.skr.erp.service.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VendorController {

    private final VendorService vendorService;

    @PostMapping
    public CommonResponse<VendorResponse> create(@Valid @RequestBody CreateVendorRequest request) {
        return CommonResponse.<VendorResponse>builder().success(true)
                .message("Vendor created successfully")
                .data(vendorService.create(request)).build();
    }

    @PutMapping("/{id}")
    public CommonResponse<VendorResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateVendorRequest request) {
        return CommonResponse.<VendorResponse>builder().success(true)
                .message("Vendor updated successfully")
                .data(vendorService.update(id, request)).build();
    }

    @GetMapping
    public CommonResponse<List<VendorResponse>> search(@RequestParam(required = false) String name,
                                                         @RequestParam(required = false) String mobile,
                                                       @RequestParam(required = false) Boolean active) {

        return CommonResponse.<List<VendorResponse>>builder().success(true)
                .message("Vendors fetched successfully")
                .data(vendorService.search(name, mobile, active)).build();
    }

    @GetMapping("/{id}")
    public CommonResponse<VendorDetailsResponse> getById(@PathVariable UUID id) {
        return CommonResponse.<VendorDetailsResponse>builder().success(true)
                .message("Vendor fetched successfully")
                .data(vendorService.getById(id)).build();
    }

    @PatchMapping("/{id}/status")
    public CommonResponse<VendorResponse> toggleStatus(@PathVariable UUID id) {
        VendorResponse response = vendorService.toggleStatus(id);
        return CommonResponse.<VendorResponse>builder().success(true)
                .message(response.getActive() ? "Vendor activated successfully" : "Vendor deactivated successfully")
                .data(response).build();
    }

}