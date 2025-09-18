package za.co.digitalcowboy.agents.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import za.co.digitalcowboy.agents.config.CorsProperties;
import za.co.digitalcowboy.agents.config.SecurityConfig;
import za.co.digitalcowboy.agents.config.WebConfig;
import za.co.digitalcowboy.agents.domain.User;
import za.co.digitalcowboy.agents.domain.dto.GeneratedContentResponse;
import za.co.digitalcowboy.agents.domain.social.LinkedInPostResponse;
import za.co.digitalcowboy.agents.domain.social.dto.SocialPostRequest;
import za.co.digitalcowboy.agents.repository.UserRepository;
import za.co.digitalcowboy.agents.security.JwtService;
import za.co.digitalcowboy.agents.service.AuthService;
import za.co.digitalcowboy.agents.service.GeneratedContentService;
import za.co.digitalcowboy.agents.service.social.DirectLinkedInPostingService;
import za.co.digitalcowboy.agents.service.social.LinkedInPostingService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SocialController.class, excludeAutoConfiguration = {})
@Import({SecurityConfig.class, CorsProperties.class, WebConfig.class})
class SocialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LinkedInPostingService linkedInPostingService;

    @MockBean
    private DirectLinkedInPostingService directLinkedInPostingService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private GeneratedContentService generatedContentService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private AuthService authService;

    private User testUser;
    private GeneratedContentResponse testContent;
    private SocialPostRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "Test", "User", "hashedPassword");
        testUser.setId(1L);

        // Create test content response
        GeneratedContentResponse.ResearchData research = new GeneratedContentResponse.ResearchData(
            "AI Topic",
            Arrays.asList("Point 1", "Point 2")
        );

        GeneratedContentResponse.ContentData content = new GeneratedContentResponse.ContentData(
            "linkedin",
            "professional",
            "AI Revolution",
            "AI is transforming industries across the globe...",
            "Learn more about AI",
            Arrays.asList("#AI", "#Technology")
        );

        GeneratedContentResponse.ImageData image = new GeneratedContentResponse.ImageData(
            "AI futuristic image",
            Arrays.asList("https://openai.com/image1.png"),
            Arrays.asList("/local/image1.png"),
            Arrays.asList("http://localhost:8080/generated-image/image1.png")
        );

        testContent = new GeneratedContentResponse(
            1L,
            "AI Topic",
            "linkedin",
            "professional",
            1,
            research,
            content,
            image,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        testRequest = new SocialPostRequest(1L);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void postToLinkedIn_Success_OAuth() throws Exception {
        // Setup mocks
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(generatedContentService.getContentById(1L, "test@example.com")).thenReturn(testContent);
        when(linkedInPostingService.validateConnection(1L)).thenReturn(true);

        LinkedInPostResponse linkedInResponse = new LinkedInPostResponse();
        linkedInResponse.setId("urn:li:activity:123456789");
        linkedInResponse.setState("PUBLISHED");
        when(linkedInPostingService.postToLinkedIn(eq(1L), anyString(), anyString()))
            .thenReturn(linkedInResponse);

        mockMvc.perform(post("/api/v1/social/linkedin/post")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(testRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.postId").value("urn:li:activity:123456789"))
            .andExpect(jsonPath("$.state").value("PUBLISHED"))
            .andExpect(jsonPath("$.postUrl").value("https://www.linkedin.com/posts/activity-123456789"))
            .andExpect(jsonPath("$.message").value("Post created successfully via OAuth"));

        verify(generatedContentService).getContentById(1L, "test@example.com");
        verify(linkedInPostingService).postToLinkedIn(1L, "AI is transforming industries across the globe...", "/local/image1.png");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void postToLinkedIn_Success_DirectToken() throws Exception {
        // Setup mocks
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(generatedContentService.getContentById(1L, "test@example.com")).thenReturn(testContent);
        when(linkedInPostingService.validateConnection(1L)).thenReturn(false);
        when(directLinkedInPostingService.isConfigured()).thenReturn(true);
        when(directLinkedInPostingService.postToLinkedIn(anyString(), anyString()))
            .thenReturn("direct-post-123");

        mockMvc.perform(post("/api/v1/social/linkedin/post")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(testRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.postId").value("direct-post-123"))
            .andExpect(jsonPath("$.state").value("PUBLISHED"))
            .andExpect(jsonPath("$.message").value("Post created successfully via direct token fallback"));

        verify(directLinkedInPostingService).postToLinkedIn("AI is transforming industries across the globe...", "/local/image1.png");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void postToLinkedIn_ContentNotFound() throws Exception {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(generatedContentService.getContentById(1L, "test@example.com"))
            .thenThrow(new IllegalArgumentException("Content not found"));

        mockMvc.perform(post("/api/v1/social/linkedin/post")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(testRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Content not found or you don't have permission to access it"))
            .andExpect(jsonPath("$.status").value(400));

        verify(generatedContentService).getContentById(1L, "test@example.com");
        verify(linkedInPostingService, never()).postToLinkedIn(anyLong(), anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void postToLinkedIn_NoLinkedInConnection() throws Exception {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(generatedContentService.getContentById(1L, "test@example.com")).thenReturn(testContent);
        when(linkedInPostingService.validateConnection(1L)).thenReturn(false);
        when(directLinkedInPostingService.isConfigured()).thenReturn(false);

        mockMvc.perform(post("/api/v1/social/linkedin/post")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(testRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("LinkedIn connection not found or expired. Please reconnect your LinkedIn account."));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void postToLinkedIn_InvalidRequest_MissingId() throws Exception {
        SocialPostRequest invalidRequest = new SocialPostRequest();

        mockMvc.perform(post("/api/v1/social/linkedin/post")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getLinkedInStatus_Success() throws Exception {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(linkedInPostingService.validateConnection(1L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/social/linkedin/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.connected").value(true))
            .andExpect(jsonPath("$.message").value("LinkedIn connection is active and ready for posting"));
    }

    @Test
    void postToLinkedIn_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/social/linkedin/post")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(testRequest)))
            .andExpect(status().isUnauthorized());

        verify(generatedContentService, never()).getContentById(anyLong(), anyString());
    }
}