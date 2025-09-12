package za.co.digitalcowboy.agents.domain.oauth;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import za.co.digitalcowboy.agents.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "connected_accounts")
public class ConnectedAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;
    
    @Convert(converter = OAuthProviderConverter.class)
    @Column(nullable = false)
    @NotNull
    private OAuthProvider provider;
    
    @Column(name = "provider_user_id")
    private String providerUserId;
    
    @Column(name = "provider_username")
    private String providerUsername;
    
    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String accessToken;  // Will be encrypted
    
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken; // Will be encrypted
    
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;
    
    @Column(columnDefinition = "TEXT")
    private String scopes; // JSON array stored as string
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectionStatus status = ConnectionStatus.ACTIVE;
    
    @Column(name = "connected_at", nullable = false, updatable = false)
    private LocalDateTime connectedAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        connectedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public ConnectedAccount() {}
    
    public ConnectedAccount(User user, OAuthProvider provider, String accessToken) {
        this.user = user;
        this.provider = provider;
        this.accessToken = accessToken;
        this.status = ConnectionStatus.ACTIVE;
    }
    
    // Helper methods for scopes
    public List<String> getScopesList() {
        if (scopes == null || scopes.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            // First try to parse as JSON array
            ObjectMapper mapper = new ObjectMapper();
            List<String> scopeList = mapper.readValue(scopes, new TypeReference<List<String>>() {});
            
            // Handle case where we have a JSON array with a single comma-separated string element
            if (scopeList.size() == 1 && scopeList.get(0).contains(",")) {
                String singleElement = scopeList.get(0);
                return Arrays.asList(singleElement.split("[,\\s]+"));
            }
            
            return scopeList;
        } catch (Exception e) {
            // If that fails, treat as comma or space-separated string
            String cleanScopes = scopes.replace("[", "").replace("]", "").replace("\"", "");
            return Arrays.asList(cleanScopes.split("[,\\s]+"));
        }
    }
    
    public void setScopesList(List<String> scopesList) {
        if (scopesList == null || scopesList.isEmpty()) {
            this.scopes = null;
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.scopes = mapper.writeValueAsString(scopesList);
        } catch (Exception e) {
            this.scopes = null;
        }
    }
    
    public boolean isExpired() {
        return tokenExpiresAt != null && tokenExpiresAt.isBefore(LocalDateTime.now());
    }
    
    public boolean isActive() {
        return status == ConnectionStatus.ACTIVE && !isExpired();
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public OAuthProvider getProvider() {
        return provider;
    }
    
    public void setProvider(OAuthProvider provider) {
        this.provider = provider;
    }
    
    public String getProviderUserId() {
        return providerUserId;
    }
    
    public void setProviderUserId(String providerUserId) {
        this.providerUserId = providerUserId;
    }
    
    public String getProviderUsername() {
        return providerUsername;
    }
    
    public void setProviderUsername(String providerUsername) {
        this.providerUsername = providerUsername;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public LocalDateTime getTokenExpiresAt() {
        return tokenExpiresAt;
    }
    
    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }
    
    public String getScopes() {
        return scopes;
    }
    
    public void setScopes(String scopes) {
        this.scopes = scopes;
    }
    
    public ConnectionStatus getStatus() {
        return status;
    }
    
    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}