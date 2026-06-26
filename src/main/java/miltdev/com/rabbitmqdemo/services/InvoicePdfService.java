package miltdev.com.rabbitmqdemo.services;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import miltdev.com.rabbitmqdemo.entities.Invoice;
import miltdev.com.rabbitmqdemo.exceptions.PdfGenerateException;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private final TemplateEngine templateEngine;

    public File generateInvoicePdf(Invoice invoice) {
        try {
            Context context = new Context();
            context.setVariable("invoice", invoice);

            String html = templateEngine.process("invoice", context);

            Path outputDir = Path.of("generated-invoices");
            Files.createDirectories(outputDir);

            Path pdfPath = outputDir.resolve("invoice-" + invoice.getId() + ".pdf");

            try (OutputStream outputStream = Files.newOutputStream(pdfPath)) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(html, null);
                builder.toStream(outputStream);
                builder.run();
            }

            return pdfPath.toFile();
        } catch (Exception e) {
            throw new PdfGenerateException(e);
        }
    }
}