package za.co.digitalcowboy.agents.domain;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
    String message,
    int status,
    LocalDateTime timestamp,
    Map<String, String> details
) {
    // Constructor for simple error without details
    public ErrorResponse(String message, int status) {
        this(message, status, LocalDateTime.now(), null);
    }
    
    // Constructor with timestamp but no details
    public ErrorResponse(String message, int status, LocalDateTime timestamp) {
        this(message, status, timestamp, null);
    }
}