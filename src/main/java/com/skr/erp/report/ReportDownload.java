package com.skr.erp.report;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Builds attachment responses for report downloads (shared by the report endpoints). */
public final class ReportDownload {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private ReportDownload() {
    }

    public static ResponseEntity<byte[]> pdf(byte[] body, String fileName) {
        return attachment(body, fileName, MediaType.APPLICATION_PDF);
    }

    public static ResponseEntity<byte[]> excel(byte[] body, String fileName) {
        return attachment(body, fileName, XLSX);
    }

    private static ResponseEntity<byte[]> attachment(byte[] body, String fileName, MediaType type) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(type).body(body);
    }
}
