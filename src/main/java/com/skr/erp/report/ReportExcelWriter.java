package com.skr.erp.report;

import com.skr.erp.exception.BusinessException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.util.List;

/** Builds a single-sheet .xlsx report: company header, title, period, summary and titled sections. */
public final class ReportExcelWriter {

    // Streaming workbook keeps this many rows in memory and flushes the rest — handles huge reports.
    private static final int WINDOW = 200;
    private static final int COL_WIDTH = 22 * 256; // fixed width (chars) so we never scan all rows to auto-size.
    private static final int COLS = 8;

    private ReportExcelWriter() {
    }

    public static byte[] build(ReportWorkbookData data) {
        SXSSFWorkbook wb = new SXSSFWorkbook(WINDOW);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Report");
            for (int c = 0; c < COLS; c++) {
                sheet.setColumnWidth(c, COL_WIDTH);
            }
            CellStyle bold = boldStyle(wb);
            int r = 0;
            r = line(sheet, r, data.companyName(), bold);
            r = line(sheet, r, data.companyLine(), null);
            r = line(sheet, r, data.title() + "  (" + data.period() + ")", bold);
            r++;
            for (String[] pair : data.summary()) {
                r = row(sheet, r, List.of(pair[0], pair[1]), null);
            }
            for (ReportSection section : data.sections()) {
                r++;
                r = line(sheet, r, section.title(), bold);
                r = row(sheet, r, section.headers(), bold);
                for (List<String> dataRow : section.rows()) {
                    r = row(sheet, r, dataRow, null);
                }
            }
            wb.write(os);
            return os.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Failed to build Excel report: " + e.getMessage());
        } finally {
            wb.dispose(); // remove the streaming temp files
        }
    }

    private static int line(Sheet sheet, int r, String text, CellStyle style) {
        Cell cell = sheet.createRow(r).createCell(0);
        cell.setCellValue(text);
        if (style != null) cell.setCellStyle(style);
        return r + 1;
    }

    private static int row(Sheet sheet, int r, List<String> values, CellStyle style) {
        Row row = sheet.createRow(r);
        for (int c = 0; c < values.size(); c++) {
            Cell cell = row.createCell(c);
            cell.setCellValue(values.get(c));
            if (style != null) cell.setCellStyle(style);
        }
        return r + 1;
    }

    private static CellStyle boldStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        return style;
    }
}
