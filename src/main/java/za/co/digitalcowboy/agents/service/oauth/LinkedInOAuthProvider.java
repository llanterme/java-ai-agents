package za.co.digitalcowboy.agents.service.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LinkedInOAuthProvider implements OAuthProviderService {
    
    private static final Logger logger = LoggerFactory.getLogger(LinkedInOAuthProvider.class);
    
    private static final String AUTHORIZATION_URL = "https://www.linkedin.com/oauth/v2/authorization";
    private static final String TOKEN_URL = "https://www.linkedin.com/oauth/v2/accessToken";
    private static final String USER_INFO_URL = "https://api.linkedin.com/v2/userinfo";
    
    @Value("${oauth.providers.linkedin.client-id}")
    private String clientId;
    
    @Value("${oauth.providers.linkedin.client-secret}")
    private String clientSecret;
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public LinkedInOAuthProvider() {
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getAuthorizationUrl(String state, String redirectUri) {
        List<String> scopes = getRequiredScopes();
        String scopeString = String.join("%20", scopes);
        
        return AUTHORIZATION_URL + "?" +
                "response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&state=" + state +
                "&scope=" + scopeString;
    }
    
    @Override
    public TokenResponse exchangeCodeForToken(String code, String redirectUri) {
        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", redirectUri)
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .build();
        
        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to exchange code for token: " + response.code() + " " + response.message());
            }
            
            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            String accessToken = jsonNode.get("access_token").asText();
            
            // DEBUG: Log LinkedIn token exchange response
            logger.debug("DEBUG: LinkedIn token exchange response - access_token length: {}, starts with: {}..., ends with: ...{}", 
                accessToken != null ? accessToken.length() : 0, 
                accessToken != null && accessToken.length() > 10 ? accessToken.substring(0, 5) : "null",
                accessToken != null && accessToken.length() > 10 ? accessToken.substring(accessToken.length() - 5) : "null");
            String refreshToken = jsonNode.has("refresh_token") ? jsonNode.get("refresh_token").asText() : null;
            
            // LinkedIn tokens typically expire in 60 days, but let's check the response
            long expiresInSeconds = jsonNode.has("expires_in") ? jsonNode.get("expires_in").asLong() : 5184000; // 60 days
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresInSeconds);
            
            String scopeString = jsonNode.has("scope") ? jsonNode.get("scope").asText() : String.join(" ", getRequiredScopes());
            List<String> scopes = Arrays.asList(scopeString.split("\\s+"));
            
            return new TokenResponse(accessToken, refreshToken, expiresAt, scopes);
            
        } catch (IOException e) {
            logger.error("Error exchanging code for token", e);
            throw new RuntimeException("Failed to exchange code for token", e);
        }
    }
    
    @Override
    public TokenResponse refreshToken(String refreshToken) {
        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh token is required");
        }
        
        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .build();
        
        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to refresh token: " + response.code() + " " + response.message());
            }
            
            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            String accessToken = jsonNode.get("access_token").asText();
            String newRefreshToken = jsonNode.has("refresh_token") ? jsonNode.get("refresh_token").asText() : refreshToken;
            
            long expiresInSeconds = jsonNode.has("expires_in") ? jsonNode.get("expires_in").asLong() : 5184000;
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresInSeconds);
            
            String scopeString = jsonNode.has("scope") ? jsonNode.get("scope").asText() : String.join(" ", getRequiredScopes());
            List<String> scopes = Arrays.asList(scopeString.split("\\s+"));
            
            return new TokenResponse(accessToken, newRefreshToken, expiresAt, scopes);
            
        } catch (IOException e) {
            logger.error("Error refreshing token", e);
            throw new RuntimeException("Failed to refresh token", e);
        }
    }
    
    @Override
    public void revokeToken(String token) {
        // LinkedIn doesn't have a standard token revocation endpoint
        // Tokens expire naturally or can be revoked from the user's LinkedIn settings
        logger.info("Token revocation requested for LinkedIn. Tokens will expire naturally.");
    }
    
    @Override
    public UserInfo getUserInfo(String accessToken) {
        Request profileRequest = new Request.Builder()
                .url(USER_INFO_URL)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("X-Restli-Protocol-Version", "2.0.0")
                .build();
        
        try (Response profileResponse = httpClient.newCall(profileRequest).execute()) {
            if (!profileResponse.isSuccessful()) {
                String errorBody = profileResponse.body() != null ? profileResponse.body().string() : "No response body";
                logger.error("LinkedIn profile API error: {} - {}", profileResponse.code(), errorBody);
                throw new RuntimeException("Failed to fetch user profile: " + profileResponse.code() + " - " + errorBody);
            }
            
            String profileBody = profileResponse.body().string();
            JsonNode profileNode = objectMapper.readTree(profileBody);
            
            // OpenID Connect userinfo response format
            String id = profileNode.has("sub") ? profileNode.get("sub").asText() : "";
            String name = profileNode.has("name") ? profileNode.get("name").asText() : "";
            String email = profileNode.has("email") ? profileNode.get("email").asText() : "";
            
            // Handle given_name and family_name if name is not available
            if (name.isEmpty()) {
                String givenName = profileNode.has("given_name") ? profileNode.get("given_name").asText() : "";
                String familyName = profileNode.has("family_name") ? profileNode.get("family_name").asText() : "";
                name = (givenName + " " + familyName).trim();
            }
            
            Map<String, Object> additionalInfo = new HashMap<>();
            if (profileNode.has("picture")) {
                additionalInfo.put("picture", profileNode.get("picture").asText());
            }
            if (profileNode.has("locale")) {
                additionalInfo.put("locale", profileNode.get("locale").asText());
            }
            
            return new UserInfo(id, null, email, name, additionalInfo);
            
        } catch (IOException e) {
            logger.error("Error fetching user info", e);
            throw new RuntimeException("Failed to fetch user info", e);
        }
    }
    
    
    @Override
    public boolean isTokenExpired(LocalDateTime expiresAt) {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
    
    @Override
    public List<String> getRequiredScopes() {
        return Arrays.asList(
                "openid",                  // Required for OpenID Connect authentication
                "profile",                 // Required for lite profile (id, name, profile picture)
                "email",                   // Required for email address
                "w_member_social"          // Required for posting content on behalf of authenticated member
        );
    }
    
    @Override
    public String getProviderName() {
        return "linkedin";
    }
}