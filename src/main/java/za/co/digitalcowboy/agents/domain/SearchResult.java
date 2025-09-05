package za.co.digitalcowboy.agents.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SearchResult(
    @JsonProperty("title")
    String title,
    
    @JsonProperty("snippet")
    String snippet,
    
    @JsonProperty("link")
    String link,
    
    @JsonProperty("displayLink")
    String displayLink,
    
    @JsonProperty("date")
    String date,
    
    @JsonProperty("position")
    Integer position
) {
    public SearchResult {
        if (title == null) title = "";
        if (snippet == null) snippet = "";
        if (link == null) link = "";
        if (displayLink == null) displayLink = "";
        if (date == null) date = "";
        if (position == null) position = 0;
    }
    
    public static SearchResult of(String title, String snippet, String link) {
        return new SearchResult(title, snippet, link, "", "", 0);
    }
    
    public String toFormattedText() {
        StringBuilder sb = new StringBuilder();
        if (!title.isEmpty()) {
            sb.append(title);
        }
        if (!snippet.isEmpty()) {
            if (sb.length() > 0) sb.append(": ");
            sb.append(snippet);
        }
        if (!link.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append("[").append(link).append("]");
        }
        return sb.toString();
    }
}