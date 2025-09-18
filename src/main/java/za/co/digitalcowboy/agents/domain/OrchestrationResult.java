package za.co.digitalcowboy.agents.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record OrchestrationResult(
    @JsonProperty("topic")
    String topic,

    @JsonProperty("research")
    ResearchPoints research,

    @JsonProperty("content")
    ContentDraft content,

    @JsonProperty("image")
    ImageResult image,

    @JsonProperty("id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Long id
) {
    // Constructor without ID for backward compatibility
    public OrchestrationResult(String topic, ResearchPoints research, ContentDraft content, ImageResult image) {
        this(topic, research, content, image, null);
    }

    public static OrchestrationResult empty(String topic) {
        return new OrchestrationResult(
            topic,
            ResearchPoints.empty(),
            ContentDraft.empty(),
            ImageResult.empty(),
            null
        );
    }

    public OrchestrationResult withId(Long id) {
        return new OrchestrationResult(topic, research, content, image, id);
    }
}