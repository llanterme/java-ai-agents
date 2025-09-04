package za.co.digitalcowboy.agents.agents;

import za.co.digitalcowboy.agents.domain.ResearchPoints;
import za.co.digitalcowboy.agents.prompts.ResearchPrompt;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ResearchAgent {
    
    private static final Logger log = LoggerFactory.getLogger(ResearchAgent.class);
    
    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper;
    private final Timer researchTimer;
    
    public ResearchAgent(ChatLanguageModel chatModel, ObjectMapper objectMapper, Timer researchAgentTimer) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.researchTimer = researchAgentTimer;
    }
    
    public ResearchPoints research(String topic) {
        try {
            return researchTimer.recordCallable(() -> {
                log.debug("Starting research for topic: {}", topic);
                
                try {
                    String userPrompt = ResearchPrompt.formatUserPrompt(topic);
                    String fullPrompt = ResearchPrompt.SYSTEM_MESSAGE + "\n\nUser: " + userPrompt;
                    
                    log.debug("Sending prompt to LLM");
                    String response = chatModel.generate(fullPrompt);
                    log.debug("Received LLM response: {}", response);
                    
                    // Clean response - extract JSON if wrapped in markdown or other text
                    String cleanedResponse = extractJsonFromResponse(response);
                    
                    ResearchPoints result = objectMapper.readValue(cleanedResponse, ResearchPoints.class);
                    
                    // Validate constraints
                    if (result.points().size() < 5 || result.points().size() > 7) {
                        log.warn("Research points count {} is outside expected range 5-7", result.points().size());
                    }
                    
                    for (String point : result.points()) {
                        if (point.split("\\s+").length > 25) {
                            log.warn("Research point exceeds 25 words: {}", point);
                        }
                    }
                    
                    log.debug("Research completed successfully with {} points", result.points().size());
                    return result;
                    
                } catch (Exception e) {
                    log.error("Error during research", e);
                    // Return fallback result
                    return new ResearchPoints(
                        List.of("Unable to complete research for the topic: " + topic),
                        List.of()
                    );
                }
            });
        } catch (Exception e) {
            log.error("Timer execution error during research", e);
            return new ResearchPoints(
                List.of("Unable to complete research for the topic: " + topic),
                List.of()
            );
        }
    }
    
    private String extractJsonFromResponse(String response) {
        // Find JSON object boundaries
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }
        
        // If no JSON found, return original response
        return response.trim();
    }
}