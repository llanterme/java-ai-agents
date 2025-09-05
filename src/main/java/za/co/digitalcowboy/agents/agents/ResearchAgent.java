package za.co.digitalcowboy.agents.agents;

import za.co.digitalcowboy.agents.domain.ResearchPoints;
import za.co.digitalcowboy.agents.domain.WebSearchResponse;
import za.co.digitalcowboy.agents.prompts.ResearchPrompt;
import za.co.digitalcowboy.agents.tools.SerpApiSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ResearchAgent {
    
    private static final Logger log = LoggerFactory.getLogger(ResearchAgent.class);
    
    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper;
    private final Timer researchTimer;
    private final SerpApiSearchService searchService;
    
    @Autowired
    public ResearchAgent(ChatLanguageModel chatModel, ObjectMapper objectMapper, 
                         Timer researchAgentTimer, SerpApiSearchService searchService) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.researchTimer = researchAgentTimer;
        this.searchService = searchService;
    }
    
    public ResearchPoints research(String topic) {
        try {
            return researchTimer.recordCallable(() -> {
                log.debug("Starting research for topic: {}", topic);
                
                try {
                    // Check if web search is enabled
                    if (searchService.isEnabled()) {
                        return researchWithWebSearch(topic);
                    } else {
                        return researchWithoutWebSearch(topic);
                    }
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
    
    private ResearchPoints researchWithWebSearch(String topic) throws Exception {
        log.debug("Performing research with web search for topic: {}", topic);
        
        // Step 1: Generate search queries
        String queryPrompt = ResearchPrompt.formatQueryGenerationPrompt(topic);
        String queryResponse = chatModel.generate(queryPrompt);
        List<String> queries = Arrays.stream(queryResponse.split("\n"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .limit(3)
            .collect(Collectors.toList());
        
        if (queries.isEmpty()) {
            queries = List.of(topic, topic + " latest news", topic + " facts statistics");
        }
        
        log.debug("Generated {} search queries for topic: {}", queries.size(), topic);
        
        // Step 2: Perform web searches
        List<WebSearchResponse> searchResponses = searchService.searchMultiple(queries);
        
        // Step 3: Combine search results
        StringBuilder searchResults = new StringBuilder();
        List<String> allSources = new ArrayList<>();
        
        for (WebSearchResponse response : searchResponses) {
            searchResults.append(response.toSummaryText()).append("\n\n");
            allSources.addAll(response.extractSources());
        }
        
        // Step 4: Generate research points with web search context
        String userPrompt = ResearchPrompt.formatUserPromptWithSearch(topic, searchResults.toString());
        String fullPrompt = ResearchPrompt.SYSTEM_MESSAGE_WITH_WEB_SEARCH + "\n\nUser: " + userPrompt;
        
        log.debug("Sending prompt to LLM with web search context");
        String response = chatModel.generate(fullPrompt);
        log.debug("Received LLM response with web search context");
        
        // Clean and parse response
        String cleanedResponse = extractJsonFromResponse(response);
        ResearchPoints result = objectMapper.readValue(cleanedResponse, ResearchPoints.class);
        
        // Add web sources to the result if not already present
        List<String> finalSources = new ArrayList<>(result.sources());
        for (String source : allSources) {
            if (!finalSources.contains(source)) {
                finalSources.add(source);
            }
        }
        
        // Limit sources to top 5
        if (finalSources.size() > 5) {
            finalSources = finalSources.subList(0, 5);
        }
        
        ResearchPoints enrichedResult = new ResearchPoints(result.points(), finalSources);
        
        // Validate constraints
        validateResearchPoints(enrichedResult);
        
        log.debug("Research with web search completed successfully with {} points and {} sources", 
                 enrichedResult.points().size(), enrichedResult.sources().size());
        return enrichedResult;
    }
    
    private ResearchPoints researchWithoutWebSearch(String topic) throws Exception {
        log.debug("Performing research without web search for topic: {}", topic);
        
        String userPrompt = ResearchPrompt.formatUserPrompt(topic);
        String fullPrompt = ResearchPrompt.SYSTEM_MESSAGE + "\n\nUser: " + userPrompt;
        
        log.debug("Sending prompt to LLM");
        String response = chatModel.generate(fullPrompt);
        log.debug("Received LLM response: {}", response);
        
        // Clean response - extract JSON if wrapped in markdown or other text
        String cleanedResponse = extractJsonFromResponse(response);
        
        ResearchPoints result = objectMapper.readValue(cleanedResponse, ResearchPoints.class);
        
        // Validate constraints
        validateResearchPoints(result);
        
        log.debug("Research completed successfully with {} points", result.points().size());
        return result;
    }
    
    private void validateResearchPoints(ResearchPoints result) {
        if (result.points().size() < 5 || result.points().size() > 7) {
            log.warn("Research points count {} is outside expected range 5-7", result.points().size());
        }
        
        for (String point : result.points()) {
            if (point.split("\\s+").length > 25) {
                log.warn("Research point exceeds 25 words: {}", point);
            }
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