package za.co.digitalcowboy.agents.tools;

import za.co.digitalcowboy.agents.domain.SearchResult;
import za.co.digitalcowboy.agents.domain.WebSearchResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SerpApiSearchService {
    
    private static final Logger log = LoggerFactory.getLogger(SerpApiSearchService.class);
    
    @Value("${serpapi.api-key:}")
    private String apiKey;
    
    @Value("${serpapi.search-engine:google}")
    private String searchEngine;
    
    @Value("${serpapi.location:United States}")
    private String location;
    
    @Value("${serpapi.max-results:5}")
    private int maxResults;
    
    @Value("${serpapi.enabled:true}")
    private boolean enabled;
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    public SerpApiSearchService() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
    }
    
    @Cacheable(value = "webSearchCache", key = "#query")
    public WebSearchResponse search(String query) {
        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            log.warn("SERP API is not configured or disabled. Returning empty results.");
            return WebSearchResponse.empty(query);
        }
        
        try {
            log.debug("Searching web for query: {}", query);
            
            HttpUrl url = buildSearchUrl(query);
            Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("SERP API request failed with status: {}", response.code());
                    return WebSearchResponse.empty(query);
                }
                
                String responseBody = response.body().string();
                JsonObject results = gson.fromJson(responseBody, JsonObject.class);
                
                return parseSearchResults(query, results);
            }
            
        } catch (Exception e) {
            log.error("Error performing web search for query: {}", query, e);
            return WebSearchResponse.empty(query);
        }
    }
    
    private HttpUrl buildSearchUrl(String query) {
        return HttpUrl.parse("https://serpapi.com/search")
            .newBuilder()
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("engine", searchEngine)
            .addQueryParameter("q", query)
            .addQueryParameter("location", location)
            .addQueryParameter("hl", "en")
            .addQueryParameter("gl", "us")
            .addQueryParameter("num", String.valueOf(maxResults))
            .build();
    }
    
    public List<WebSearchResponse> searchMultiple(List<String> queries) {
        return queries.stream()
            .map(this::search)
            .toList();
    }
    
    private WebSearchResponse parseSearchResults(String query, JsonObject results) {
        List<SearchResult> searchResults = new ArrayList<>();
        Map<String, Object> knowledgeGraph = new HashMap<>();
        Long totalResults = 0L;
        Double searchTime = 0.0;
        
        try {
            // Parse organic results
            if (results.has("organic_results")) {
                JsonArray organicResults = results.getAsJsonArray("organic_results");
                for (int i = 0; i < Math.min(organicResults.size(), maxResults); i++) {
                    JsonObject result = organicResults.get(i).getAsJsonObject();
                    
                    String title = getJsonString(result, "title");
                    String snippet = getJsonString(result, "snippet");
                    String link = getJsonString(result, "link");
                    String displayLink = getJsonString(result, "displayed_link");
                    String date = getJsonString(result, "date");
                    Integer position = i + 1;
                    
                    searchResults.add(new SearchResult(title, snippet, link, displayLink, date, position));
                }
            }
            
            // Parse knowledge graph if available
            if (results.has("knowledge_graph")) {
                JsonObject kg = results.getAsJsonObject("knowledge_graph");
                if (kg.has("title")) {
                    knowledgeGraph.put("title", kg.get("title").getAsString());
                }
                if (kg.has("description")) {
                    knowledgeGraph.put("description", kg.get("description").getAsString());
                }
                if (kg.has("source")) {
                    JsonObject source = kg.getAsJsonObject("source");
                    if (source.has("link")) {
                        knowledgeGraph.put("sourceLink", source.get("link").getAsString());
                    }
                }
            }
            
            // Parse search metadata
            if (results.has("search_information")) {
                JsonObject searchInfo = results.getAsJsonObject("search_information");
                if (searchInfo.has("total_results")) {
                    totalResults = searchInfo.get("total_results").getAsLong();
                }
                if (searchInfo.has("time_taken_displayed")) {
                    String timeStr = searchInfo.get("time_taken_displayed").getAsString();
                    // Extract numeric value from string like "0.45 seconds"
                    try {
                        searchTime = Double.parseDouble(timeStr.replaceAll("[^0-9.]", ""));
                    } catch (NumberFormatException e) {
                        searchTime = 0.0;
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error parsing search results", e);
        }
        
        log.debug("Parsed {} search results for query: {}", searchResults.size(), query);
        return new WebSearchResponse(query, searchResults, knowledgeGraph, totalResults, searchTime);
    }
    
    private String getJsonString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            JsonElement element = obj.get(key);
            if (element.isJsonPrimitive()) {
                return element.getAsString();
            }
        }
        return "";
    }
    
    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isEmpty();
    }
}