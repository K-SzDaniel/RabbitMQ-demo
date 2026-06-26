package miltdev.com.rabbitmqdemo.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import miltdev.com.rabbitmqdemo.configs.RabbitMQConfig;
import miltdev.com.rabbitmqdemo.entities.Invoice;
import miltdev.com.rabbitmqdemo.enums.InvoiceStatus;
import miltdev.com.rabbitmqdemo.enums.RetryType;
import miltdev.com.rabbitmqdemo.services.EmailService;
import miltdev.com.rabbitmqdemo.services.InvoicePdfService;
import miltdev.com.rabbitmqdemo.services.InvoiceService;
import miltdev.com.rabbitmqdemo.services.RetryService;
import org.apache.commons.io.FileUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageConsumer {

    private static final int MAX_ATTEMPTS = 3;

    private final ObjectMapper objectMapper;
    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;
    private final EmailService emailService;
    private final RetryService retryService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void consumeMessage(String message) throws IOException {
        try {
            log.info("Received message: {}", message);
            List<Long> invoiceIds = objectMapper.readValue(message, new TypeReference<>() {
            });
            log.info("Received message with {} invoice IDs", invoiceIds.size());
            invoiceIds.forEach(invoiceId -> {
                Invoice invoice = invoiceService.getById(invoiceId);
                File invoicePdf = invoicePdfService.generateInvoicePdf(invoice);
                if (InvoiceStatus.EMAIL_SENT != invoice.getStatus()) {
                    boolean sent = emailService.sendInvoicePdf(invoicePdf, invoice);
                    if (sent) {
                        invoiceService.markInvoiceAsSent(invoice);
                    } else {
                        retryService.retry(RetryType.EMAIL_SENDING, MAX_ATTEMPTS, Duration.ofMinutes(5),
                                () -> emailService.sendInvoicePdf(invoicePdf, invoice));
                        invoiceService.markInvoiceAsSent(invoice);
                    }
                }
            });
        } finally {
            FileUtils.cleanDirectory(new File("generated-invoices"));
        }
    }
}
