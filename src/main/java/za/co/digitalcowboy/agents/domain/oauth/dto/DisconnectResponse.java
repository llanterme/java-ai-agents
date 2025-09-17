package za.co.digitalcowboy.agents.domain.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DisconnectResponse {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("provider")
    private String provider;
    
    @JsonProperty("message")
    private String message;
    
    public DisconnectResponse() {}
    
    public DisconnectResponse(boolean success, String provider, String message) {
        this.success = success;
        this.provider = provider;
        this.message = message;
    }
    
    public static DisconnectResponse success(String provider) {
        return new DisconnectResponse(true, provider, "Successfully disconnected from " + provider);
    }
    
    public static DisconnectResponse error(String provider, String message) {
        return new DisconnectResponse(false, provider, message);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}