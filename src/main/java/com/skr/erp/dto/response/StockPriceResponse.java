package com.skr.erp.dto.response;

import java.math.BigDecimal;

/** One step of a product's in-stock prices in selling order: this price applies to this many pieces. */
public record StockPriceResponse(BigDecimal price, BigDecimal quantity) {
}
