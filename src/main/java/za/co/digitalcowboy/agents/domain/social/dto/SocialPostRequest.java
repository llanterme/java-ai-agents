package za.co.digitalcowboy.agents.domain.social.dto;

import jakarta.validation.constraints.NotBlank;

public class SocialPostRequest {
    @NotBlank(message = "Text is required")
    private String text;
    
    private String imagePath; // Optional
    
    public SocialPostRequest() {}
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}