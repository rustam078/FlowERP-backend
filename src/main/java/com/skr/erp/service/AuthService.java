package com.skr.erp.service;

import com.skr.erp.dto.request.ChangePasswordRequest;
import com.skr.erp.dto.request.LoginRequest;
import com.skr.erp.dto.response.LoginResponse;
import com.skr.erp.entity.AppUser;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.exception.InvalidCredentialsException;
import com.skr.erp.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;

    public LoginResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByUsername(request.getUsername())
                     .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new InvalidCredentialsException("User is inactive");
        }

        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole()).build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {

        // Use BusinessException (409) not InvalidCredentialsException (401) so a wrong current
        // password doesn't trip the axios 401-logout interceptor.
        AppUser user = appUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!user.getPassword().equals(request.getCurrentPassword())) {
            throw new BusinessException("Current password is incorrect");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 4) {
            throw new BusinessException("New password must be at least 4 characters.");
        }

        user.setPassword(request.getNewPassword());
        appUserRepository.save(user);
    }
}