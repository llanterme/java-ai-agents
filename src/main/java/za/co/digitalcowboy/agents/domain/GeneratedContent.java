package za.co.digitalcowboy.agents.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "generated_content")
public class GeneratedContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(nullable = false, length = 50)
    private String platform;

    @Column(nullable = false, length = 50)
    private String tone;

    @Column(name = "image_count")
    private Integer imageCount = 1;

    // Research data
    @Column(name = "research_points", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> researchPoints;

    @Column(name = "research_sources", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> researchSources;

    // Content data
    @Column(name = "content_headline", length = 500)
    private String contentHeadline;

    @Column(name = "content_body", columnDefinition = "TEXT")
    private String contentBody;

    @Column(name = "content_cta", length = 500)
    private String contentCta;

    @Column(name = "content_hashtags", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> contentHashtags;

    // Image data
    @Column(name = "image_prompt", columnDefinition = "TEXT")
    private String imagePrompt;

    @Column(name = "image_openai_urls", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> imageOpenAiUrls;

    @Column(name = "image_local_paths", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> imageLocalPaths;

    @Column(name = "image_local_urls", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> imageLocalUrls;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public GeneratedContent() {}

    public GeneratedContent(User user, String topic, String platform, String tone, Integer imageCount) {
        this.user = user;
        this.topic = topic;
        this.platform = platform;
        this.tone = tone;
        this.imageCount = imageCount != null ? imageCount : 1;
    }

    // Static factory method from OrchestrationResult
    public static GeneratedContent fromOrchestrationResult(User user, TopicRequest request, OrchestrationResult result) {
        GeneratedContent content = new GeneratedContent(
            user,
            request.topic(),
            request.platform(),
            request.tone(),
            request.imageCount()
        );

        // Set research data
        if (result.research() != null) {
            if (result.research().points() != null) {
                content.setResearchPoints(result.research().points());
            }
            if (result.research().sources() != null) {
                content.setResearchSources(result.research().sources());
            }
        }

        // Set content data
        if (result.content() != null) {
            content.setContentHeadline(result.content().headline());
            content.setContentBody(result.content().body());
            content.setContentCta(result.content().cta());
            // Note: hashtags are not part of ContentDraft currently
        }

        // Set image data
        if (result.image() != null) {
            content.setImagePrompt(result.image().prompt());
            content.setImageOpenAiUrls(result.image().openAiImageUrls());
            content.setImageLocalPaths(result.image().localImagePaths());
            content.setImageLocalUrls(result.image().localImageUrls());
        }

        return content;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }

    public Integer getImageCount() {
        return imageCount;
    }

    public void setImageCount(Integer imageCount) {
        this.imageCount = imageCount;
    }

    public List<String> getResearchPoints() {
        return researchPoints;
    }

    public void setResearchPoints(List<String> researchPoints) {
        this.researchPoints = researchPoints;
    }

    public List<String> getResearchSources() {
        return researchSources;
    }

    public void setResearchSources(List<String> researchSources) {
        this.researchSources = researchSources;
    }

    public String getContentHeadline() {
        return contentHeadline;
    }

    public void setContentHeadline(String contentHeadline) {
        this.contentHeadline = contentHeadline;
    }

    public String getContentBody() {
        return contentBody;
    }

    public void setContentBody(String contentBody) {
        this.contentBody = contentBody;
    }

    public String getContentCta() {
        return contentCta;
    }

    public void setContentCta(String contentCta) {
        this.contentCta = contentCta;
    }

    public List<String> getContentHashtags() {
        return contentHashtags;
    }

    public void setContentHashtags(List<String> contentHashtags) {
        this.contentHashtags = contentHashtags;
    }

    public String getImagePrompt() {
        return imagePrompt;
    }

    public void setImagePrompt(String imagePrompt) {
        this.imagePrompt = imagePrompt;
    }

    public List<String> getImageOpenAiUrls() {
        return imageOpenAiUrls;
    }

    public void setImageOpenAiUrls(List<String> imageOpenAiUrls) {
        this.imageOpenAiUrls = imageOpenAiUrls;
    }

    public List<String> getImageLocalPaths() {
        return imageLocalPaths;
    }

    public void setImageLocalPaths(List<String> imageLocalPaths) {
        this.imageLocalPaths = imageLocalPaths;
    }

    public List<String> getImageLocalUrls() {
        return imageLocalUrls;
    }

    public void setImageLocalUrls(List<String> imageLocalUrls) {
        this.imageLocalUrls = imageLocalUrls;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}