package miltdev.com.rabbitmqdemo.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import miltdev.com.rabbitmqdemo.entities.Invoice;
import miltdev.com.rabbitmqdemo.enums.InvoiceStatus;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
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

    private static final int BATCH_SIZE = 100;
    private final RabbitMQService rabbitMQService;
    private final InvoiceService invoiceService;
    private final RedisLockService redisLockService;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 * * * * *")
    public void prepareInvoiceJobs() throws InterruptedException {
        String lockKey = "invoice-job-initialization";
        String uuid = UUID.randomUUID().toString();
        if (!redisLockService.acquireLock(lockKey, uuid, Duration.ofMinutes(30))) {
            log.info("Invoice job already running");
            return;
        }
        List<Invoice> invoices = invoiceService.getAllByPendingStatus();
        if (invoices == null || invoices.isEmpty()) {
            log.info("No processable invoices found");
            return;
        }
        List<Long> invoiceIds = new ArrayList<>();
        if (invoices.size() >= BATCH_SIZE) {
            for (int i = 0; i < invoices.size(); i += BATCH_SIZE) {
                List<Invoice> invoiceBatch = invoices.subList(i, (Math.min(i + BATCH_SIZE, invoices.size())));
                invoiceBatch.forEach(invoice -> invoice.setStatus(InvoiceStatus.PROCESSING));
                invoiceIds.addAll(invoiceBatch
                        .stream()
                        .map(Invoice::getId)
                        .toList());
                log.info("Sending {} invoiceId to RabbitMQ", invoiceIds.size());
                String rabbitMQMessage = null;
                try {
                    rabbitMQMessage = objectMapper.writeValueAsString(invoiceIds);
                    rabbitMQService.sendMessage(rabbitMQMessage);
                    invoiceService.updateInvoiceStatus(invoiceIds, InvoiceStatus.PROCESSING);
                    invoiceIds.clear();
                } catch (JacksonException e) {
                    log.error("JacksonException occurred", e);
                } catch (AmqpException e) {
                    log.error("Message sending failed to RabbitMQ", e);
                    retry(rabbitMQMessage);
                }
            }
        } else {
            rabbitMQService.sendMessage(objectMapper.writeValueAsString(invoices
                    .stream()
                    .map(Invoice::getId)
                    .toList()));
        }
        redisLockService.releaseLock(lockKey, uuid);
    }

    private void retry(String rabbitMQMessage) throws InterruptedException {
        if (StringUtils.isBlank(rabbitMQMessage)) {
            log.error("RabbitMQ message is blank retry skipped");
            return;
        }
        for (int i = 0; i < 3; i++) {
            try {
                rabbitMQService.sendMessage(rabbitMQMessage);
                break;
            } catch (AmqpException e) {
                log.error("Message sending with retry failed to RabbitMQ attempts {}", i, e);
                Thread.sleep(1000);
            }
        }
    }
}
