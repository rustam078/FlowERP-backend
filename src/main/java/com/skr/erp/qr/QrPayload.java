package com.skr.erp.qr;

import com.skr.erp.exception.BusinessException;

import java.math.BigDecimal;

/**
 * QR content format: {@code SKR1|<serial>|<productName>|<price>}.
 * The serial is authoritative; the name/price make the tag self-describing.
 */
public final class QrPayload {

    public static final String PREFIX = "SKR1";
    private static final String SEP = "|";

    private QrPayload() {
    }

    public static String build(String serial, String productName, BigDecimal price) {
        // Keep the product name delimiter-safe so the payload always splits cleanly.
        String safeName = productName == null ? "" : productName.replace(SEP, "/");
        return String.join(SEP, PREFIX, serial, safeName, price.toPlainString());
    }

    /**
     * Extract the serial from a scanned code. Accepts either a full payload
     * ({@code SKR1|serial|...}) or a bare serial ({@code BT000087-001}).
     */
    public static String extractSerial(String scanned) {
        if (scanned == null || scanned.isBlank()) {
            throw new BusinessException("Empty scan.");
        }
        String code = scanned.trim();
        if (code.contains(SEP)) {
            String[] parts = code.split("\\" + SEP);
            if (parts.length < 2 || !PREFIX.equals(parts[0]) || parts[1].isBlank()) {
                throw new BusinessException("Unrecognised QR code.");
            }
            return parts[1].trim();
        }
        return code;
    }
}
