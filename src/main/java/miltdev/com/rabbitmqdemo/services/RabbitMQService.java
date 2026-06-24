package miltdev.com.rabbitmqdemo.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import miltdev.com.rabbitmqdemo.configs.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQService {

    private final RabbitTemplate rabbitTemplate;

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, message);
        log.info("Message sent to RabbitMQ: {}", message);
    }
}
