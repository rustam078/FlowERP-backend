package com.skr.erp.service;

import com.skr.erp.common.constants.Role;
import com.skr.erp.dto.request.CreatePortfolioRequest;
import com.skr.erp.dto.response.PortfolioResponse;
import com.skr.erp.entity.AppUser;
import com.skr.erp.entity.Portfolio;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.AppUserRepository;
import com.skr.erp.repository.PortfolioRepository;
import com.skr.erp.repository.SystemSettingRepository;
import com.skr.erp.repository.UserSessionRepository;
import com.skr.erp.security.PasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** SUPER_ADMIN portfolio management: list, create (with first admin + default settings), (de)activate, delete. */
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final AppUserRepository appUserRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordService passwordService;

    @Transactional(readOnly = true)
    public List<PortfolioResponse> list(String search) {
        String pattern = search == null || search.isBlank() ? null : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        Map<UUID, Long> users = userCounts();
        return portfolioRepository.search(pattern).stream()
                .map(p -> toResponse(p, users.getOrDefault(p.getId(), 0L)))
                .toList();
    }

    // Live check while the super admin types the 2 letters.
    @Transactional(readOnly = true)
    public boolean isPrefixAvailable(String prefix) {
        String p = normalizePrefix(prefix);
        return p.matches("[A-Z]{2}") && !portfolioRepository.existsByBatchPrefix(p);
    }

    // New portfolio + its first ADMIN + a copy of the default settings, all in one transaction.
    @Transactional
    public PortfolioResponse create(CreatePortfolioRequest request) {
        String name = request.getName().trim();
        String prefix = normalizePrefix(request.getBatchPrefix());
        String email = request.getAdminEmail().trim().toLowerCase(Locale.ROOT);
        validateNew(name, prefix, email);
        Portfolio portfolio = new Portfolio();
        portfolio.setName(name);
        portfolio.setBatchPrefix(prefix); // locked from here on (column is not updatable)
        portfolioRepository.saveAndFlush(portfolio);
        createFirstAdmin(portfolio, request, email);
        systemSettingRepository.copySettings(Portfolio.DEFAULT_ID, portfolio.getId(), name);
        return toResponse(portfolio, 1);
    }

    // Deactivate: its users can't sign in and are signed out now. Activate: usable again.
    @Transactional
    public PortfolioResponse setActive(UUID id, boolean active) {
        Portfolio portfolio = getOrThrow(id);
        portfolio.setActive(active);
        if (!active) userSessionRepository.deleteAllByPortfolioId(id);
        return toResponse(portfolio, userCounts().getOrDefault(id, 0L));
    }

    // Soft delete (data kept, hidden, locked). The exact name must be typed to confirm.
    @Transactional
    public void delete(UUID id, String confirmName) {
        Portfolio portfolio = getOrThrow(id);
        if (portfolio.isDefault()) throw new BusinessException("The default portfolio cannot be deleted");
        if (confirmName == null || !portfolio.getName().equals(confirmName.trim())) {
            throw new BusinessException("Type the portfolio name exactly to delete it");
        }
        portfolio.setDeleted(true);
        portfolio.setActive(false);
        userSessionRepository.deleteAllByPortfolioId(id);
    }

    private void validateNew(String name, String prefix, String email) {
        if (!prefix.matches("[A-Z]{2}")) throw new BusinessException("Batch prefix must be exactly 2 letters");
        if (portfolioRepository.existsByBatchPrefix(prefix)) {
            throw new BusinessException("Batch prefix " + prefix + " is already used by another portfolio");
        }
        if (portfolioRepository.existsByNameIgnoreCaseAndDeletedFalse(name)) {
            throw new BusinessException("A portfolio with this name already exists");
        }
        if (appUserRepository.existsByEmailIgnoreCase(email)) throw new BusinessException("Admin email already in use");
    }

    private void createFirstAdmin(Portfolio portfolio, CreatePortfolioRequest request, String email) {
        AppUser admin = new AppUser();
        admin.setEmail(email);
        admin.setPassword(passwordService.hashNew(request.getAdminPassword()));
        admin.setFullName(request.getAdminFullName().trim());
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        admin.setPortfolio(portfolio);
        appUserRepository.save(admin);
    }

    private Portfolio getOrThrow(UUID id) {
        return portfolioRepository.findById(id).filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .orElseThrow(() -> new BusinessException("Portfolio not found"));
    }

    private Map<UUID, Long> userCounts() {
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : appUserRepository.countPerPortfolio()) counts.put((UUID) row[0], ((Number) row[1]).longValue());
        return counts;
    }

    private static String normalizePrefix(String prefix) {
        return prefix == null ? "" : prefix.trim().toUpperCase(Locale.ROOT);
    }

    private static PortfolioResponse toResponse(Portfolio p, long userCount) {
        return PortfolioResponse.builder()
                .id(p.getId()).name(p.getName()).batchPrefix(p.getBatchPrefix())
                .active(p.getActive()).defaultPortfolio(p.isDefault())
                .userCount(userCount).createdAt(p.getCreatedAt())
                .build();
    }
}
