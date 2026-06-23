package miltdev.com.rabbitmqdemo.consumer;

import lombok.extern.slf4j.Slf4j;
import miltdev.com.rabbitmqdemo.configs.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MessageConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void consumeMessage(String message) {
        log.info("Received message: {}", message);
    }
}
