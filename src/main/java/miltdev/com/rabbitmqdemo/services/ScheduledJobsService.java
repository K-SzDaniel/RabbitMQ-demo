package miltdev.com.rabbitmqdemo.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduledJobsService {

    private final RabbitMQService rabbitMQService;

    @Scheduled(cron = "0/5 * * * * ?")
    public void scheduledJob() {
        rabbitMQService.sendMessage("Scheduled job executed");
    }

}
