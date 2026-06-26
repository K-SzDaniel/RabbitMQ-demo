package miltdev.com.rabbitmqdemo.exceptions;

public class RetryFailedException extends RuntimeException {
    public RetryFailedException(String message) {
        super(message);
    }

    public RetryFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
