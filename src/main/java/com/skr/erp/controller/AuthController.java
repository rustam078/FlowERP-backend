package com.skr.erp.controller;

import com.skr.erp.common.response.CommonResponse;
import com.skr.erp.dto.request.ChangePasswordRequest;
import com.skr.erp.dto.request.LoginRequest;
import com.skr.erp.dto.response.LoginResponse;
import com.skr.erp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public CommonResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return CommonResponse.<LoginResponse>builder()
                .success(true)
                .message("Login successful")
                .data(response)
                .build();
    }

    @PostMapping("/change-password")
    public CommonResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return CommonResponse.<Void>builder()
                .success(true)
                .message("Password changed successfully")
                .build();
    }
}