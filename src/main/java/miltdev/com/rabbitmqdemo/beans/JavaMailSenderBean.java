package miltdev.com.rabbitmqdemo.beans;

import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class JavaMailSenderBean {

    @Bean
    public JavaMailSender mailSender(){
       JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

       mailSender.setHost("localhost");
       mailSender.setPort(1025);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.debug", "false");

       return mailSender;
    }
}
