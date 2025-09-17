package za.co.digitalcowboy.agents.service.oauth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface OAuthProviderService {
    
    String getAuthorizationUrl(String state, String redirectUri);
    
    TokenResponse exchangeCodeForToken(String code, String redirectUri);
    
    TokenResponse refreshToken(String refreshToken);
    
    void revokeToken(String token);
    
    UserInfo getUserInfo(String accessToken);
    
    boolean isTokenExpired(LocalDateTime expiresAt);
    
    List<String> getRequiredScopes();
    
    String getProviderName();
    
    class TokenResponse {
        private final String accessToken;
        private final String refreshToken;
        private final LocalDateTime expiresAt;
        private final List<String> scopes;
        
        public TokenResponse(String accessToken, String refreshToken, LocalDateTime expiresAt, List<String> scopes) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresAt = expiresAt;
            this.scopes = scopes;
        }
        
        public String getAccessToken() {
            return accessToken;
        }
        
        public String getRefreshToken() {
            return refreshToken;
        }
        
        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }
        
        public List<String> getScopes() {
            return scopes;
        }
    }
    
    class UserInfo {
        private final String id;
        private final String username;
        private final String email;
        private final String name;
        private final Map<String, Object> additionalInfo;
        
        public UserInfo(String id, String username, String email, String name, Map<String, Object> additionalInfo) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.name = name;
            this.additionalInfo = additionalInfo;
        }
        
        public String getId() {
            return id;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getEmail() {
            return email;
        }
        
        public String getName() {
            return name;
        }
        
        public Map<String, Object> getAdditionalInfo() {
            return additionalInfo;
        }
    }
}