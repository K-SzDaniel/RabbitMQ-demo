package miltdev.com.rabbitmqdemo.services;

import lombok.extern.slf4j.Slf4j;
import miltdev.com.rabbitmqdemo.enums.RetryType;
import miltdev.com.rabbitmqdemo.exceptions.RetryFailedException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.Callable;

@Service
@Slf4j
public class RetryService {

    public boolean retry(RetryType actionName, int maxAttempts, Duration delay, Callable<Boolean> action) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (Boolean.TRUE.equals(action.call())) {
                    return true;
                }

                log.warn("{} returned false, attempt {}/{}", actionName, attempt, maxAttempts);
            } catch (Exception e) {
                lastException = e;
                log.error("{} failed, attempt {}/{}", actionName, attempt, maxAttempts, e);
            }

            if (attempt < maxAttempts) {
                sleep(delay);
            }
        }

        throw new RetryFailedException(actionName + " failed after retries", lastException);
    }


    private void sleep(Duration delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RetryFailedException("Retry interrupted", e);
        }
    }
}

