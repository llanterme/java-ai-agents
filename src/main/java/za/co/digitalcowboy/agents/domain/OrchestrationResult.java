package za.co.digitalcowboy.agents.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrchestrationResult(
    @JsonProperty("topic")
    String topic,
    
    @JsonProperty("research")
    ResearchPoints research,
    
    @JsonProperty("content")
    ContentDraft content,
    
    @JsonProperty("image")
    ImageResult image
) {
    public static OrchestrationResult empty(String topic) {
        return new OrchestrationResult(
            topic, 
            ResearchPoints.empty(), 
            ContentDraft.empty(), 
            ImageResult.empty()
        );
    }
}