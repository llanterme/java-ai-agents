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
import za.co.digitalcowboy.agents.domain.dto.GeneratedContentResponse;
import za.co.digitalcowboy.agents.security.JwtService;
import za.co.digitalcowboy.agents.service.AuthService;
import za.co.digitalcowboy.agents.service.GeneratedContentService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ContentController.class, excludeAutoConfiguration = {})
@Import({SecurityConfig.class, CorsProperties.class, WebConfig.class})
class ContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GeneratedContentService generatedContentService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private AuthService authService;

    private GeneratedContentResponse testContentResponse;

    @BeforeEach
    void setUp() {
        GeneratedContentResponse.ResearchData research = new GeneratedContentResponse.ResearchData(
            "Artificial Intelligence",
            Arrays.asList("Point 1", "Point 2", "Point 3"),
            Arrays.asList("https://source1.com", "https://source2.com")
        );

        GeneratedContentResponse.ContentData content = new GeneratedContentResponse.ContentData(
            "linkedin",
            "professional",
            "AI Revolution",
            "AI is transforming industries...",
            "Learn more about AI",
            Arrays.asList("#AI", "#Technology")
        );

        GeneratedContentResponse.ImageData image = new GeneratedContentResponse.ImageData(
            "AI futuristic image",
            Arrays.asList("https://openai.com/image1.png"),
            Arrays.asList("/local/image1.png"),
            Arrays.asList("http://localhost:8080/generated-image/image1.png")
        );

        testContentResponse = new GeneratedContentResponse(
            1L,
            "Artificial Intelligence",
            "linkedin",
            "professional",
            2,
            research,
            content,
            image,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getAllContent_Success() throws Exception {
        List<GeneratedContentResponse> contents = Arrays.asList(testContentResponse);
        when(generatedContentService.getAllContentForUser("test@example.com"))
            .thenReturn(contents);

        mockMvc.perform(get("/api/v1/content"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].topic").value("Artificial Intelligence"))
            .andExpect(jsonPath("$[0].platform").value("linkedin"));

        verify(generatedContentService).getAllContentForUser("test@example.com");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getContentById_Success() throws Exception {
        when(generatedContentService.getContentById(1L, "test@example.com"))
            .thenReturn(testContentResponse);

        mockMvc.perform(get("/api/v1/content/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.topic").value("Artificial Intelligence"))
            .andExpect(jsonPath("$.platform").value("linkedin"))
            .andExpect(jsonPath("$.content.body").value("AI is transforming industries..."))
            .andExpect(jsonPath("$.image.prompt").value("AI futuristic image"));

        verify(generatedContentService).getContentById(1L, "test@example.com");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getContentById_NotFound() throws Exception {
        when(generatedContentService.getContentById(999L, "test@example.com"))
            .thenThrow(new IllegalArgumentException("Content not found"));

        mockMvc.perform(get("/api/v1/content/999"))
            .andExpect(status().isNotFound());

        verify(generatedContentService).getContentById(999L, "test@example.com");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void deleteContent_Success() throws Exception {
        doNothing().when(generatedContentService).deleteContent(1L, "test@example.com");

        mockMvc.perform(delete("/api/v1/content/1"))
            .andExpect(status().isNoContent());

        verify(generatedContentService).deleteContent(1L, "test@example.com");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void deleteContent_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Content not found"))
            .when(generatedContentService).deleteContent(999L, "test@example.com");

        mockMvc.perform(delete("/api/v1/content/999"))
            .andExpect(status().isNotFound());

        verify(generatedContentService).deleteContent(999L, "test@example.com");
    }

    @Test
    void getAllContent_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/content"))
            .andExpect(status().isUnauthorized());

        verify(generatedContentService, never()).getAllContentForUser(any());
    }

    @Test
    void getContentById_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/content/1"))
            .andExpect(status().isUnauthorized());

        verify(generatedContentService, never()).getContentById(any(), any());
    }

    @Test
    void deleteContent_Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/content/1"))
            .andExpect(status().isUnauthorized());

        verify(generatedContentService, never()).deleteContent(any(), any());
    }
}