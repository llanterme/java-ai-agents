package za.co.digitalcowboy.agents.domain.social.dto;

public class SocialPostResponse {
    private final String postId;
    private final String state;
    private final String postUrl;
    private final String message;
    
    public SocialPostResponse(String postId, String state, String postUrl, String message) {
        this.postId = postId;
        this.state = state;
        this.postUrl = postUrl;
        this.message = message;
    }
    
    public String getPostId() {
        return postId;
    }
    
    public String getState() {
        return state;
    }
    
    public String getPostUrl() {
        return postUrl;
    }
    
    public String getMessage() {
        return message;
    }
}