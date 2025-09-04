package za.co.digitalcowboy.agents.agents;

import za.co.digitalcowboy.agents.domain.ContentDraft;
import za.co.digitalcowboy.agents.domain.ImageBrief;
import za.co.digitalcowboy.agents.domain.ImageResult;
import za.co.digitalcowboy.agents.prompts.ImagePrompt;
import za.co.digitalcowboy.agents.tools.OpenAiImageTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ImageAgent {
    
    private static final Logger log = LoggerFactory.getLogger(ImageAgent.class);
    
    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper;
    private final OpenAiImageTool imageTool;
    private final Timer imageTimer;
    
    public ImageAgent(ChatLanguageModel chatModel, ObjectMapper objectMapper, 
                     OpenAiImageTool imageTool, Timer imageAgentTimer) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.imageTool = imageTool;
        this.imageTimer = imageAgentTimer;
    }
    
    public ImageResult generateImage(ContentDraft content, int imageCount, String topic) {
        try {
            return imageTimer.recordCallable(() -> {
            log.debug("Generating {} image(s) for content", imageCount);
            
            try {
                // Step 1: Generate image prompt from content
                ImageBrief brief = generateImageBrief(content);
                log.debug("Generated image brief: {}", brief.prompt());
                
                // Step 2: Generate actual images using OpenAI Images API
                ImageResult result = imageTool.generateImage(brief.prompt(), imageCount, topic);
                
                log.debug("Image generation completed with {} URLs", result.openAiImageUrls().size());
                log.debug("Image generation completed with {} local files", result.localImagePaths().size());
                return result;
                
            } catch (Exception e) {
                log.error("Error during image generation", e);
                // Return fallback result
                return new ImageResult(
                    "Image generation failed for content: " + content.headline(),
                    List.of()
                );
            }
        });
        } catch (Exception e) {
            log.error("Timer execution error during image generation", e);
            return new ImageResult(
                "Image generation failed for content: " + content.headline(),
                List.of()
            );
        }
    }
    
    private ImageBrief generateImageBrief(ContentDraft content) {
        try {
            String userPrompt = ImagePrompt.formatUserPrompt(content);
            String fullPrompt = ImagePrompt.SYSTEM_MESSAGE + "\n\nUser: " + userPrompt;
            
            log.debug("Generating image prompt from content");
            String response = chatModel.generate(fullPrompt);
            log.debug("Received image prompt response: {}", response);
            
            String cleanedResponse = extractJsonFromResponse(response);
            ImageBrief brief = objectMapper.readValue(cleanedResponse, ImageBrief.class);
            
            // Validate prompt is not empty
            if (brief.prompt() == null || brief.prompt().trim().isEmpty()) {
                log.warn("Generated image prompt is empty, using fallback");
                return new ImageBrief("Abstract illustration related to: " + content.headline());
            }
            
            return brief;
            
        } catch (Exception e) {
            log.error("Error generating image brief", e);
            // Return fallback brief
            return new ImageBrief("Professional illustration for: " + content.headline());
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