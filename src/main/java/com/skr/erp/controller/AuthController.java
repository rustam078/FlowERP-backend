package com.skr.erp.controller;

import com.skr.erp.audit.Audit;
import com.skr.erp.common.response.CommonResponse;
import com.skr.erp.dto.request.ChangePasswordRequest;
import com.skr.erp.dto.request.LoginRequest;
import com.skr.erp.dto.request.SwitchPortfolioRequest;
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

    @Audit("Login")
    @PostMapping("/login")
    public CommonResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return CommonResponse.<LoginResponse>builder()
                .success(true)
                .message("Login successful")
                .data(response)
                .build();
    }

    @PostMapping("/logout")
    public CommonResponse<Void> logout() {
        authService.logout();
        return CommonResponse.<Void>builder().success(true).message("Signed out").build();
    }

    // Current session: user + portfolio being worked in.
    @GetMapping("/me")
    public CommonResponse<LoginResponse> me() {
        return CommonResponse.<LoginResponse>builder().success(true).data(authService.me()).build();
    }

    // SUPER_ADMIN only: enter another portfolio (portfolioId null = back to the picker).
    @PostMapping("/switch-portfolio")
    public CommonResponse<LoginResponse> switchPortfolio(@RequestBody SwitchPortfolioRequest request) {
        return CommonResponse.<LoginResponse>builder().success(true).data(authService.switchPortfolio(request)).build();
    }

    @Audit("Own password changed")
    @PostMapping("/change-password")
    public CommonResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return CommonResponse.<Void>builder()
                .success(true)
                .message("Password changed successfully")
                .build();
    }
}