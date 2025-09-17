package za.co.digitalcowboy.agents.service.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import za.co.digitalcowboy.agents.domain.User;
import za.co.digitalcowboy.agents.domain.oauth.ConnectedAccount;
import za.co.digitalcowboy.agents.domain.oauth.ConnectionStatus;
import za.co.digitalcowboy.agents.domain.oauth.OAuthProvider;
import za.co.digitalcowboy.agents.repository.ConnectedAccountRepository;
import za.co.digitalcowboy.agents.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthConnectionServiceTest {
    
    @Mock
    private OAuthProviderFactory providerFactory;
    
    @Mock
    private ConnectedAccountRepository connectedAccountRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private TokenEncryptionService encryptionService;
    
    @Mock
    private CacheManager cacheManager;
    
    @Mock
    private OAuthProviderService mockProvider;
    
    private OAuthConnectionService oAuthConnectionService;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        oAuthConnectionService = new OAuthConnectionService(
                providerFactory, 
                connectedAccountRepository, 
                userRepository, 
                encryptionService, 
                cacheManager
        );
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test");
        testUser.setSurname("User");
    }
    
    @Test
    void initiateConnection_ShouldReturnAuthorizationUrl_WhenNoExistingConnection() {
        // Given
        String providerName = "linkedin";
        String redirectUri = "http://localhost:8080/api/v1/connections/linkedin/callback";
        
        when(providerFactory.getProvider(OAuthProvider.LINKEDIN)).thenReturn(mockProvider);
        when(connectedAccountRepository.findByUserIdAndProvider(1L, OAuthProvider.LINKEDIN))
                .thenReturn(Optional.empty());
        when(mockProvider.getAuthorizationUrl(anyString(), eq(redirectUri)))
                .thenReturn("https://linkedin.com/oauth/authorize?...");
        when(cacheManager.getCache("oauthState"))
                .thenReturn(new ConcurrentMapCache("oauthState"));
        
        // When
        String authorizationUrl = oAuthConnectionService.initiateConnection(providerName, 1L, redirectUri);
        
        // Then
        assertThat(authorizationUrl).startsWith("https://linkedin.com/oauth/authorize");
        verify(providerFactory).getProvider(OAuthProvider.LINKEDIN);
        verify(mockProvider).getAuthorizationUrl(anyString(), eq(redirectUri));
    }
    
    @Test
    void initiateConnection_ShouldThrowException_WhenActiveConnectionExists() {
        // Given
        String providerName = "linkedin";
        String redirectUri = "http://localhost:8080/api/v1/connections/linkedin/callback";
        
        ConnectedAccount existingConnection = new ConnectedAccount(testUser, OAuthProvider.LINKEDIN, "encrypted-token");
        existingConnection.setStatus(ConnectionStatus.ACTIVE);
        existingConnection.setTokenExpiresAt(LocalDateTime.now().plusDays(30));
        
        when(connectedAccountRepository.findByUserIdAndProvider(1L, OAuthProvider.LINKEDIN))
                .thenReturn(Optional.of(existingConnection));
        
        // When & Then
        assertThatThrownBy(() -> oAuthConnectionService.initiateConnection(providerName, 1L, redirectUri))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User already has an active connection");
    }
    
    @Test
    void handleCallback_ShouldCreateConnection_WhenValidCodeProvided() {
        // Given
        String providerName = "linkedin";
        String code = "auth-code-123";
        String state = "secure-state-456";
        String redirectUri = "http://localhost:8080/api/v1/connections/linkedin/callback";
        
        OAuthProviderService.TokenResponse tokenResponse = new OAuthProviderService.TokenResponse(
                "access-token", 
                "refresh-token", 
                LocalDateTime.now().plusDays(60), 
                Arrays.asList("r_liteprofile", "r_emailaddress")
        );
        
        OAuthProviderService.UserInfo userInfo = new OAuthProviderService.UserInfo(
                "linkedin-id-123", 
                "testuser", 
                "test@example.com", 
                "Test User",
                Collections.emptyMap()
        );
        
        when(cacheManager.getCache("oauthState"))
                .thenReturn(new ConcurrentMapCache("oauthState"));
        when(providerFactory.getProvider(OAuthProvider.LINKEDIN)).thenReturn(mockProvider);
        when(mockProvider.exchangeCodeForToken(code, redirectUri)).thenReturn(tokenResponse);
        when(mockProvider.getUserInfo("access-token")).thenReturn(userInfo);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(connectedAccountRepository.findByUserIdAndProvider(1L, OAuthProvider.LINKEDIN))
                .thenReturn(Optional.empty());
        when(encryptionService.encrypt("access-token")).thenReturn("encrypted-access-token");
        when(encryptionService.encrypt("refresh-token")).thenReturn("encrypted-refresh-token");
        when(connectedAccountRepository.save(any(ConnectedAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Simulate state storage and retrieval
        ConcurrentMapCache cache = new ConcurrentMapCache("oauthState");
        when(cacheManager.getCache("oauthState")).thenReturn(cache);
        
        // Store state manually for test
        OAuthConnectionService.StateInfo stateInfo = new OAuthConnectionService.StateInfo(1L, OAuthProvider.LINKEDIN, LocalDateTime.now());
        cache.put(state, stateInfo);
        
        // When
        String frontendRedirectUrl = oAuthConnectionService.handleCallback(providerName, code, state, redirectUri);
        
        // Then
        assertThat(frontendRedirectUrl).contains("status=success").contains("provider=linkedin");
        verify(mockProvider).exchangeCodeForToken(code, redirectUri);
        verify(mockProvider).getUserInfo("access-token");
        verify(connectedAccountRepository).save(any(ConnectedAccount.class));
    }
    
    @Test
    void isConnected_ShouldReturnTrue_WhenActiveConnectionExists() {
        // Given
        String providerName = "linkedin";
        ConnectedAccount activeConnection = new ConnectedAccount(testUser, OAuthProvider.LINKEDIN, "encrypted-token");
        activeConnection.setStatus(ConnectionStatus.ACTIVE);
        activeConnection.setTokenExpiresAt(LocalDateTime.now().plusDays(30));
        
        when(connectedAccountRepository.findByUserIdAndProvider(1L, OAuthProvider.LINKEDIN))
                .thenReturn(Optional.of(activeConnection));
        
        // When
        boolean isConnected = oAuthConnectionService.isConnected(1L, providerName);
        
        // Then
        assertThat(isConnected).isTrue();
    }
    
    @Test
    void isConnected_ShouldReturnFalse_WhenNoConnectionExists() {
        // Given
        String providerName = "linkedin";
        
        when(connectedAccountRepository.findByUserIdAndProvider(1L, OAuthProvider.LINKEDIN))
                .thenReturn(Optional.empty());
        
        // When
        boolean isConnected = oAuthConnectionService.isConnected(1L, providerName);
        
        // Then
        assertThat(isConnected).isFalse();
    }
    
    @Test
    void disconnectAccount_ShouldDeleteConnection_WhenConnectionExists() {
        // Given
        String providerName = "linkedin";
        ConnectedAccount connection = new ConnectedAccount(testUser, OAuthProvider.LINKEDIN, "encrypted-token");
        
        when(connectedAccountRepository.findByUserIdAndProvider(1L, OAuthProvider.LINKEDIN))
                .thenReturn(Optional.of(connection));
        when(providerFactory.getProvider(OAuthProvider.LINKEDIN)).thenReturn(mockProvider);
        when(encryptionService.decrypt("encrypted-token")).thenReturn("decrypted-token");
        
        // When
        oAuthConnectionService.disconnectAccount(1L, providerName);
        
        // Then
        verify(mockProvider).revokeToken("decrypted-token");
        verify(connectedAccountRepository).delete(connection);
    }
}