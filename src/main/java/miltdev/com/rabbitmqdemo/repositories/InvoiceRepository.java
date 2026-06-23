package miltdev.com.rabbitmqdemo.repositories;

import miltdev.com.rabbitmqdemo.entities.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findAllByStatusIs(String status);
}
