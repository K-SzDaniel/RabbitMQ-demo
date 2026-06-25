package miltdev.com.rabbitmqdemo.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import miltdev.com.rabbitmqdemo.configs.RabbitMQConfig;
import miltdev.com.rabbitmqdemo.entities.Invoice;
import miltdev.com.rabbitmqdemo.exceptions.NotFoundException;
import miltdev.com.rabbitmqdemo.exceptions.PdfGenerateException;
import miltdev.com.rabbitmqdemo.services.InvoicePdfService;
import miltdev.com.rabbitmqdemo.services.InvoiceService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageConsumer {

    private final ObjectMapper objectMapper;
    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void consumeMessage(String message) {
        log.info("Received message: {}", message);
        List<Long> invoiceIds = objectMapper.readValue(message, new TypeReference<>() {
        });
        log.info("Received message with {} invoice IDs", invoiceIds.size());
        invoiceIds.forEach(invoiceId -> {
            try {
                Invoice invoice = invoiceService.getById(invoiceId);
                invoicePdfService.generateInvoicePdf(invoice);
            } catch (NotFoundException e) {
                log.error("Invoice not found", e);
            } catch (PdfGenerateException e) {
                log.error("Failed to generate invoice PDF", e);
            }
        });
    }
}
