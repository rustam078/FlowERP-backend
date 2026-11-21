package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/** One portfolio in the super admin list. */
@Data
@Builder
public class PortfolioResponse {
    private UUID id;
    private String name;
    private String batchPrefix;
    private Boolean active;
    private Boolean defaultPortfolio; // the seeded ILITY portfolio: cannot be deleted
    private long userCount;
    private LocalDateTime createdAt;
}
