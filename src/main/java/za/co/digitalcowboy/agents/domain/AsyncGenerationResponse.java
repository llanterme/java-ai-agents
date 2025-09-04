package za.co.digitalcowboy.agents.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AsyncGenerationResponse(
    @JsonProperty("taskId")
    String taskId,
    
    @JsonProperty("status")
    TaskStatus status,
    
    @JsonProperty("statusUrl")
    String statusUrl,
    
    @JsonProperty("resultUrl")
    String resultUrl
) {
    public static AsyncGenerationResponse forTask(String taskId) {
        return new AsyncGenerationResponse(
            taskId,
            TaskStatus.PENDING,
            "/api/v1/generate/status/" + taskId,
            "/api/v1/generate/result/" + taskId
        );
    }
}