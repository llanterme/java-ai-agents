package za.co.digitalcowboy.agents.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ImageBrief(
    @JsonProperty("prompt")
    String prompt
) {
    public static ImageBrief empty() {
        return new ImageBrief("");
    }
}