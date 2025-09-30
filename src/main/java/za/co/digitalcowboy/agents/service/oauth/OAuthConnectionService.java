package za.co.digitalcowboy.agents.service.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.digitalcowboy.agents.domain.User;
import za.co.digitalcowboy.agents.domain.oauth.ConnectedAccount;
import za.co.digitalcowboy.agents.domain.oauth.ConnectionStatus;
import za.co.digitalcowboy.agents.domain.oauth.OAuthProvider;
import za.co.digitalcowboy.agents.repository.ConnectedAccountRepository;
import za.co.digitalcowboy.agents.repository.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OAuthConnectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(OAuthConnectionService.class);
    private static final String OAUTH_STATE_CACHE = "oauthState";
    
    @Value("${oauth.frontend-success-url:http://localhost:3000/settings/connections}")
    private String frontendSuccessUrl;
    
    private final OAuthProviderFactory providerFactory;
    private final ConnectedAccountRepository connectedAccountRepository;
    private final UserRepository userRepository;
    private final TokenEncryptionService encryptionService;
    private final CacheManager cacheManager;
    private final SecureRandom secureRandom;
    
    @Autowired
    public OAuthConnectionService(
            OAuthProviderFactory providerFactory,
            ConnectedAccountRepository connectedAccountRepository,
            UserRepository userRepository,
            TokenEncryptionService encryptionService,
            CacheManager cacheManager) {
        this.providerFactory = providerFactory;
        this.connectedAccountRepository = connectedAccountRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.cacheManager = cacheManager;
        this.secureRandom = new SecureRandom();
    }
    
    public String initiateConnection(String providerName, Long userId, String redirectUri) {
        OAuthProvider provider = OAuthProvider.fromValue(providerName);
        OAuthProviderService providerService = providerFactory.getProvider(provider);
        
        // Check if user already has a connection to this provider
        Optional<ConnectedAccount> existingConnection = connectedAccountRepository.findByUserIdAndProvider(userId, provider);
        if (existingConnection.isPresent() && existingConnection.get().isActive()) {
            throw new IllegalStateException("User already has an active connection to " + providerName);
        }
        
        // Generate secure state parameter for CSRF protection
        String state = generateSecureState();
        storeState(state, userId, provider);
        
        // Get authorization URL from provider
        String authorizationUrl = providerService.getAuthorizationUrl(state, redirectUri);
        
        logger.info("Initiated OAuth connection for user {} with provider {}", userId, providerName);
        return authorizationUrl;
    }
    
    public String handleCallback(String providerName, String code, String state, String redirectUri) {
        // Validate state parameter
        StateInfo stateInfo = validateAndConsumeState(state);
        if (stateInfo == null) {
            throw new IllegalArgumentException("Invalid or expired state parameter");
        }
        
        OAuthProvider provider = OAuthProvider.fromValue(providerName);
        if (!provider.equals(stateInfo.provider)) {
            throw new IllegalArgumentException("State provider mismatch");
        }
        
        OAuthProviderService providerService = providerFactory.getProvider(provider);
        
        try {
            // Exchange authorization code for tokens
            OAuthProviderService.TokenResponse tokenResponse = providerService.exchangeCodeForToken(code, redirectUri);
            
            // Get user info from the provider
            OAuthProviderService.UserInfo userInfo = providerService.getUserInfo(tokenResponse.getAccessToken());
            
            // Find our user
            User user = userRepository.findById(stateInfo.userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Create or update connected account
            ConnectedAccount connectedAccount = connectedAccountRepository
                    .findByUserIdAndProvider(stateInfo.userId, provider)
                    .orElse(new ConnectedAccount(user, provider, ""));
            
            // Update connection details
            connectedAccount.setAccessToken(encryptionService.encrypt(tokenResponse.getAccessToken()));
            if (tokenResponse.getRefreshToken() != null) {
                connectedAccount.setRefreshToken(encryptionService.encrypt(tokenResponse.getRefreshToken()));
            }
            connectedAccount.setTokenExpiresAt(tokenResponse.getExpiresAt());
            connectedAccount.setScopesList(tokenResponse.getScopes());
            connectedAccount.setProviderUserId(userInfo.getId());
            connectedAccount.setProviderUsername(userInfo.getUsername());
            connectedAccount.setStatus(ConnectionStatus.ACTIVE);
            
            connectedAccountRepository.save(connectedAccount);
            
            logger.info("Successfully connected user {} to provider {}", stateInfo.userId, providerName);
            
            // Return redirect URL for frontend
            return frontendSuccessUrl + "?status=success&provider=" + providerName;
            
        } catch (Exception e) {
            logger.error("Failed to handle OAuth callback for provider {}", providerName, e);
            return frontendSuccessUrl + "?status=error&provider=" + providerName + "&error=" + e.getMessage();
        }
    }
    
    @Transactional(readOnly = true)
    public List<ConnectedAccount> getUserConnections(Long userId) {
        return connectedAccountRepository.findByUserId(userId);
    }
    
    public void disconnectAccount(Long userId, String providerName) {
        OAuthProvider provider = OAuthProvider.fromValue(providerName);
        ConnectedAccount account = connectedAccountRepository.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found"));
        
        // Optionally revoke the token with the provider
        try {
            OAuthProviderService providerService = providerFactory.getProvider(provider);
            
            // DEBUG: Log token decryption for disconnect
            String encryptedToken = account.getAccessToken();
            logger.debug("DEBUG: Disconnect - Encrypted token from DB - length: {}, starts with: {}...", 
                encryptedToken != null ? encryptedToken.length() : 0, 
                encryptedToken != null && encryptedToken.length() > 10 ? encryptedToken.substring(0, 5) : "null");
            
            String decryptedToken = encryptionService.decrypt(account.getAccessToken());
            
            logger.debug("DEBUG: Disconnect - Decrypted token - length: {}, starts with: {}...", 
                decryptedToken != null ? decryptedToken.length() : 0, 
                decryptedToken != null && decryptedToken.length() > 10 ? decryptedToken.substring(0, 5) : "null");
            
            providerService.revokeToken(decryptedToken);
        } catch (Exception e) {
            logger.warn("Failed to revoke token with provider {}", providerName, e);
        }
        
        connectedAccountRepository.delete(account);
        logger.info("Disconnected user {} from provider {}", userId, providerName);
    }
    
    public boolean refreshTokenIfNeeded(Long userId, String providerName) {
        return refreshTokenIfNeeded(userId, providerName, 5); // Default to 5 days proactive refresh
    }

    /**
     * Refreshes token if needed, with configurable proactive refresh window.
     *
     * @param userId The user ID
     * @param providerName The OAuth provider name
     * @param daysBeforeExpiry Number of days before expiry to trigger refresh (for proactive refresh)
     * @return true if token is valid or successfully refreshed, false otherwise
     */
    public boolean refreshTokenIfNeeded(Long userId, String providerName, int daysBeforeExpiry) {
        OAuthProvider provider = OAuthProvider.fromValue(providerName);
        Optional<ConnectedAccount> accountOpt = connectedAccountRepository.findByUserIdAndProvider(userId, provider);

        if (!accountOpt.isPresent()) {
            logger.warn("No {} connection found for user {}", providerName, userId);
            return false;
        }

        ConnectedAccount account = accountOpt.get();

        // Check if token needs refresh (expired or expires within N days)
        boolean needsRefresh = false;
        if (account.getTokenExpiresAt() == null) {
            logger.warn("Token expiry date is null for user {} provider {}, assuming needs refresh", userId, providerName);
            needsRefresh = true;
        } else {
            LocalDateTime refreshThreshold = LocalDateTime.now().plusDays(daysBeforeExpiry);
            needsRefresh = account.getTokenExpiresAt().isBefore(refreshThreshold);

            if (!needsRefresh) {
                logger.debug("Token for user {} provider {} is still valid until {}",
                    userId, providerName, account.getTokenExpiresAt());
                return true;
            }
        }

        if (!needsRefresh) {
            return true; // Token is still valid
        }

        logger.info("Token for user {} provider {} expires within {} days, attempting refresh",
            userId, providerName, daysBeforeExpiry);

        if (account.getRefreshToken() == null) {
            logger.error("No refresh token available for user {} provider {}, marking as expired", userId, providerName);
            account.setStatus(ConnectionStatus.EXPIRED);
            connectedAccountRepository.save(account);
            return false;
        }

        try {
            OAuthProviderService providerService = providerFactory.getProvider(provider);
            String decryptedRefreshToken = encryptionService.decrypt(account.getRefreshToken());

            OAuthProviderService.TokenResponse tokenResponse = providerService.refreshToken(decryptedRefreshToken);

            // Update account with new tokens
            account.setAccessToken(encryptionService.encrypt(tokenResponse.getAccessToken()));
            if (tokenResponse.getRefreshToken() != null) {
                account.setRefreshToken(encryptionService.encrypt(tokenResponse.getRefreshToken()));
            }
            account.setTokenExpiresAt(tokenResponse.getExpiresAt());
            account.setStatus(ConnectionStatus.ACTIVE);

            connectedAccountRepository.save(account);

            logger.info("Successfully refreshed token for user {} provider {}, new expiry: {}",
                userId, providerName, tokenResponse.getExpiresAt());
            return true;

        } catch (Exception e) {
            logger.error("Failed to refresh token for user {} provider {}", userId, providerName, e);
            account.setStatus(ConnectionStatus.EXPIRED);
            connectedAccountRepository.save(account);
            return false;
        }
    }
    
    @Transactional(readOnly = true)
    public Optional<ConnectedAccount> getActiveConnection(Long userId, String providerName) {
        OAuthProvider provider = OAuthProvider.fromValue(providerName);
        Optional<ConnectedAccount> account = connectedAccountRepository.findByUserIdAndProvider(userId, provider);
        return account.filter(ConnectedAccount::isActive);
    }
    
    @Transactional(readOnly = true)
    public OAuthProviderService.UserInfo getUserInfo(Long userId, String providerName) {
        OAuthProvider provider = OAuthProvider.fromValue(providerName);
        ConnectedAccount account = connectedAccountRepository.findByUserIdAndProvider(userId, provider)
            .orElseThrow(() -> new IllegalStateException("No connection found for user " + userId + " and provider " + providerName));
        
        if (!account.isActive()) {
            throw new IllegalStateException("Connection is not active for user " + userId + " and provider " + providerName);
        }
        
        OAuthProviderService providerService = providerFactory.getProvider(provider);
        
        // DEBUG: Log encrypted token before decryption
        String encryptedToken = account.getAccessToken();
        logger.debug("DEBUG: Encrypted token from DB - length: {}, starts with: {}..., ends with: ...{}", 
            encryptedToken != null ? encryptedToken.length() : 0, 
            encryptedToken != null && encryptedToken.length() > 10 ? encryptedToken.substring(0, 5) : "null",
            encryptedToken != null && encryptedToken.length() > 10 ? encryptedToken.substring(encryptedToken.length() - 5) : "null");
        
        String decryptedToken = encryptionService.decrypt(account.getAccessToken());
        
        // DEBUG: Log decrypted token characteristics
        logger.debug("DEBUG: Decrypted token - length: {}, starts with: {}..., ends with: ...{}", 
            decryptedToken != null ? decryptedToken.length() : 0, 
            decryptedToken != null && decryptedToken.length() > 10 ? decryptedToken.substring(0, 5) : "null",
            decryptedToken != null && decryptedToken.length() > 10 ? decryptedToken.substring(decryptedToken.length() - 5) : "null");
        
        return providerService.getUserInfo(decryptedToken);
    }
    
    @Transactional(readOnly = true)
    public boolean isConnected(Long userId, String providerName) {
        OAuthProvider provider = OAuthProvider.fromValue(providerName);
        Optional<ConnectedAccount> account = connectedAccountRepository.findByUserIdAndProvider(userId, provider);
        return account.isPresent() && account.get().isActive();
    }
    
    private String generateSecureState() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    private void storeState(String state, Long userId, OAuthProvider provider) {
        Cache cache = cacheManager.getCache(OAUTH_STATE_CACHE);
        if (cache != null) {
            StateInfo stateInfo = new StateInfo(userId, provider, LocalDateTime.now());
            cache.put(state, stateInfo);
        }
    }
    
    private StateInfo validateAndConsumeState(String state) {
        Cache cache = cacheManager.getCache(OAUTH_STATE_CACHE);
        if (cache == null) {
            return null;
        }
        
        Cache.ValueWrapper wrapper = cache.get(state);
        if (wrapper == null) {
            return null;
        }
        
        StateInfo stateInfo = (StateInfo) wrapper.get();
        if (stateInfo == null) {
            return null;
        }
        
        // Check if state has expired (10 minutes timeout)
        if (stateInfo.createdAt.isBefore(LocalDateTime.now().minusMinutes(10))) {
            cache.evict(state);
            return null;
        }
        
        // Consume the state (remove from cache)
        cache.evict(state);
        
        return stateInfo;
    }
    
    public static class StateInfo {
        public final Long userId;
        public final OAuthProvider provider;
        public final LocalDateTime createdAt;
        
        public StateInfo(Long userId, OAuthProvider provider, LocalDateTime createdAt) {
            this.userId = userId;
            this.provider = provider;
            this.createdAt = createdAt;
        }
    }
}