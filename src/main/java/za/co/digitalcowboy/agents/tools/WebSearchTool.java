package za.co.digitalcowboy.agents.tools;

import za.co.digitalcowboy.agents.domain.WebSearchResponse;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WebSearchTool {
    
    private static final Logger log = LoggerFactory.getLogger(WebSearchTool.class);
    
    private final SerpApiSearchService searchService;
    
    public WebSearchTool(SerpApiSearchService searchService) {
        this.searchService = searchService;
    }
    
    @Tool("Search the web for current information about a topic")
    public String searchWeb(String query) {
        log.debug("WebSearchTool invoked with query: {}", query);
        
        try {
            WebSearchResponse response = searchService.search(query);
            
            if (response.results().isEmpty()) {
                return "No search results found for: " + query;
            }
            
            return response.toSummaryText();
            
        } catch (Exception e) {
            log.error("Error in WebSearchTool", e);
            return "Error performing web search: " + e.getMessage();
        }
    }
    
    @Tool("Generate effective search queries for a topic")
    public String[] generateSearchQueries(String topic, int numberOfQueries) {
        log.debug("Generating {} search queries for topic: {}", numberOfQueries, topic);
        
        // Basic query variations - in production, this could use LLM to generate better queries
        String[] queries = new String[Math.min(numberOfQueries, 3)];
        
        if (queries.length >= 1) {
            queries[0] = topic + " latest news " + java.time.Year.now().getValue();
        }
        if (queries.length >= 2) {
            queries[1] = topic + " statistics facts data";
        }
        if (queries.length >= 3) {
            queries[2] = "what is " + topic + " current state";
        }
        
        return queries;
    }
}