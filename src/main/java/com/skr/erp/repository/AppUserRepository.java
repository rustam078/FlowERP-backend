package com.skr.erp.repository;

import com.skr.erp.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    // Login lookup with the portfolio loaded (one query).
    @Query("SELECT u FROM AppUser u LEFT JOIN FETCH u.portfolio WHERE LOWER(u.email) = LOWER(:email)")
    Optional<AppUser> findByEmailWithPortfolio(@Param("email") String email);

    boolean existsByEmailIgnoreCase(String email);

    List<AppUser> findAllByPortfolioIdOrderByCreatedAtDesc(UUID portfolioId);

    Optional<AppUser> findByIdAndPortfolioId(UUID id, UUID portfolioId);

    // User count per portfolio in one query (portfolio list).
    @Query("SELECT u.portfolio.id, COUNT(u) FROM AppUser u WHERE u.portfolio IS NOT NULL GROUP BY u.portfolio.id")
    List<Object[]> countPerPortfolio();

    // Active admins of a portfolio other than the given user (last-admin lockout guard).
    long countByPortfolioIdAndRoleAndActiveTrueAndIdNot(UUID portfolioId, String role, UUID excludingId);
}
