package com.skr.erp.repository;

import com.skr.erp.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByUsername(String username);

    List<AppUser> findAllByOrderByCreatedAtDesc();

    boolean existsByUsernameIgnoreCase(String username);
}