package za.co.digitalcowboy.agents.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.digitalcowboy.agents.domain.*;
import za.co.digitalcowboy.agents.domain.dto.GeneratedContentResponse;
import za.co.digitalcowboy.agents.repository.GeneratedContentRepository;
import za.co.digitalcowboy.agents.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeneratedContentServiceTest {

    @Mock
    private GeneratedContentRepository contentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GeneratedContentService generatedContentService;

    private User testUser;
    private TopicRequest testRequest;
    private OrchestrationResult testResult;
    private GeneratedContent testContent;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "Test", "User", "hashedPassword");
        testUser.setId(1L);

        testRequest = new TopicRequest(
            "Artificial Intelligence",
            "linkedin",
            "professional",
            2
        );

        ResearchPoints research = new ResearchPoints(
            Arrays.asList("Point 1", "Point 2", "Point 3"),
            Arrays.asList("Source 1", "Source 2")
        );

        ContentDraft content = new ContentDraft(
            "linkedin",
            "professional",
            "AI Revolution",
            "AI is transforming industries...",
            "Learn more about AI"
        );

        ImageResult image = new ImageResult(
            "AI futuristic image",
            Arrays.asList("https://openai.com/image1.png"),
            Arrays.asList("/local/image1.png"),
            Arrays.asList("http://localhost:8080/generated-image/image1.png")
        );

        testResult = new OrchestrationResult(
            "Artificial Intelligence",
            research,
            content,
            image,
            null
        );

        testContent = GeneratedContent.fromOrchestrationResult(testUser, testRequest, testResult);
        testContent.setId(1L);
    }

    @Test
    void saveGeneratedContent_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(contentRepository.save(any(GeneratedContent.class))).thenReturn(testContent);

        GeneratedContent saved = generatedContentService.saveGeneratedContent(
            "test@example.com", testRequest, testResult);

        assertNotNull(saved);
        assertEquals(1L, saved.getId());
        assertEquals("Artificial Intelligence", saved.getTopic());
        assertEquals("linkedin", saved.getPlatform());
        assertEquals("professional", saved.getTone());

        verify(userRepository).findByEmail("test@example.com");
        verify(contentRepository).save(any(GeneratedContent.class));
    }

    @Test
    void saveGeneratedContent_UserNotFound() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            generatedContentService.saveGeneratedContent(
                "nonexistent@example.com", testRequest, testResult));

        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(contentRepository, never()).save(any());
    }

    @Test
    void getAllContentForUser_Success() {
        List<GeneratedContent> contents = Arrays.asList(testContent);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(contentRepository.findByUserOrderByCreatedAtDesc(testUser)).thenReturn(contents);

        List<GeneratedContentResponse> responses = generatedContentService.getAllContentForUser("test@example.com");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).id());
        assertEquals("Artificial Intelligence", responses.get(0).topic());

        verify(userRepository).findByEmail("test@example.com");
        verify(contentRepository).findByUserOrderByCreatedAtDesc(testUser);
    }

    @Test
    void getContentById_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(contentRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testContent));

        GeneratedContentResponse response = generatedContentService.getContentById(1L, "test@example.com");

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Artificial Intelligence", response.topic());
        assertEquals("linkedin", response.platform());

        verify(userRepository).findByEmail("test@example.com");
        verify(contentRepository).findByIdAndUser(1L, testUser);
    }

    @Test
    void getContentById_NotFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(contentRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            generatedContentService.getContentById(999L, "test@example.com"));

        verify(userRepository).findByEmail("test@example.com");
        verify(contentRepository).findByIdAndUser(999L, testUser);
    }

    @Test
    void deleteContent_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(contentRepository.existsByIdAndUser(1L, testUser)).thenReturn(true);

        generatedContentService.deleteContent(1L, "test@example.com");

        verify(userRepository).findByEmail("test@example.com");
        verify(contentRepository).existsByIdAndUser(1L, testUser);
        verify(contentRepository).deleteByIdAndUser(1L, testUser);
    }

    @Test
    void deleteContent_NotFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(contentRepository.existsByIdAndUser(999L, testUser)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
            generatedContentService.deleteContent(999L, "test@example.com"));

        verify(userRepository).findByEmail("test@example.com");
        verify(contentRepository).existsByIdAndUser(999L, testUser);
        verify(contentRepository, never()).deleteByIdAndUser(any(), any());
    }
}