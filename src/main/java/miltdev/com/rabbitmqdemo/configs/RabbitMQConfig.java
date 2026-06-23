package miltdev.com.rabbitmqdemo.configs;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE = "demo.queue";
    public static final String EXCHANGE = "demo.exchange";
    public static final String ROUTING_KEY = "demo.routing.key";

    @Bean
    Queue demoQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    DirectExchange demoExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    Binding demoBinding(Queue demoQueue, DirectExchange demoExchange) {
        return BindingBuilder
                .bind(demoQueue)
                .to(demoExchange)
                .with(ROUTING_KEY);
    }
}
