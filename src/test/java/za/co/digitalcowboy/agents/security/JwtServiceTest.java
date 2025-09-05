package za.co.digitalcowboy.agents.security;

import za.co.digitalcowboy.agents.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    
    private JwtService jwtService;
    private UserDetails userDetails;
    
    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString("mySecretKeyForTestingPurposes123456789".getBytes());
        jwtService = new JwtService(secret, 30L, 7L);
        
        User user = new User("test@example.com", "John", "Doe", "hashedPassword");
        user.setId(1L);
        userDetails = user;
    }
    
    @Test
    void constructor_ShouldThrowException_WhenSecretIsNull() {
        assertThatThrownBy(() -> new JwtService(null, 30L, 7L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("JWT secret cannot be null or empty");
    }
    
    @Test
    void constructor_ShouldThrowException_WhenSecretIsEmpty() {
        assertThatThrownBy(() -> new JwtService("", 30L, 7L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("JWT secret cannot be null or empty");
    }
    
    @Test
    void generateAccessToken_ShouldReturnValidToken() {
        String token = jwtService.generateAccessToken(userDetails);
        
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(jwtService.extractUsername(token)).isEqualTo("test@example.com");
        assertThat(jwtService.extractTokenType(token)).isEqualTo("access");
    }
    
    @Test
    void generateRefreshToken_ShouldReturnValidToken() {
        String token = jwtService.generateRefreshToken(userDetails);
        
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(jwtService.extractUsername(token)).isEqualTo("test@example.com");
        assertThat(jwtService.extractTokenType(token)).isEqualTo("refresh");
    }
    
    @Test
    void extractUsername_ShouldReturnCorrectUsername() {
        String token = jwtService.generateAccessToken(userDetails);
        
        String extractedUsername = jwtService.extractUsername(token);
        
        assertThat(extractedUsername).isEqualTo("test@example.com");
    }
    
    @Test
    void extractTokenType_ShouldReturnCorrectType() {
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        
        assertThat(jwtService.extractTokenType(accessToken)).isEqualTo("access");
        assertThat(jwtService.extractTokenType(refreshToken)).isEqualTo("refresh");
    }
    
    @Test
    void isAccessToken_ShouldReturnTrue_ForAccessToken() {
        String accessToken = jwtService.generateAccessToken(userDetails);
        
        assertThat(jwtService.isAccessToken(accessToken)).isTrue();
    }
    
    @Test
    void isAccessToken_ShouldReturnFalse_ForRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        
        assertThat(jwtService.isAccessToken(refreshToken)).isFalse();
    }
    
    @Test
    void isRefreshToken_ShouldReturnTrue_ForRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        
        assertThat(jwtService.isRefreshToken(refreshToken)).isTrue();
    }
    
    @Test
    void isRefreshToken_ShouldReturnFalse_ForAccessToken() {
        String accessToken = jwtService.generateAccessToken(userDetails);
        
        assertThat(jwtService.isRefreshToken(accessToken)).isFalse();
    }
    
    @Test
    void isTokenValid_ShouldReturnTrue_ForValidToken() {
        String token = jwtService.generateAccessToken(userDetails);
        
        boolean isValid = jwtService.isTokenValid(token, userDetails);
        
        assertThat(isValid).isTrue();
    }
    
    @Test
    void isTokenValid_ShouldReturnFalse_ForDifferentUser() {
        String token = jwtService.generateAccessToken(userDetails);
        
        User differentUser = new User("different@example.com", "Jane", "Smith", "hashedPassword");
        
        boolean isValid = jwtService.isTokenValid(token, differentUser);
        
        assertThat(isValid).isFalse();
    }
    
    @Test
    void isTokenValid_ShouldReturnFalse_ForInvalidToken() {
        String invalidToken = "invalid.token.here";
        
        boolean isValid = jwtService.isTokenValid(invalidToken, userDetails);
        
        assertThat(isValid).isFalse();
    }
    
    @Test
    void extractExpiration_ShouldReturnFutureDate() {
        String token = jwtService.generateAccessToken(userDetails);
        
        var expiration = jwtService.extractExpiration(token);
        
        assertThat(expiration).isAfter(new java.util.Date());
    }
    
    @Test
    void getAccessTokenExpirySeconds_ShouldReturn1800() {
        long expirySeconds = jwtService.getAccessTokenExpirySeconds();
        
        assertThat(expirySeconds).isEqualTo(1800L); // 30 minutes * 60 seconds
    }
}