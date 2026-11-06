package com.skr.erp.pdf;

import com.skr.erp.dto.response.ProductionDetailsResponse;
import com.skr.erp.dto.response.ProductionItemResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Builds a styled .xlsx export of a single production entry.
 */
public class ProductionExcelGenerator {

    private ProductionExcelGenerator() {
    }

    public static byte[] generate(ProductionDetailsResponse d) {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Production");
            sheet.setColumnWidth(0, 8000);
            sheet.setColumnWidth(1, 5000);
            sheet.setColumnWidth(2, 4000);
            sheet.setColumnWidth(3, 4000);
            sheet.setColumnWidth(4, 5000);

            // ── Styles ──
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(titleFont);

            Font labelFont = wb.createFont();
            labelFont.setBold(true);
            CellStyle labelStyle = wb.createCellStyle();
            labelStyle.setFont(labelFont);

            Font headFont = wb.createFont();
            headFont.setBold(true);
            headFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle headStyle = wb.createCellStyle();
            headStyle.setFont(headFont);
            headStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            CellStyle totalStyle = wb.createCellStyle();
            Font totalFont = wb.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);
            totalStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            int r = 0;
            Row title = sheet.createRow(r++);
            Cell tc = title.createCell(0);
            tc.setCellValue("SKR Garment ERP — Production Report");
            tc.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
            r++; // blank

            r = addInfo(sheet, r, labelStyle, "Ref ID", d.getId() != null ? d.getId().toString() : "");
            r = addInfo(sheet, r, labelStyle, "Production Date",
                    d.getProductionDate() != null ? d.getProductionDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")) : "");
            r = addInfo(sheet, r, labelStyle, "Employee", nn(d.getEmployeeName()));
            r++; // blank

            // Header row
            Row head = sheet.createRow(r++);
            String[] heads = {"Product", "Piece Code", "Quantity", "Rate", "Amount"};
            for (int c = 0; c < heads.length; c++) {
                Cell cell = head.createCell(c);
                cell.setCellValue(heads[c]);
                cell.setCellStyle(headStyle);
            }

            // Items
            if (d.getItems() != null) {
                for (ProductionItemResponse item : d.getItems()) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(nn(item.getProductName()));
                    row.createCell(1).setCellValue(nn(item.getPieceCode()));
                    row.createCell(2).setCellValue(item.getQuantity() != null ? item.getQuantity() : 0);
                    Cell rate = row.createCell(3);
                    rate.setCellValue(item.getRate() != null ? item.getRate().doubleValue() : 0);
                    rate.setCellStyle(moneyStyle);
                    Cell amt = row.createCell(4);
                    amt.setCellValue(item.getAmount() != null ? item.getAmount().doubleValue() : 0);
                    amt.setCellStyle(moneyStyle);
                }
            }

            // Totals
            Row totalRow = sheet.createRow(r++);
            Cell totLabel = totalRow.createCell(0);
            totLabel.setCellValue("Total");
            totLabel.setCellStyle(labelStyle);
            Cell totQty = totalRow.createCell(2);
            totQty.setCellValue(d.getTotalQuantity() != null ? d.getTotalQuantity() : 0);
            totQty.setCellStyle(labelStyle);
            Cell totAmt = totalRow.createCell(4);
            totAmt.setCellValue(d.getTotalAmount() != null ? d.getTotalAmount().doubleValue() : 0);
            totAmt.setCellStyle(totalStyle);

            wb.write(os);
            return os.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }

    private static int addInfo(Sheet sheet, int r, CellStyle labelStyle, String label, String value) {
        Row row = sheet.createRow(r);
        Cell l = row.createCell(0);
        l.setCellValue(label);
        l.setCellStyle(labelStyle);
        row.createCell(1).setCellValue(value);
        return r + 1;
    }

    private static String nn(String s) {
        return s == null ? "" : s;
    }

    private static String nn(BigDecimal b) {
        return b == null ? "0" : b.toPlainString();
    }
}
