package miltdev.com.rabbitmqdemo.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import miltdev.com.rabbitmqdemo.entities.Invoice;
import miltdev.com.rabbitmqdemo.enums.InvoiceStatus;
import miltdev.com.rabbitmqdemo.repositories.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public List<Invoice> getAllByPendingStatus() {
        return invoiceRepository.findAllByStatusIs(InvoiceStatus.PENDING.name());
    }

    public void updateInvoiceStatus(List<Long> invoiceIds, InvoiceStatus invoiceStatus) {

    }
}
