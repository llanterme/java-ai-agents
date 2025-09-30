package za.co.digitalcowboy.agents.service.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LinkedInOAuthProviderTest {
    
    private LinkedInOAuthProvider linkedInProvider;
    
    @BeforeEach
    void setUp() {
        linkedInProvider = new LinkedInOAuthProvider();
        ReflectionTestUtils.setField(linkedInProvider, "clientId", "test-client-id");
        ReflectionTestUtils.setField(linkedInProvider, "clientSecret", "test-client-secret");
    }
    
    @Test
    void getProviderName_ShouldReturnLinkedIn() {
        // When
        String providerName = linkedInProvider.getProviderName();
        
        // Then
        assertThat(providerName).isEqualTo("linkedin");
    }
    
    @Test
    void getRequiredScopes_ShouldReturnExpectedScopes() {
        // When
        List<String> scopes = linkedInProvider.getRequiredScopes();

        // Then
        assertThat(scopes).containsExactlyInAnyOrder(
                "openid",
                "profile",
                "email",
                "w_member_social"
        );
    }
    
    @Test
    void getAuthorizationUrl_ShouldBuildCorrectUrl() {
        // Given
        String state = "test-state-123";
        String redirectUri = "http://localhost:8080/api/v1/connections/linkedin/callback";
        
        // When
        String authorizationUrl = linkedInProvider.getAuthorizationUrl(state, redirectUri);
        
        // Then
        assertThat(authorizationUrl)
                .contains("https://www.linkedin.com/oauth/v2/authorization")
                .contains("response_type=code")
                .contains("client_id=test-client-id")
                .contains("redirect_uri=" + redirectUri)
                .contains("state=" + state)
                .contains("scope=");
    }
    
    @Test
    void isTokenExpired_ShouldReturnTrue_WhenTokenIsExpired() {
        // Given
        LocalDateTime expiredTime = LocalDateTime.now().minusHours(1);
        
        // When
        boolean isExpired = linkedInProvider.isTokenExpired(expiredTime);
        
        // Then
        assertThat(isExpired).isTrue();
    }
    
    @Test
    void isTokenExpired_ShouldReturnFalse_WhenTokenIsValid() {
        // Given
        LocalDateTime validTime = LocalDateTime.now().plusHours(1);
        
        // When
        boolean isExpired = linkedInProvider.isTokenExpired(validTime);
        
        // Then
        assertThat(isExpired).isFalse();
    }
    
    @Test
    void isTokenExpired_ShouldReturnFalse_WhenExpiryIsNull() {
        // When
        boolean isExpired = linkedInProvider.isTokenExpired(null);
        
        // Then
        assertThat(isExpired).isFalse();
    }
}