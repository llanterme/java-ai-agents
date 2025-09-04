package za.co.digitalcowboy.agents.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.digitalcowboy.agents.service.AsyncGenerationService;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    
    private final AsyncGenerationService asyncGenerationService;
    
    public HealthController(AsyncGenerationService asyncGenerationService) {
        this.asyncGenerationService = asyncGenerationService;
    }
    
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        int activeTasks = asyncGenerationService.getActiveTaskCount();
        int totalTasks = asyncGenerationService.getTotalTaskCount();
        
        return ResponseEntity.ok(new HealthResponse(
            "healthy", 
            System.currentTimeMillis(),
            activeTasks,
            totalTasks
        ));
    }
    
    public record HealthResponse(
        String status, 
        long timestamp,
        int activeTasks,
        int totalTasks
    ) {}
}