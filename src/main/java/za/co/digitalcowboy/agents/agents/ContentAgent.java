package za.co.digitalcowboy.agents.agents;

import za.co.digitalcowboy.agents.domain.ContentDraft;
import za.co.digitalcowboy.agents.domain.ResearchPoints;
import za.co.digitalcowboy.agents.prompts.ContentPrompt;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ContentAgent {
    
    private static final Logger log = LoggerFactory.getLogger(ContentAgent.class);
    
    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper;
    private final Timer contentTimer;
    
    public ContentAgent(ChatLanguageModel chatModel, ObjectMapper objectMapper, Timer contentAgentTimer) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.contentTimer = contentAgentTimer;
    }
    
    public ContentDraft createContent(ResearchPoints research, String platform, String tone) {
        try {
            return contentTimer.recordCallable(() -> {
            log.debug("Creating {} content with {} tone", platform, tone);
            
            try {
                String userPrompt = ContentPrompt.formatUserPrompt(research, platform, tone);
                String fullPrompt = ContentPrompt.SYSTEM_MESSAGE + "\n\nUser: " + userPrompt;
                
                log.debug("Sending content prompt to LLM");
                String response = chatModel.generate(fullPrompt);
                log.debug("Received content response: {}", response);
                
                String cleanedResponse = extractJsonFromResponse(response);
                ContentDraft result = objectMapper.readValue(cleanedResponse, ContentDraft.class);
                
                // Platform-specific validation
                validatePlatformConstraints(result, platform);
                
                log.debug("Content creation completed for platform: {}", platform);
                return result;
                
            } catch (Exception e) {
                log.error("Error during content creation", e);
                // Return fallback content with platform/tone info preserved
                return new ContentDraft(
                    platform,
                    tone,
                    "Content Creation Error",
                    "Unable to generate content based on the research provided.",
                    "Please try again."
                );
            }
        });
        } catch (Exception e) {
            log.error("Timer execution error during content creation", e);
            return new ContentDraft(
                platform,
                tone,
                "Content Creation Error",
                "Unable to generate content based on the research provided.",
                "Please try again."
            );
        }
    }
    
    private void validatePlatformConstraints(ContentDraft content, String platform) {
        switch (platform.toLowerCase()) {
            case "twitter" -> {
                int totalLength = (content.headline() + " " + content.body() + " " + content.cta()).length();
                if (totalLength > 280) {
                    log.warn("Twitter content exceeds 280 characters: {} chars", totalLength);
                }
            }
            case "blog" -> {
                int wordCount = content.body().split("\\s+").length;
                if (wordCount < 300 || wordCount > 500) {
                    log.warn("Blog content word count {} is outside expected range 300-500", wordCount);
                }
            }
            case "linkedin" -> {
                String[] paragraphs = content.body().split("\n\n");
                if (paragraphs.length < 3 || paragraphs.length > 5) {
                    log.warn("LinkedIn content has {} paragraphs, expected 3-5", paragraphs.length);
                }
            }
            case "instagram" -> {
                if (!content.body().contains("\n")) {
                    log.warn("Instagram content should contain line breaks for better formatting");
                }
            }
        }
    }
    
    private String extractJsonFromResponse(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }
        
        return response.trim();
    }
}