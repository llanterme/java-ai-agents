package za.co.digitalcowboy.agents.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import za.co.digitalcowboy.agents.domain.*;
import za.co.digitalcowboy.agents.graph.AgentGraph;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Service
public class AsyncGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AsyncGenerationService.class);

    private final AgentGraph agentGraph;
    private final Executor taskExecutor;
    private final GeneratedContentService generatedContentService;
    private final Map<String, GenerationTask> taskStore = new ConcurrentHashMap<>();

    public AsyncGenerationService(AgentGraph agentGraph,
                                 @Qualifier("taskExecutor") Executor taskExecutor,
                                 GeneratedContentService generatedContentService) {
        this.agentGraph = agentGraph;
        this.taskExecutor = taskExecutor;
        this.generatedContentService = generatedContentService;
    }
    
    public String startGeneration(TopicRequest request) {
        String taskId = UUID.randomUUID().toString();

        // Capture the current user context
        SecurityContext context = SecurityContextHolder.getContext();
        String username = context.getAuthentication() != null ?
            context.getAuthentication().getName() : null;

        GenerationTask task = new GenerationTask(taskId, request);
        taskStore.put(taskId, task);

        log.info("Started async generation task: {} for topic: {} by user: {}", taskId, request.topic(), username);

        // Start async processing using CompletableFuture to avoid self-invocation issue
        // Pass the security context to the async thread
        CompletableFuture.runAsync(() -> {
            SecurityContextHolder.setContext(context);
            executeGeneration(taskId, username);
        }, taskExecutor);

        return taskId;
    }
    
    private void executeGeneration(String taskId, String username) {
        try {
            GenerationTask task = taskStore.get(taskId);
            if (task == null) {
                log.error("Task not found: {}", taskId);
                return;
            }

            // Update status to IN_PROGRESS
            updateTaskStatus(taskId, TaskStatus.IN_PROGRESS);

            log.info("Executing generation task: {} for user: {}", taskId, username);

            // Execute the agent graph
            OrchestrationResult result = agentGraph.run(task.request());

            // Persist the generated content if user is authenticated
            if (username != null) {
                try {
                    GeneratedContent savedContent = generatedContentService.saveGeneratedContent(
                        username, task.request(), result);
                    // Add the ID to the result
                    result = result.withId(savedContent.getId());
                    log.info("Persisted content with ID: {} for task: {}", savedContent.getId(), taskId);
                } catch (Exception e) {
                    log.error("Failed to persist content for task: {}, continuing without persistence", taskId, e);
                }
            }

            // Update task with result
            updateTaskWithResult(taskId, result);

            log.info("Completed generation task: {}", taskId);

        } catch (Exception e) {
            log.error("Error executing generation task: {}", taskId, e);
            updateTaskWithError(taskId, e.getMessage());
        }
    }
    
    public GenerationTask getTask(String taskId) {
        return taskStore.get(taskId);
    }
    
    public TaskStatus getTaskStatus(String taskId) {
        GenerationTask task = taskStore.get(taskId);
        return task != null ? task.status() : null;
    }
    
    public OrchestrationResult getTaskResult(String taskId) {
        GenerationTask task = taskStore.get(taskId);
        if (task != null && task.status() == TaskStatus.COMPLETED) {
            return task.result();
        }
        return null;
    }
    
    private void updateTaskStatus(String taskId, TaskStatus status) {
        GenerationTask currentTask = taskStore.get(taskId);
        if (currentTask != null) {
            GenerationTask updatedTask = currentTask.withStatus(status);
            taskStore.put(taskId, updatedTask);
        }
    }
    
    private void updateTaskWithResult(String taskId, OrchestrationResult result) {
        GenerationTask currentTask = taskStore.get(taskId);
        if (currentTask != null) {
            GenerationTask updatedTask = currentTask.withResult(result);
            taskStore.put(taskId, updatedTask);
        }
    }
    
    private void updateTaskWithError(String taskId, String error) {
        GenerationTask currentTask = taskStore.get(taskId);
        if (currentTask != null) {
            GenerationTask updatedTask = currentTask.withError(error);
            taskStore.put(taskId, updatedTask);
        }
    }
    
    // Cleanup old tasks (called by scheduled task)
    public void cleanupOldTasks() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        taskStore.entrySet().removeIf(entry -> {
            GenerationTask task = entry.getValue();
            return task.createdAt().isBefore(cutoff);
        });
    }
    
    public int getActiveTaskCount() {
        return (int) taskStore.values().stream()
                .filter(task -> !task.isCompleted())
                .count();
    }
    
    public int getTotalTaskCount() {
        return taskStore.size();
    }
}