package com.skr.erp.pdf;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;

/**
 * Renders an XHTML string (a stored template with placeholders already filled)
 * into a PDF byte array using OpenHTMLtoPDF. Bundles a Unicode font so symbols
 * like the rupee sign (₹) render instead of showing as tofu/#.
 */
public class HtmlToPdfGenerator {

    private HtmlToPdfGenerator() {
    }

    public static byte[] render(String xhtml) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerFonts(builder);
            builder.withHtmlContent(xhtml, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render PDF from HTML template", e);
        }
    }

    // Register Noto Sans (which includes the rupee glyph) under the families the templates use,
    // so no template change is needed and every currency symbol renders.
    private static void registerFonts(PdfRendererBuilder builder) {
        for (String family : new String[]{"Helvetica", "Arial", "sans-serif"}) {
            builder.useFont(() -> HtmlToPdfGenerator.class.getResourceAsStream("/fonts/NotoSans-Regular.ttf"),
                    family, 400, FontStyle.NORMAL, true);
            builder.useFont(() -> HtmlToPdfGenerator.class.getResourceAsStream("/fonts/NotoSans-Bold.ttf"),
                    family, 700, FontStyle.NORMAL, true);
        }
    }
}
