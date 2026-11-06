package com.skr.erp.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import com.skr.erp.dto.response.ProductionDetailsResponse;
import com.skr.erp.dto.response.ProductionItemResponse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ProductionPdfGenerator {

    public static byte[] generate(ProductionDetailsResponse response) {

        try {

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4, 30, 30, 30, 30);

            PdfWriter.getInstance(document, outputStream);

            document.open();

            addHeader(document);

            addInfo(document, response);

            addTable(document, response);

            addSummary(document, response);

            addFooter(document);

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException("Unable to generate pdf", e);
        }
    }

    private static void addHeader(Document document) throws Exception {

        try {

            InputStream is = ProductionPdfGenerator.class.getResourceAsStream("/static/logo.png");

            if (is != null) {

                byte[] bytes = is.readAllBytes();

                Image logo = Image.getInstance(bytes);

                logo.scaleToFit(80, 80);

                logo.setAlignment(Image.ALIGN_CENTER);

                document.add(logo);
            }

        } catch (Exception ignored) {
        }

        Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD);

        Paragraph title = new Paragraph("SKR GARMENT ERP", titleFont);

        title.setAlignment(Element.ALIGN_CENTER);

        document.add(title);

        Paragraph reportTitle = new Paragraph("Production Report", new Font(Font.HELVETICA, 14, Font.BOLD));

        reportTitle.setSpacingBefore(10);
        reportTitle.setSpacingAfter(20);

        reportTitle.setAlignment(Element.ALIGN_CENTER);

        document.add(reportTitle);
    }

    private static void addInfo(Document document, ProductionDetailsResponse response) throws Exception {

        PdfPTable table = new PdfPTable(2);

        table.setWidthPercentage(100);

        table.addCell("Date");
        table.addCell(response.getProductionDate().toString());

        table.addCell("Employee");
        table.addCell(response.getEmployeeName());

        document.add(table);

        document.add(new Paragraph(" "));
    }

    private static void addTable(Document document, ProductionDetailsResponse response) throws Exception {

        PdfPTable table = new PdfPTable(4);

        table.setWidthPercentage(100);

        table.addCell("Product");
        table.addCell("Quantity");
        table.addCell("Rate");
        table.addCell("Amount");

        for (ProductionItemResponse item : response.getItems()) {

            table.addCell(item.getProductName());

            table.addCell(String.valueOf(item.getQuantity()));

            table.addCell("₹" + item.getRate());

            table.addCell("₹" + item.getAmount());
        }

        document.add(table);
    }

    private static void addSummary(Document document, ProductionDetailsResponse response) throws Exception {

        document.add(new Paragraph(" "));

        Paragraph summary = new Paragraph("Total Products : " + response.getProductCount() + "\nTotal Quantity : " + response.getTotalQuantity() + "\nTotal Amount : ₹" + response.getTotalAmount());

        summary.setSpacingBefore(10);

        document.add(summary);
    }

    private static void addFooter(Document document) throws Exception {

        document.add(new Paragraph(" "));

        Paragraph footer = new Paragraph("Generated On : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a")));

        footer.setAlignment(Element.ALIGN_RIGHT);

        document.add(footer);
    }
}