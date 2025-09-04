package za.co.digitalcowboy.agents.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ContentDraft(
    @JsonProperty("platform")
    String platform,
    
    @JsonProperty("tone")
    String tone,
    
    @JsonProperty("headline")
    String headline,
    
    @JsonProperty("body")
    String body,
    
    @JsonProperty("cta")
    String cta
) {
    public static ContentDraft empty() {
        return new ContentDraft("", "", "", "", "");
    }
}