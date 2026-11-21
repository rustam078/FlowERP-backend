package com.skr.erp.util;

import com.skr.erp.repository.PortfolioRepository;
import com.skr.erp.security.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Per-portfolio document numbers from the portfolio's own counters. Each number is taken in one
 * locked UPDATE, so simultaneous saves never get the same number, and a rolled-back save gives
 * its number back (no gaps).
 */
@Component
@RequiredArgsConstructor
public class DocumentNumberGenerator {

    private static final String INVOICE_FORMAT = "INV-%06d";

    private final PortfolioRepository portfolioRepository;

    // Locked 2-letter prefix + counter: IL001, IL002 ... IL1000.
    public String nextBatchNumber() {
        return portfolioRepository.nextBatchNumber(RequestContext.portfolioId());
    }

    public String nextSaleInvoiceNumber() {
        return String.format(INVOICE_FORMAT, portfolioRepository.nextSaleInvoiceSeq(RequestContext.portfolioId()));
    }

    public String nextPurchaseInvoiceNumber() {
        return String.format(INVOICE_FORMAT, portfolioRepository.nextPurchaseInvoiceSeq(RequestContext.portfolioId()));
    }
}
