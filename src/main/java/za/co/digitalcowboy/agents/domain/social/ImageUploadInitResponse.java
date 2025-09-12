package za.co.digitalcowboy.agents.domain.social;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ImageUploadInitResponse {
    
    @JsonProperty("value")
    private UploadValue value;
    
    public ImageUploadInitResponse() {}
    
    public UploadValue getValue() {
        return value;
    }
    
    public void setValue(UploadValue value) {
        this.value = value;
    }
    
    public static class UploadValue {
        @JsonProperty("image")
        private String imageUrn;
        
        @JsonProperty("uploadUrl")
        private String uploadUrl;
        
        public UploadValue() {}
        
        public String getImageUrn() {
            return imageUrn;
        }
        
        public void setImageUrn(String imageUrn) {
            this.imageUrn = imageUrn;
        }
        
        public String getUploadUrl() {
            return uploadUrl;
        }
        
        public void setUploadUrl(String uploadUrl) {
            this.uploadUrl = uploadUrl;
        }
    }
}