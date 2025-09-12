package za.co.digitalcowboy.agents.domain.social;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LinkedInPostResponse {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("state")
    private String state;
    
    @JsonProperty("author")
    private String author;
    
    @JsonProperty("commentary")
    private String commentary;
    
    public LinkedInPostResponse() {}
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getCommentary() {
        return commentary;
    }
    
    public void setCommentary(String commentary) {
        this.commentary = commentary;
    }
    
    public String getLinkedInPostUrl() {
        if (id != null && id.contains("activity:")) {
            // Extract activity ID from URN format: urn:li:activity:1234567890
            String activityId = id.substring(id.lastIndexOf(':') + 1);
            return "https://www.linkedin.com/posts/activity-" + activityId;
        }
        return null;
    }
}