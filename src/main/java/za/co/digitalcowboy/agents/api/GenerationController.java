package za.co.digitalcowboy.agents.api;

import za.co.digitalcowboy.agents.domain.*;
import za.co.digitalcowboy.agents.graph.AgentGraph;
import za.co.digitalcowboy.agents.service.AsyncGenerationService;
import za.co.digitalcowboy.agents.service.GeneratedContentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/generate")
public class GenerationController {

    private static final Logger log = LoggerFactory.getLogger(GenerationController.class);

    private final AsyncGenerationService asyncGenerationService;
    private final AgentGraph agentGraph;
    private final GeneratedContentService generatedContentService;

    public GenerationController(AsyncGenerationService asyncGenerationService,
                              AgentGraph agentGraph,
                              GeneratedContentService generatedContentService) {
        this.asyncGenerationService = asyncGenerationService;
        this.agentGraph = agentGraph;
        this.generatedContentService = generatedContentService;
    }
    
    @PostMapping
    public ResponseEntity<OrchestrationResult> generateContent(
            @Valid @RequestBody TopicRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Synchronous generation for topic: {} on platform: {} by user: {}",
            request.topic(), request.platform(), userDetails.getUsername());

        // Execute the agent graph synchronously
        OrchestrationResult result = agentGraph.run(request);

        // Persist the generated content
        GeneratedContent savedContent = generatedContentService.saveGeneratedContent(
            userDetails.getUsername(), request, result);

        // Create a new result with the persisted content ID
        OrchestrationResult resultWithId = result.withId(savedContent.getId());

        log.info("Completed synchronous generation with content ID: {}", savedContent.getId());
        return ResponseEntity.ok(resultWithId);
    }

    @PostMapping("/async")
    public ResponseEntity<AsyncGenerationResponse> startAsyncGeneration(@Valid @RequestBody TopicRequest request) {
        log.info("Starting async generation for topic: {} on platform: {}", request.topic(), request.platform());
        
        String taskId = asyncGenerationService.startGeneration(request);
        AsyncGenerationResponse response = AsyncGenerationResponse.forTask(taskId);
        
        log.info("Started async generation task: {} for topic: {}", taskId, request.topic());
        return ResponseEntity.accepted().body(response);
    }
    
    @GetMapping("/status/{taskId}")
    public ResponseEntity<GenerationTask> getTaskStatus(@PathVariable String taskId) {
        GenerationTask task = asyncGenerationService.getTask(taskId);
        
        if (task == null) {
            log.warn("Task not found: {}", taskId);
            return ResponseEntity.notFound().build();
        }
        
        log.debug("Retrieved status for task: {} - Status: {}", taskId, task.status());
        return ResponseEntity.ok(task);
    }
    
    @GetMapping("/result/{taskId}")
    public ResponseEntity<OrchestrationResult> getTaskResult(@PathVariable String taskId) {
        GenerationTask task = asyncGenerationService.getTask(taskId);
        
        if (task == null) {
            log.warn("Task not found: {}", taskId);
            return ResponseEntity.notFound().build();
        }
        
        if (task.status() != TaskStatus.COMPLETED) {
            log.warn("Task {} not completed yet. Status: {}", taskId, task.status());
            return ResponseEntity.badRequest().build();
        }
        
        OrchestrationResult result = task.result();
        if (result == null) {
            log.error("Task {} marked as completed but has no result", taskId);
            return ResponseEntity.internalServerError().build();
        }
        
        log.info("Retrieved result for completed task: {}", taskId);
        return ResponseEntity.ok(result);
    }
}