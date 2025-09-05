package za.co.digitalcowboy.agents.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record WebSearchResponse(
    @JsonProperty("query")
    String query,
    
    @JsonProperty("results")
    List<SearchResult> results,
    
    @JsonProperty("knowledgeGraph")
    Map<String, Object> knowledgeGraph,
    
    @JsonProperty("totalResults")
    Long totalResults,
    
    @JsonProperty("searchTime")
    Double searchTime
) {
    public WebSearchResponse {
        if (query == null) query = "";
        if (results == null) results = List.of();
        if (knowledgeGraph == null) knowledgeGraph = Map.of();
        if (totalResults == null) totalResults = 0L;
        if (searchTime == null) searchTime = 0.0;
    }
    
    public static WebSearchResponse empty(String query) {
        return new WebSearchResponse(query, List.of(), Map.of(), 0L, 0.0);
    }
    
    public String toSummaryText() {
        if (results.isEmpty()) {
            return "No search results found for: " + query;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Search results for '").append(query).append("':\n\n");
        
        for (int i = 0; i < Math.min(results.size(), 5); i++) {
            SearchResult result = results.get(i);
            sb.append(i + 1).append(". ").append(result.toFormattedText()).append("\n\n");
        }
        
        return sb.toString();
    }
    
    public List<String> extractSources() {
        return results.stream()
            .map(SearchResult::link)
            .filter(link -> !link.isEmpty())
            .distinct()
            .limit(5)
            .toList();
    }
}