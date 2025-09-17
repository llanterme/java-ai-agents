package za.co.digitalcowboy.agents.domain.social;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LinkedInPostRequest {
    
    @JsonProperty("author")
    private String author;
    
    @JsonProperty("commentary")
    private String commentary;
    
    @JsonProperty("content")
    private Content content;
    
    @JsonProperty("distribution")
    private Distribution distribution;
    
    public LinkedInPostRequest() {}
    
    public LinkedInPostRequest(String author, String commentary, String imageUrn) {
        this.author = author;
        this.commentary = commentary;
        this.content = new Content(imageUrn);
        this.distribution = new Distribution();
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
    
    public Content getContent() {
        return content;
    }
    
    public void setContent(Content content) {
        this.content = content;
    }
    
    public Distribution getDistribution() {
        return distribution;
    }
    
    public void setDistribution(Distribution distribution) {
        this.distribution = distribution;
    }
    
    public static class Content {
        @JsonProperty("media")
        private Media media;
        
        public Content() {}
        
        public Content(String imageUrn) {
            this.media = new Media(imageUrn);
        }
        
        public Media getMedia() {
            return media;
        }
        
        public void setMedia(Media media) {
            this.media = media;
        }
        
        public static class Media {
            @JsonProperty("id")
            private String id;
            
            public Media() {}
            
            public Media(String id) {
                this.id = id;
            }
            
            public String getId() {
                return id;
            }
            
            public void setId(String id) {
                this.id = id;
            }
        }
    }
    
    public static class Distribution {
        @JsonProperty("feedDistribution")
        private String feedDistribution = "MAIN_FEED";
        
        public Distribution() {}
        
        public String getFeedDistribution() {
            return feedDistribution;
        }
        
        public void setFeedDistribution(String feedDistribution) {
            this.feedDistribution = feedDistribution;
        }
    }
}