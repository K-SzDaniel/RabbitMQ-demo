package miltdev.com.rabbitmqdemo.entities;

import jakarta.persistence.*;
import lombok.Data;
import miltdev.com.rabbitmqdemo.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String invoiceNumber;

    private String customerName;
    private String customerEmail;
    private String customerAddress;

    private LocalDate issueDate;
    private LocalDate dueDate;

    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    private String currency;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    private String pdfPath;
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime sentAt;
}
