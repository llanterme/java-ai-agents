package za.co.digitalcowboy.agents.domain.social;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ImageUploadInitRequest {
    
    @JsonProperty("initializeUploadRequest")
    private InitializeUploadRequest initializeUploadRequest;
    
    public ImageUploadInitRequest() {}
    
    public ImageUploadInitRequest(String owner) {
        this.initializeUploadRequest = new InitializeUploadRequest(owner);
    }
    
    public InitializeUploadRequest getInitializeUploadRequest() {
        return initializeUploadRequest;
    }
    
    public void setInitializeUploadRequest(InitializeUploadRequest initializeUploadRequest) {
        this.initializeUploadRequest = initializeUploadRequest;
    }
    
    public static class InitializeUploadRequest {
        @JsonProperty("owner")
        private String owner;
        
        public InitializeUploadRequest() {}
        
        public InitializeUploadRequest(String owner) {
            this.owner = owner;
        }
        
        public String getOwner() {
            return owner;
        }
        
        public void setOwner(String owner) {
            this.owner = owner;
        }
    }
}