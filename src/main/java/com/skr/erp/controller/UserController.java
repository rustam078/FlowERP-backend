package com.skr.erp.controller;

import com.skr.erp.dto.request.CreateUserRequest;
import com.skr.erp.dto.request.ResetPasswordRequest;
import com.skr.erp.dto.request.UpdateUserRequest;
import com.skr.erp.dto.response.UserResponse;
import com.skr.erp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// User management. Gated as the "settings" module by PermissionInterceptor, so ADMIN-only in practice.
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponse> getAll() {
        return userService.getAll();
    }

    @PostMapping
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @PatchMapping("/{id}/password")
    public UserResponse resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        return userService.resetPassword(id, request);
    }

    @PatchMapping("/{id}/status")
    public UserResponse toggleStatus(@PathVariable UUID id) {
        return userService.toggleStatus(id);
    }
}
