package za.co.digitalcowboy.agents.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final Map<String, GenerationTask> taskStore = new ConcurrentHashMap<>();
    
    public AsyncGenerationService(AgentGraph agentGraph, @Qualifier("taskExecutor") Executor taskExecutor) {
        this.agentGraph = agentGraph;
        this.taskExecutor = taskExecutor;
    }
    
    public String startGeneration(TopicRequest request) {
        String taskId = UUID.randomUUID().toString();
        GenerationTask task = new GenerationTask(taskId, request);
        taskStore.put(taskId, task);
        
        log.info("Started async generation task: {} for topic: {}", taskId, request.topic());
        
        // Start async processing using CompletableFuture to avoid self-invocation issue
        CompletableFuture.runAsync(() -> executeGeneration(taskId), taskExecutor);
        
        return taskId;
    }
    
    private void executeGeneration(String taskId) {
        try {
            GenerationTask task = taskStore.get(taskId);
            if (task == null) {
                log.error("Task not found: {}", taskId);
                return;
            }
            
            // Update status to IN_PROGRESS
            updateTaskStatus(taskId, TaskStatus.IN_PROGRESS);
            
            log.info("Executing generation task: {}", taskId);
            
            // Execute the agent graph
            OrchestrationResult result = agentGraph.run(task.request());
            
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