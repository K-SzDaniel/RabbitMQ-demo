package miltdev.com.rabbitmqdemo.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import miltdev.com.rabbitmqdemo.entities.Invoice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceJobService {

    private final RabbitMQService rabbitMQService;
    private final InvoiceService invoiceService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void processInvoice() {
        List<Invoice> allByPendingStatus = invoiceService.getAllByPendingStatus();
        if (allByPendingStatus == null || !allByPendingStatus.isEmpty()) {
            log.info("No processable invoices found");
            return;
        }

    }
}
