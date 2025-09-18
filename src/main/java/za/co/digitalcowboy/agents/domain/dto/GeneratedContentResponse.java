package za.co.digitalcowboy.agents.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import za.co.digitalcowboy.agents.domain.GeneratedContent;

import java.time.LocalDateTime;
import java.util.List;

public record GeneratedContentResponse(
    @JsonProperty("id")
    Long id,

    @JsonProperty("topic")
    String topic,

    @JsonProperty("platform")
    String platform,

    @JsonProperty("tone")
    String tone,

    @JsonProperty("imageCount")
    Integer imageCount,

    @JsonProperty("research")
    ResearchData research,

    @JsonProperty("content")
    ContentData content,

    @JsonProperty("image")
    ImageData image,

    @JsonProperty("createdAt")
    LocalDateTime createdAt,

    @JsonProperty("updatedAt")
    LocalDateTime updatedAt
) {

    public record ResearchData(
        @JsonProperty("topic")
        String topic,

        @JsonProperty("insights")
        List<String> insights
    ) {}

    public record ContentData(
        @JsonProperty("platform")
        String platform,

        @JsonProperty("tone")
        String tone,

        @JsonProperty("headline")
        String headline,

        @JsonProperty("body")
        String body,

        @JsonProperty("cta")
        String cta,

        @JsonProperty("hashtags")
        List<String> hashtags
    ) {}

    public record ImageData(
        @JsonProperty("prompt")
        String prompt,

        @JsonProperty("openAiImageUrls")
        List<String> openAiImageUrls,

        @JsonProperty("localImagePaths")
        List<String> localImagePaths,

        @JsonProperty("localImageUrls")
        List<String> localImageUrls
    ) {}

    public static GeneratedContentResponse from(GeneratedContent entity) {
        ResearchData research = new ResearchData(
            entity.getTopic(),
            entity.getResearchPoints()
        );

        ContentData content = new ContentData(
            entity.getPlatform(),
            entity.getTone(),
            entity.getContentHeadline(),
            entity.getContentBody(),
            entity.getContentCta(),
            entity.getContentHashtags()
        );

        ImageData image = new ImageData(
            entity.getImagePrompt(),
            entity.getImageOpenAiUrls(),
            entity.getImageLocalPaths(),
            entity.getImageLocalUrls()
        );

        return new GeneratedContentResponse(
            entity.getId(),
            entity.getTopic(),
            entity.getPlatform(),
            entity.getTone(),
            entity.getImageCount(),
            research,
            content,
            image,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}