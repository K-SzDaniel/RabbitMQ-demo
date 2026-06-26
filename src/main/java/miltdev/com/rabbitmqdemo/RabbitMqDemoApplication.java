package miltdev.com.rabbitmqdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RabbitMqDemoApplication {

   public static void main(String[] args) {
        SpringApplication.run(RabbitMqDemoApplication.class, args);
    }

}
