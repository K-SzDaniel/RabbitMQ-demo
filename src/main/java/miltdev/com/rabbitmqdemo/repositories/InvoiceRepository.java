package miltdev.com.rabbitmqdemo.repositories;

import miltdev.com.rabbitmqdemo.entities.Invoice;
import miltdev.com.rabbitmqdemo.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findAllByStatusIs(InvoiceStatus status);

    @Modifying
    @Query("update Invoice i set i.status = :status where i.id in :ids")
    int updateStatusByIdIn(@Param("ids") List<Long> ids,
                           @Param("status") InvoiceStatus status);
}
