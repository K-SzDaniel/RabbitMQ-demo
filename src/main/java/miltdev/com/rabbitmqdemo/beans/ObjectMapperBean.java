package miltdev.com.rabbitmqdemo.beans;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ObjectMapperBean {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
