package za.co.digitalcowboy.agents.domain.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import za.co.digitalcowboy.agents.domain.oauth.ConnectionStatus;

import java.time.LocalDateTime;
import java.util.List;

public class ConnectionStatusResponse {
    
    @JsonProperty("provider")
    private String provider;
    
    @JsonProperty("connected")
    private boolean connected;
    
    @JsonProperty("status")
    private ConnectionStatus status;
    
    @JsonProperty("providerUsername")
    private String providerUsername;
    
    @JsonProperty("scopes")
    private List<String> scopes;
    
    @JsonProperty("connectedAt")
    private LocalDateTime connectedAt;
    
    @JsonProperty("expiresAt")
    private LocalDateTime expiresAt;
    
    @JsonProperty("expired")
    private boolean expired;
    
    public ConnectionStatusResponse() {}
    
    public ConnectionStatusResponse(String provider, boolean connected, ConnectionStatus status) {
        this.provider = provider;
        this.connected = connected;
        this.status = status;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
    
    public ConnectionStatus getStatus() {
        return status;
    }
    
    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }
    
    public String getProviderUsername() {
        return providerUsername;
    }
    
    public void setProviderUsername(String providerUsername) {
        this.providerUsername = providerUsername;
    }
    
    public List<String> getScopes() {
        return scopes;
    }
    
    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
    
    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }
    
    public void setConnectedAt(LocalDateTime connectedAt) {
        this.connectedAt = connectedAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public boolean isExpired() {
        return expired;
    }
    
    public void setExpired(boolean expired) {
        this.expired = expired;
    }
}