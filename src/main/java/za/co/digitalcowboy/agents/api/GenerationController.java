package za.co.digitalcowboy.agents.api;

import za.co.digitalcowboy.agents.domain.*;
import za.co.digitalcowboy.agents.service.AsyncGenerationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/generate")
@CrossOrigin(origins = "*")
public class GenerationController {
    
    private static final Logger log = LoggerFactory.getLogger(GenerationController.class);
    
    private final AsyncGenerationService asyncGenerationService;
    
    public GenerationController(AsyncGenerationService asyncGenerationService) {
        this.asyncGenerationService = asyncGenerationService;
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