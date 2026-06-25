package miltdev.com.rabbitmqdemo.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import miltdev.com.rabbitmqdemo.entities.Invoice;
import miltdev.com.rabbitmqdemo.enums.InvoiceStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceJobService {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int BATCH_SIZE = 100;
    private static final int RETRY_INTERVAL_MINUTES = 1;

    private final RabbitMQService rabbitMQService;
    private final InvoiceService invoiceService;
    private final RedisLockService redisLockService;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 * * * * *")
    public void prepareInvoiceJobs() {
        String lockKey = "invoice-job-initialization";
        String uuid = UUID.randomUUID().toString();
        if (!redisLockService.acquireLock(lockKey, uuid, Duration.ofMinutes(30))) {
            log.info("Invoice job already running");
            return;
        }
        try {
            List<Invoice> invoices = invoiceService.getAllByPendingStatus();
            if (invoices == null || invoices.isEmpty()) {
                log.info("No processable invoices found");
                return;
            }
            log.info("Sending {} invoiceId to RabbitMQ", invoices.size());
            getInvoiceIds(invoices).forEach(this::sendRabbitMQMessage);
        } finally {
            redisLockService.releaseLock(lockKey, uuid);
        }
    }

    private void sendRabbitMQMessage(List<Long> invoiceIds) {
        try {
            if (sendMessage(invoiceIds) || retry(invoiceIds)) {
                invoiceService.updateInvoiceStatus(invoiceIds, InvoiceStatus.PROCESSING);
            }
        } catch (JacksonException e) {
            log.error("JacksonException occurred", e);
        }
    }

    private boolean sendMessage(List<Long> invoiceIds) {
        return rabbitMQService.sendMessage(objectMapper.writeValueAsString(invoiceIds));
    }

    private List<List<Long>> getInvoiceIds(List<Invoice> invoices) {
        List<List<Long>> invoiceIdLists = new ArrayList<>();
        for (int i = 0; i < invoices.size(); i += BATCH_SIZE) {
            invoiceIdLists.add(getInvoiceIds(invoices, i));
        }
        return invoiceIdLists;
    }

    private List<Long> getInvoiceIds(List<Invoice> invoices, int i) {
        List<Invoice> invoiceBatch = invoices.subList(i, (Math.min(i + BATCH_SIZE, invoices.size())));
        return invoiceBatch
                .stream()
                .map(Invoice::getId)
                .toList();
    }

    private boolean retry(List<Long> invoiceIds) {
        for (int i = 0; i < MAX_RETRY_ATTEMPTS; i++) {
            try {
                Thread.sleep(Duration.ofMinutes(RETRY_INTERVAL_MINUTES));
                if (sendMessage(invoiceIds)) {
                    return true;
                }
            } catch (InterruptedException ex) {
                log.error("RabbitMQ message retry failed", ex);
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
