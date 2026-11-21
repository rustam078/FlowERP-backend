package com.skr.erp.dto.request;

import lombok.Data;

import java.util.UUID;

/** SUPER_ADMIN only: the portfolio to work in next (null = leave and go back to the picker). */
@Data
public class SwitchPortfolioRequest {

    private UUID portfolioId;
}
