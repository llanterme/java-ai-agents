package za.co.digitalcowboy.agents.service;

import za.co.digitalcowboy.agents.domain.User;
import za.co.digitalcowboy.agents.domain.auth.AuthResponse;
import za.co.digitalcowboy.agents.domain.auth.LoginRequest;
import za.co.digitalcowboy.agents.domain.auth.RegisterRequest;
import za.co.digitalcowboy.agents.domain.auth.UserResponse;
import za.co.digitalcowboy.agents.repository.UserRepository;
import za.co.digitalcowboy.agents.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService implements UserDetailsService {
    
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }
    
    public AuthResponse register(RegisterRequest request) {
        log.debug("Registering new user with email: {}", request.email());
        
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User with this email already exists");
        }
        
        User user = new User(
            request.email(),
            request.name(),
            request.surname(),
            passwordEncoder.encode(request.password())
        );
        
        user = userRepository.save(user);
        log.info("User registered successfully with ID: {}", user.getId());
        
        return generateAuthResponse(user);
    }
    
    public AuthResponse login(LoginRequest request) {
        log.debug("User login attempt for email: {}", request.email());
        
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        
        if (!user.getActive()) {
            throw new BadCredentialsException("User account is inactive");
        }
        
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        
        log.info("User logged in successfully: {}", user.getEmail());
        return generateAuthResponse(user);
    }
    
    public AuthResponse refreshToken(String refreshToken) {
        log.debug("Refreshing token");
        
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token type");
        }
        
        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("User not found"));
        
        if (!user.getActive()) {
            throw new BadCredentialsException("User account is inactive");
        }
        
        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        
        log.info("Token refreshed successfully for user: {}", user.getEmail());
        return generateAuthResponse(user);
    }
    
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        
        return UserResponse.from(user);
    }
    
    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        long expiresIn = jwtService.getAccessTokenExpirySeconds();
        
        return new AuthResponse(accessToken, refreshToken, expiresIn);
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}