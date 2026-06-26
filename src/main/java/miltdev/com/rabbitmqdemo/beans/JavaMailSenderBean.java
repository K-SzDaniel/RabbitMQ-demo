package miltdev.com.rabbitmqdemo.beans;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class JavaMailSenderBean {

    @Value("${spring.mail.host}")
    private String host;
    @Value("${spring.mail.port}")
    private int port;
    @Value("${spring.mail.properties.mail.transport.protocol:smtp}")
    private String protocol;
    @Value("${spring.mail.properties.mail.smtp.auth:false}")
    private String smtpAuth;
    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private String startTlsEnabled;
    @Value("${spring.mail.properties.mail.debug:false}")
    private String debug;

    @Bean
    public JavaMailSender mailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(host);
        mailSender.setPort(port);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", protocol);
        props.put("mail.smtp.auth", smtpAuth);
        props.put("mail.smtp.starttls.enable", startTlsEnabled);
        props.put("mail.debug", debug);

        return mailSender;
    }
}
