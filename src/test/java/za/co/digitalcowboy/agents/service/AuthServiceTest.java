package za.co.digitalcowboy.agents.service;

import za.co.digitalcowboy.agents.domain.User;
import za.co.digitalcowboy.agents.domain.auth.*;
import za.co.digitalcowboy.agents.repository.UserRepository;
import za.co.digitalcowboy.agents.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtService jwtService;
    
    private AuthService authService;
    
    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }
    
    @Test
    void register_ShouldReturnAuthResponse_WhenValidRequest() {
        RegisterRequest request = new RegisterRequest(
            "test@example.com",
            "John",
            "Doe",
            "password123"
        );
        
        User savedUser = new User("test@example.com", "John", "Doe", "hashedPassword");
        savedUser.setId(1L);
        
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(1800L);
        
        AuthResponse response = authService.register(request);
        
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.expiresIn()).isEqualTo(1800L);
        
        verify(userRepository).existsByEmail(request.email());
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(request.password());
    }
    
    @Test
    void register_ShouldThrowException_WhenEmailExists() {
        RegisterRequest request = new RegisterRequest(
            "existing@example.com",
            "John",
            "Doe",
            "password123"
        );
        
        when(userRepository.existsByEmail(request.email())).thenReturn(true);
        
        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User with this email already exists");
        
        verify(userRepository).existsByEmail(request.email());
        verifyNoMoreInteractions(userRepository);
    }
    
    @Test
    void login_ShouldReturnAuthResponse_WhenValidCredentials() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        
        User user = new User("test@example.com", "John", "Doe", "hashedPassword");
        user.setId(1L);
        
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(1800L);
        
        AuthResponse response = authService.login(request);
        
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.expiresIn()).isEqualTo(1800L);
    }
    
    @Test
    void login_ShouldThrowException_WhenUserNotFound() {
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");
        
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Invalid email or password");
    }
    
    @Test
    void login_ShouldThrowException_WhenInvalidPassword() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
        
        User user = new User("test@example.com", "John", "Doe", "hashedPassword");
        
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(false);
        
        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Invalid email or password");
    }
    
    @Test
    void login_ShouldThrowException_WhenUserInactive() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        
        User user = new User("test@example.com", "John", "Doe", "hashedPassword");
        user.setActive(false);
        
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        
        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("User account is inactive");
    }
    
    @Test
    void refreshToken_ShouldReturnAuthResponse_WhenValidToken() {
        String refreshToken = "valid-refresh-token";
        String email = "test@example.com";
        
        User user = new User(email, "John", "Doe", "hashedPassword");
        user.setId(1L);
        
        when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtService.extractUsername(refreshToken)).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid(refreshToken, user)).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(1800L);
        
        AuthResponse response = authService.refreshToken(refreshToken);
        
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.expiresIn()).isEqualTo(1800L);
    }
    
    @Test
    void refreshToken_ShouldThrowException_WhenNotRefreshToken() {
        String accessToken = "access-token";
        
        when(jwtService.isRefreshToken(accessToken)).thenReturn(false);
        
        assertThatThrownBy(() -> authService.refreshToken(accessToken))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Invalid refresh token type");
    }
    
    @Test
    void loadUserByUsername_ShouldReturnUser_WhenExists() {
        String email = "test@example.com";
        User user = new User(email, "John", "Doe", "hashedPassword");
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        
        User result = (User) authService.loadUserByUsername(email);
        
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getName()).isEqualTo("John");
    }
    
    @Test
    void loadUserByUsername_ShouldThrowException_WhenNotExists() {
        String email = "nonexistent@example.com";
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> authService.loadUserByUsername(email))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("User not found: " + email);
    }
}