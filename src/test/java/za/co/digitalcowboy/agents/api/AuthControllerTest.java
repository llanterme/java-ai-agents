package za.co.digitalcowboy.agents.api;

import za.co.digitalcowboy.agents.domain.User;
import za.co.digitalcowboy.agents.domain.auth.*;
import za.co.digitalcowboy.agents.repository.UserRepository;
import za.co.digitalcowboy.agents.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class AuthControllerTest {
    
    @Autowired
    private WebApplicationContext context;
    
    @MockBean
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }
    
    @Test
    void register_ShouldReturnAuthResponse_WhenValidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "test@example.com",
            "John",
            "Doe",
            "password123"
        );
        
        User savedUser = new User("test@example.com", "John", "Doe", passwordEncoder.encode("password123"));
        savedUser.setId(1L);
        
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.expiresIn").exists());
    }
    
    @Test
    void register_ShouldReturnBadRequest_WhenEmailExists() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "existing@example.com",
            "John",
            "Doe",
            "password123"
        );
        
        when(userRepository.existsByEmail(request.email())).thenReturn(true);
        
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("User with this email already exists"));
    }
    
    @Test
    void register_ShouldReturnBadRequest_WhenInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "invalid-email",
            "John",
            "Doe",
            "password123"
        );
        
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Validation failed"));
    }
    
    @Test
    void login_ShouldReturnAuthResponse_WhenValidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        
        User user = new User("test@example.com", "John", "Doe", passwordEncoder.encode("password123"));
        user.setId(1L);
        
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.expiresIn").exists());
    }
    
    @Test
    void login_ShouldReturnUnauthorized_WhenInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
        
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
    
    @Test
    void refresh_ShouldReturnAuthResponse_WhenValidRefreshToken() throws Exception {
        User user = new User("test@example.com", "John", "Doe", "hashedPassword");
        user.setId(1L);
        
        String refreshToken = jwtService.generateRefreshToken(user);
        
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        
        mockMvc.perform(post("/api/v1/auth/refresh")
                .header("Authorization", "Bearer " + refreshToken))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.expiresIn").exists());
    }
    
    @Test
    void refresh_ShouldReturnBadRequest_WhenMissingToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Missing or invalid refresh token"));
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void me_ShouldReturnUserResponse_WhenAuthenticated() throws Exception {
        User user = new User("test@example.com", "John", "Doe", "hashedPassword");
        user.setId(1L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.name").value("John"))
            .andExpect(jsonPath("$.surname").value("Doe"))
            .andExpect(jsonPath("$.roles").isArray())
            .andExpect(jsonPath("$.active").value(true));
    }
    
    @Test
    void me_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void protectedEndpoint_ShouldReturnUnauthorized_WhenNoToken() throws Exception {
        mockMvc.perform(post("/api/v1/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"test\",\"platform\":\"twitter\",\"tone\":\"casual\"}"))
            .andExpect(status().isUnauthorized());
    }
}