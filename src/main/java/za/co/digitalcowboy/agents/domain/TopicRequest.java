package za.co.digitalcowboy.agents.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TopicRequest(
    @NotBlank(message = "Topic is required")
    String topic,
    
    @NotNull(message = "Platform is required")
    @Pattern(regexp = "^(twitter|linkedin|instagram|blog)$", 
             message = "Platform must be one of: twitter, linkedin, instagram, blog")
    String platform,
    
    @NotNull(message = "Tone is required")
    @Pattern(regexp = "^(professional|casual|playful|authoritative)$", 
             message = "Tone must be one of: professional, casual, playful, authoritative")
    String tone,
    
    @Positive(message = "Image count must be positive")
    int imageCount
) {
    @JsonCreator
    public TopicRequest(
        @JsonProperty("topic") String topic,
        @JsonProperty("platform") String platform,
        @JsonProperty("tone") String tone,
        @JsonProperty("imageCount") Integer imageCount
    ) {
        this(topic, platform, tone, imageCount != null ? imageCount : 1);
    }
}