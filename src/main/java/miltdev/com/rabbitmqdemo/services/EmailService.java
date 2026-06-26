package miltdev.com.rabbitmqdemo.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import miltdev.com.rabbitmqdemo.entities.Invoice;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final TemplateEngine templateEngine;
    private final JavaMailSender mailSender;

    public boolean sendInvoicePdf(File pdf, Invoice invoice){
        Context context = new Context();
        context.setVariable("name", invoice.getCustomerName());
        String htmlBody = templateEngine.process("email-template", context);
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    true,
                    StandardCharsets.UTF_8.name()
            );
            helper.setTo(invoice.getCustomerEmail());
            helper.setSubject("Your invoice document");
            helper.setText(htmlBody, true);
            helper.addAttachment(pdf.getName(), pdf);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error("Failed to send email", e);
            return false;
        }
        return true;
    }
}
