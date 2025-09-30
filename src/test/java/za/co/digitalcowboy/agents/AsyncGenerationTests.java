package za.co.digitalcowboy.agents;

import za.co.digitalcowboy.agents.agents.ContentAgent;
import za.co.digitalcowboy.agents.agents.ImageAgent;
import za.co.digitalcowboy.agents.agents.ResearchAgent;
import za.co.digitalcowboy.agents.domain.*;
import za.co.digitalcowboy.agents.graph.AgentGraph;
import za.co.digitalcowboy.agents.service.AsyncGenerationService;
import za.co.digitalcowboy.agents.tools.OpenAiImageTool;
import za.co.digitalcowboy.agents.tools.SerpApiSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class AsyncGenerationTests {
    
    @Mock
    private ChatLanguageModel mockChatModel;
    
    @Mock
    private OpenAiImageTool mockImageTool;
    
    @Mock
    private Executor mockExecutor;
    
    @Mock
    private SerpApiSearchService mockSearchService;
    
    private AsyncGenerationService asyncGenerationService;
    private AgentGraph agentGraph;
    
    @BeforeEach
    void setUp() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        Timer mockTimer = Timer.builder("test.timer").register(meterRegistry);
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Create agents with mocks
        ResearchAgent researchAgent = new ResearchAgent(mockChatModel, objectMapper, mockTimer, mockSearchService);
        ContentAgent contentAgent = new ContentAgent(mockChatModel, objectMapper, mockTimer);
        ImageAgent imageAgent = new ImageAgent(mockChatModel, objectMapper, mockImageTool, mockTimer);
        
        // Create agent graph
        agentGraph = new AgentGraph(researchAgent, contentAgent, imageAgent, mockTimer);
        
        // Create async service with mock executor and null content service (not needed for these tests)
        asyncGenerationService = new AsyncGenerationService(agentGraph, mockExecutor, null);
    }
    
    @Test
    void testAsyncGenerationStartsSuccessfully() {
        TopicRequest request = new TopicRequest("AI Testing", "twitter", "casual", 1);
        
        String taskId = asyncGenerationService.startGeneration(request);
        
        assertThat(taskId).isNotNull().isNotEmpty();
        
        GenerationTask task = asyncGenerationService.getTask(taskId);
        assertThat(task).isNotNull();
        assertThat(task.id()).isEqualTo(taskId);
        assertThat(task.request()).isEqualTo(request);
        // In test environment without @Async config, it runs synchronously and completes
        assertThat(task.status()).isIn(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED);
    }
    
    @Test
    void testTaskCompletion() throws InterruptedException {
        TopicRequest request = new TopicRequest("AI Testing", "twitter", "casual", 1);

        String taskId = asyncGenerationService.startGeneration(request);

        GenerationTask task = asyncGenerationService.getTask(taskId);

        // Task should be created but may not be completed yet in async scenario
        assertThat(task).isNotNull();
        assertThat(task.status()).isIn(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED);
        // If completed, verify result structure
        if (task.isCompleted()) {
            assertThat(task.result()).isNotNull();
            assertThat(task.error()).isNull();
        }
    }
    
    @Test
    void testTaskOperations() {
        // Test task creation
        TopicRequest request = new TopicRequest("AI Testing", "twitter", "casual", 1);
        String taskId = asyncGenerationService.startGeneration(request);
        
        // Test task retrieval
        GenerationTask task = asyncGenerationService.getTask(taskId);
        assertThat(task).isNotNull();
        
        // Test non-existent task
        String nonExistentTaskId = "non-existent-task";
        GenerationTask nonExistentTask = asyncGenerationService.getTask(nonExistentTaskId);
        assertThat(nonExistentTask).isNull();
        
        TaskStatus status = asyncGenerationService.getTaskStatus(nonExistentTaskId);
        assertThat(status).isNull();
        
        OrchestrationResult result = asyncGenerationService.getTaskResult(nonExistentTaskId);
        assertThat(result).isNull();
    }
    
    
    @Test
    void testTaskCleanup() {
        int initialCount = asyncGenerationService.getTotalTaskCount();
        
        // Create some tasks
        TopicRequest request = new TopicRequest("AI Testing", "twitter", "casual", 1);
        asyncGenerationService.startGeneration(request);
        asyncGenerationService.startGeneration(request);
        
        assertThat(asyncGenerationService.getTotalTaskCount()).isEqualTo(initialCount + 2);
        
        // Cleanup should not remove recent tasks
        asyncGenerationService.cleanupOldTasks();
        assertThat(asyncGenerationService.getTotalTaskCount()).isEqualTo(initialCount + 2);
    }
    
    @Test 
    void testTaskCount() {
        int initialTotal = asyncGenerationService.getTotalTaskCount();
        int initialActive = asyncGenerationService.getActiveTaskCount();
        
        TopicRequest request = new TopicRequest("AI Testing", "twitter", "casual", 1);
        asyncGenerationService.startGeneration(request);
        
        assertThat(asyncGenerationService.getTotalTaskCount()).isEqualTo(initialTotal + 1);
        // Active count may be 0 if task completed synchronously
        assertThat(asyncGenerationService.getActiveTaskCount()).isGreaterThanOrEqualTo(initialActive);
    }
}