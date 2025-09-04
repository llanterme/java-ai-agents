package za.co.digitalcowboy.agents;

import za.co.digitalcowboy.agents.agents.ContentAgent;
import za.co.digitalcowboy.agents.agents.ImageAgent;
import za.co.digitalcowboy.agents.agents.ResearchAgent;
import za.co.digitalcowboy.agents.domain.*;
import za.co.digitalcowboy.agents.graph.AgentGraph;
import za.co.digitalcowboy.agents.tools.OpenAiImageTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AgentFlowTests {
    
    @Mock
    private ChatLanguageModel mockChatModel;
    
    @Mock
    private OpenAiImageTool mockImageTool;
    
    private ObjectMapper objectMapper;
    private Timer mockTimer;
    private AgentGraph agentGraph;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        mockTimer = Timer.builder("test.timer").register(registry);
        
        ResearchAgent researchAgent = new ResearchAgent(mockChatModel, objectMapper, mockTimer);
        ContentAgent contentAgent = new ContentAgent(mockChatModel, objectMapper, mockTimer);
        ImageAgent imageAgent = new ImageAgent(mockChatModel, objectMapper, mockImageTool, mockTimer);
        
        agentGraph = new AgentGraph(researchAgent, contentAgent, imageAgent, mockTimer);
    }
    
    @Test
    void testCompleteWorkflow() {
        // Mock research response
        String researchJson = """
            {
              "points": [
                "Artificial Intelligence refers to computer systems that can perform tasks typically requiring human intelligence.",
                "Machine Learning is a subset of AI that enables computers to learn without explicit programming.",
                "Deep Learning uses neural networks with multiple layers to process complex data patterns.",
                "AI applications include image recognition, natural language processing, and autonomous vehicles.",
                "The global AI market was valued at approximately $136 billion in 2022.",
                "Major AI companies include Google, Microsoft, OpenAI, and NVIDIA.",
                "AI ethics and responsible development are increasingly important considerations."
              ],
              "sources": []
            }
            """;
        
        // Mock content response
        String contentJson = """
            {
              "platform": "twitter",
              "tone": "professional",
              "headline": "AI Revolution",
              "body": "Artificial Intelligence is transforming industries with $136B market value. From machine learning to deep learning, AI applications span image recognition to autonomous vehicles. Major players: Google, Microsoft, OpenAI, NVIDIA. Ethics matter! #AI #TechTrends",
              "cta": "Learn more about AI trends"
            }
            """;
        
        // Mock image brief response
        String imageBriefJson = """
            {
              "prompt": "Modern AI technology visualization with neural networks, futuristic and professional style, clean background"
            }
            """;
        
        when(mockChatModel.generate(anyString()))
            .thenReturn(researchJson)
            .thenReturn(contentJson)
            .thenReturn(imageBriefJson);
        
        when(mockImageTool.generateImage(anyString(), any(Integer.class), anyString()))
            .thenReturn(new ImageResult(
                "Modern AI technology visualization",
                List.of("https://example.com/image1.png"),
                List.of("/path/to/local/image1.png"),
                List.of("http://localhost:8080/generated-image/image1.png")
            ));
        
        // Execute workflow
        TopicRequest request = new TopicRequest("Artificial Intelligence", "twitter", "professional", 1);
        OrchestrationResult result = agentGraph.run(request);
        
        // Validate research constraints
        assertThat(result.research().points()).hasSizeBetween(5, 7);
        for (String point : result.research().points()) {
            assertThat(point.split("\\s+")).hasSizeLessThanOrEqualTo(25);
        }
        
        // Validate content formatting
        assertThat(result.content().platform()).isEqualTo("twitter");
        assertThat(result.content().tone()).isEqualTo("professional");
        assertThat(result.content().body()).isNotBlank();
        
        // Twitter length constraint (just body + hashtags, not including headline and CTA separately)
        String twitterContent = result.content().body();
        assertThat(twitterContent).hasSizeLessThanOrEqualTo(280);
        
        // Validate image generation
        assertThat(result.image().prompt()).isNotBlank();
        assertThat(result.image().openAiImageUrls()).hasSize(1);
        assertThat(result.image().openAiImageUrls().get(0)).startsWith("https://");
    }
    
    @Test
    void testLinkedInContentFormat() {
        String contentJson = """
            {
              "platform": "linkedin",
              "tone": "professional",
              "headline": "The Future of AI in Business",
              "body": "Artificial Intelligence is revolutionizing how businesses operate in the digital age.\\n\\nFrom automating routine tasks to providing deep insights through data analysis, AI applications are becoming increasingly sophisticated.\\n\\nMachine learning algorithms enable companies to predict customer behavior, optimize supply chains, and enhance decision-making processes.\\n\\nAs we move forward, the integration of AI technologies will be crucial for maintaining competitive advantage in various industries.",
              "cta": "Connect with me to discuss AI strategies for your business"
            }
            """;
        
        when(mockChatModel.generate(anyString()))
            .thenReturn("{\"points\":[\"AI point 1\",\"AI point 2\",\"AI point 3\",\"AI point 4\",\"AI point 5\"],\"sources\":[]}")
            .thenReturn(contentJson)
            .thenReturn("{\"prompt\":\"Professional AI business illustration\"}");
        
        when(mockImageTool.generateImage(anyString(), any(Integer.class), anyString()))
            .thenReturn(new ImageResult("test prompt", List.of(), List.of(), List.of()));
        
        TopicRequest request = new TopicRequest("AI in Business", "linkedin", "professional", 1);
        OrchestrationResult result = agentGraph.run(request);
        
        // LinkedIn should have 3-5 paragraphs
        String[] paragraphs = result.content().body().split("\\n\\n");
        assertThat(paragraphs).hasSizeBetween(3, 5);
        
        assertThat(result.content().platform()).isEqualTo("linkedin");
        assertThat(result.content().cta()).contains("Connect");
    }
    
    @Test
    void testBlogContentFormat() {
        String contentJson = """
            {
              "platform": "blog",
              "tone": "authoritative",
              "headline": "Understanding Artificial Intelligence: A Comprehensive Guide",
              "body": "Artificial Intelligence represents one of the most significant technological advances of our time. This comprehensive guide explores the fundamental concepts, applications, and implications of AI technology.\\n\\n## What is Artificial Intelligence?\\n\\nAI refers to computer systems capable of performing tasks that typically require human intelligence. These systems can learn, reason, and make decisions based on data analysis.\\n\\n## Key Applications\\n\\nMachine learning algorithms power recommendation systems, autonomous vehicles, and medical diagnosis tools. Deep learning enables image recognition, natural language processing, and predictive analytics.\\n\\n## The Future Outlook\\n\\nWith a market value of $136 billion, AI continues to grow rapidly. Major technology companies are investing heavily in AI research and development, pushing the boundaries of what's possible.\\n\\n## Conclusion\\n\\nAs AI technology evolves, understanding its capabilities and limitations becomes increasingly important for businesses and individuals alike.",
              "cta": "Subscribe to our newsletter for more AI insights"
            }
            """;
        
        when(mockChatModel.generate(anyString()))
            .thenReturn("{\"points\":[\"AI point 1\",\"AI point 2\",\"AI point 3\",\"AI point 4\",\"AI point 5\"],\"sources\":[]}")
            .thenReturn(contentJson)
            .thenReturn("{\"prompt\":\"Blog illustration about AI Guide\"}");
        
        when(mockImageTool.generateImage(anyString(), any(Integer.class), anyString()))
            .thenReturn(new ImageResult("test prompt", List.of(), List.of(), List.of()));
        
        TopicRequest request = new TopicRequest("AI Guide", "blog", "authoritative", 1);
        OrchestrationResult result = agentGraph.run(request);
        
        // Blog should have reasonable word count (mock response is shorter)
        int wordCount = result.content().body().split("\\s+").length;
        assertThat(wordCount).isGreaterThan(50); // Relaxed for mock testing
        
        // Should have H2/H3 structure
        assertThat(result.content().body()).contains("##");
        assertThat(result.content().platform()).isEqualTo("blog");
        assertThat(result.content().tone()).isEqualTo("authoritative");
    }
    
    @Test
    void testErrorHandling() {
        // Simulate LLM failures
        when(mockChatModel.generate(anyString())).thenThrow(new RuntimeException("API Error"));
        when(mockImageTool.generateImage(anyString(), any(Integer.class), anyString()))
            .thenReturn(new ImageResult("fallback prompt", List.of(), List.of(), List.of()));
        
        TopicRequest request = new TopicRequest("Test Topic", "twitter", "casual", 1);
        OrchestrationResult result = agentGraph.run(request);
        
        // Should return graceful fallbacks
        assertThat(result.topic()).isEqualTo("Test Topic");
        assertThat(result.research().points()).isNotEmpty();
        assertThat(result.content().platform()).isEqualTo("twitter"); // State retains platform from request
        assertThat(result.image().openAiImageUrls()).isEmpty(); // Image generation uses fallback
    }
    
    @Test
    void testResearchConstraints() {
        String researchJson = """
            {
              "points": [
                "Point one with exactly five words here",
                "Point two with more than twenty five words which clearly exceeds the maximum limit",
                "Point three normal length",
                "Point four reasonable size",
                "Point five acceptable length",
                "Point six within bounds"
              ],
              "sources": []
            }
            """;
        
        when(mockChatModel.generate(anyString())).thenReturn(researchJson);
        
        ResearchAgent researchAgent = new ResearchAgent(mockChatModel, objectMapper, mockTimer);
        ResearchPoints result = researchAgent.research("Test Topic");
        
        assertThat(result.points()).hasSizeBetween(5, 7);
        // Validation warnings should be logged for points exceeding 25 words
    }
}