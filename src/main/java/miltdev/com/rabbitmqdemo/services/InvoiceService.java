package miltdev.com.rabbitmqdemo.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import miltdev.com.rabbitmqdemo.entities.Invoice;
import miltdev.com.rabbitmqdemo.enums.InvoiceStatus;
import miltdev.com.rabbitmqdemo.exceptions.NotFoundException;
import miltdev.com.rabbitmqdemo.repositories.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public List<Invoice> getAllByPendingStatus() {
        return invoiceRepository.findAllByStatusIs(InvoiceStatus.PENDING);
    }

    @Transactional
    public void updateInvoiceStatus(List<Long> invoiceIds, InvoiceStatus invoiceStatus) {
        int updatedRowsCount = invoiceRepository.updateStatusByIdIn(invoiceIds, invoiceStatus);
        log.info("Updated {} invoices", updatedRowsCount);
    }

    public Invoice getById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found with id: " + invoiceId));
    }

    public void markInvoiceAsSent(Invoice invoice) {
        invoice.setStatus(InvoiceStatus.EMAIL_SENT);
        invoiceRepository.save(invoice);
    }
}
