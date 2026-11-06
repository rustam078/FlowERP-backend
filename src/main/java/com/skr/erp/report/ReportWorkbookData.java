package com.skr.erp.report;

import java.util.List;

/** Everything one Excel report needs: header, period, summary pairs and titled sections. */
public record ReportWorkbookData(
        String title,
        String period,
        String companyName,
        String companyLine,
        List<String[]> summary,
        List<ReportSection> sections) {
}
