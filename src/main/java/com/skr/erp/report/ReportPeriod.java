package com.skr.erp.report;

import com.skr.erp.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/** A validated from–to window for reports: at most one month, with labels and a file-name token. */
public class ReportPeriod {

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM_yyyy");
    private static final DateTimeFormatter TOKEN = DateTimeFormatter.ofPattern("ddMMMyyyy");

    private final LocalDate from;
    private final LocalDate to;

    private ReportPeriod(LocalDate from, LocalDate to) {
        this.from = from;
        this.to = to;
    }

    // Validates the range (both present, ordered, at most one month) before a report is built.
    public static ReportPeriod of(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException("Please select both From and To dates.");
        }
        if (to.isBefore(from)) {
            throw new BusinessException("'To' date must be on or after 'From' date.");
        }
        if (ChronoUnit.DAYS.between(from, to) > 90) {
            throw new BusinessException("Please select a range of at most 90 days.");
        }
        return new ReportPeriod(from, to);
    }

    public LocalDate from() { return from; }
    public LocalDate to() { return to; }
    public LocalDateTime start() { return from.atStartOfDay(); }
    public LocalDateTime endExclusive() { return to.plusDays(1).atStartOfDay(); }
    public String fromLabel() { return from.format(LABEL); }
    public String toLabel() { return to.format(LABEL); }
    public String label() { return fromLabel() + " to " + toLabel(); }

    // "Jan_2026" for a full calendar month, otherwise "01Jan2026_to_15Jan2026".
    public String fileToken() {
        if (from.getDayOfMonth() == 1 && to.equals(from.withDayOfMonth(from.lengthOfMonth()))) {
            return from.format(MONTH);
        }
        return from.format(TOKEN) + "_to_" + to.format(TOKEN);
    }
}
