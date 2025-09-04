package za.co.digitalcowboy.agents.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record GenerationTask(
    @JsonProperty("id")
    String id,
    
    @JsonProperty("request")
    TopicRequest request,
    
    @JsonProperty("status")
    TaskStatus status,
    
    @JsonProperty("result")
    OrchestrationResult result,
    
    @JsonProperty("error")
    String error,
    
    @JsonProperty("createdAt")
    LocalDateTime createdAt,
    
    @JsonProperty("updatedAt")
    LocalDateTime updatedAt,
    
    @JsonProperty("completedAt")
    LocalDateTime completedAt
) {
    public GenerationTask(String id, TopicRequest request) {
        this(id, request, TaskStatus.PENDING, null, null, 
             LocalDateTime.now(), LocalDateTime.now(), null);
    }
    
    public GenerationTask withStatus(TaskStatus newStatus) {
        return new GenerationTask(id, request, newStatus, result, error, 
                                createdAt, LocalDateTime.now(), completedAt);
    }
    
    public GenerationTask withResult(OrchestrationResult newResult) {
        return new GenerationTask(id, request, TaskStatus.COMPLETED, newResult, error,
                                createdAt, LocalDateTime.now(), LocalDateTime.now());
    }
    
    public GenerationTask withError(String newError) {
        return new GenerationTask(id, request, TaskStatus.FAILED, result, newError,
                                createdAt, LocalDateTime.now(), LocalDateTime.now());
    }
    
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED || status == TaskStatus.FAILED;
    }
}