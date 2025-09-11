package za.co.digitalcowboy.agents.domain.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConnectResponse {
    
    @JsonProperty("authorizationUrl")
    private String authorizationUrl;
    
    @JsonProperty("provider")
    private String provider;
    
    @JsonProperty("state")
    private String state;
    
    public ConnectResponse() {}
    
    public ConnectResponse(String authorizationUrl, String provider, String state) {
        this.authorizationUrl = authorizationUrl;
        this.provider = provider;
        this.state = state;
    }
    
    public ConnectResponse(String authorizationUrl, String provider) {
        this(authorizationUrl, provider, null);
    }
    
    public String getAuthorizationUrl() {
        return authorizationUrl;
    }
    
    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
}