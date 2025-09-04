package za.co.digitalcowboy.agents.api;

import za.co.digitalcowboy.agents.domain.OrchestrationResult;
import za.co.digitalcowboy.agents.domain.TopicRequest;
import za.co.digitalcowboy.agents.graph.AgentGraph;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class GenerationController {
    
    private static final Logger log = LoggerFactory.getLogger(GenerationController.class);
    
    private final AgentGraph agentGraph;
    
    public GenerationController(AgentGraph agentGraph) {
        this.agentGraph = agentGraph;
    }
    
    @PostMapping("/generate")
    public ResponseEntity<OrchestrationResult> generate(@Valid @RequestBody TopicRequest request) {
        log.info("Received generation request: topic={}, platform={}, tone={}, imageCount={}", 
            request.topic(), request.platform(), request.tone(), request.imageCount());
        
        try {
            OrchestrationResult result = agentGraph.run(request);
            
            log.info("Generation completed successfully for topic: {}", request.topic());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Generation failed for request: {}", request, e);
            
            // Return partial result with error details
            OrchestrationResult errorResult = OrchestrationResult.empty(request.topic());
            return ResponseEntity.status(500).body(errorResult);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("healthy", System.currentTimeMillis()));
    }
    
    public record HealthResponse(String status, long timestamp) {}
}