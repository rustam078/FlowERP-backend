package com.skr.erp.report;

import com.skr.erp.entity.SystemSetting;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.pdf.HtmlToPdfGenerator;
import com.skr.erp.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/** Shared report helpers: company header from settings, value formatting, HTML template → PDF. */
@Component
@RequiredArgsConstructor
public class ReportSupport {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");

    private static final long SYMBOL_TTL_MS = 30_000;

    private final SystemSettingRepository systemSettingRepository;
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("en", "IN"));
    // Currency symbol is read for every money() cell; cache it briefly so a big report doesn't fire
    // one system_setting query per row (was an N+1 that stalled large Excel/PDF reports).
    private volatile String cachedSymbol;
    private volatile long cachedSymbolAt;

    // Live system-setting value, or the fallback when missing/blank.
    public String setting(String key, String fallback) {
        return systemSettingRepository.findBySettingKey(key).map(SystemSetting::getSettingValue)
                .filter(s -> s != null && !s.isBlank()).orElse(fallback);
    }

    // Escape dynamic values so the filled template stays valid XHTML for the renderer.
    public String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // Currency symbol from settings (falls back to Rs.); cached for a few seconds to avoid a
    // per-row DB hit while a report formats thousands of amounts.
    public String currencySymbol() {
        long now = System.currentTimeMillis();
        if (cachedSymbol == null || now - cachedSymbolAt > SYMBOL_TTL_MS) {
            cachedSymbol = setting("CURRENCY_SYMBOL", "Rs.");
            cachedSymbolAt = now;
        }
        return cachedSymbol;
    }

    public String money(BigDecimal v) {
        return currencySymbol() + " " + numberFormat.format(v == null ? BigDecimal.ZERO : v);
    }

    public String num(Number v) {
        return numberFormat.format(v == null ? 0 : v);
    }

    public String companyName() {
        return setting("COMPANY_NAME", "SKR Garment");
    }

    public String companyLine() {
        return setting("COMPANY_ADDRESS", "") + " | " + setting("COMPANY_CONTACT", "") + " | GSTIN " + setting("COMPANY_GSTIN", "-");
    }

    // Load the named template, fill the company header + the given placeholders, render to PDF.
    public byte[] renderPdf(String templateKey, Map<String, String> values) {
        String template = systemSettingRepository.findBySettingKey(templateKey).map(SystemSetting::getSettingValue)
                .orElseThrow(() -> new BusinessException("Report template not configured: " + templateKey));
        String html = applyCompany(template);
        for (Map.Entry<String, String> e : values.entrySet()) {
            html = html.replace("{{" + e.getKey() + "}}", e.getValue());
        }
        return HtmlToPdfGenerator.render(html);
    }

    private String applyCompany(String template) {
        return template.replace("{{companyName}}", esc(companyName()))
                .replace("{{companyAddress}}", esc(setting("COMPANY_ADDRESS", "")))
                .replace("{{companyContact}}", esc(setting("COMPANY_CONTACT", "")))
                .replace("{{companyGstin}}", esc(setting("COMPANY_GSTIN", "-")))
                .replace("{{generatedOn}}", LocalDateTime.now().format(STAMP));
    }
}
