package com.skr.erp.report;

import java.util.List;

/** One titled block in an Excel report: a heading, column headers and data rows. */
public record ReportSection(String title, List<String> headers, List<List<String>> rows) {
}
