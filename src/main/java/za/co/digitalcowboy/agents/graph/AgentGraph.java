package za.co.digitalcowboy.agents.graph;

import za.co.digitalcowboy.agents.agents.ContentAgent;
import za.co.digitalcowboy.agents.agents.ImageAgent;
import za.co.digitalcowboy.agents.agents.ResearchAgent;
import za.co.digitalcowboy.agents.domain.OrchestrationResult;
import za.co.digitalcowboy.agents.domain.TopicRequest;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AgentGraph {
    
    private static final Logger log = LoggerFactory.getLogger(AgentGraph.class);
    
    private final ResearchAgent researchAgent;
    private final ContentAgent contentAgent;
    private final ImageAgent imageAgent;
    private final Timer orchestrationTimer;
    
    public AgentGraph(ResearchAgent researchAgent, ContentAgent contentAgent, 
                     ImageAgent imageAgent, Timer orchestrationTimer) {
        this.researchAgent = researchAgent;
        this.contentAgent = contentAgent;
        this.imageAgent = imageAgent;
        this.orchestrationTimer = orchestrationTimer;
    }
    
    public OrchestrationResult run(TopicRequest request) {
        try {
            return orchestrationTimer.recordCallable(() -> {
                log.info("Starting orchestration for topic: {} on platform: {} with tone: {}", 
                    request.topic(), request.platform(), request.tone());
                
                // Initialize agent state
                AgentState state = new AgentState(request);
                
                // Execute workflow nodes in sequence
                executeResearchNode(state);
                executeContentNode(state);
                executeImageNode(state);
                
                OrchestrationResult result = state.toResult();
                log.info("Orchestration completed successfully");
                return result;
            });
        } catch (Exception e) {
            log.error("Orchestration failed", e);
            return OrchestrationResult.empty(request.topic());
        }
    }
    
    private void executeResearchNode(AgentState state) {
        log.debug("Executing research node");
        try {
            var research = researchAgent.research(state.getTopic());
            state.setResearch(research);
            log.debug("Research node completed with {} points", research.points().size());
        } catch (Exception e) {
            log.error("Research node failed", e);
            // State retains empty research, workflow continues
        }
    }
    
    private void executeContentNode(AgentState state) {
        log.debug("Executing content node");
        try {
            var content = contentAgent.createContent(state.getResearch(), state.getPlatform(), state.getTone());
            state.setContent(content);
            log.debug("Content node completed for platform: {}", content.platform());
        } catch (Exception e) {
            log.error("Content node failed", e);
            // State retains empty content, workflow continues
        }
    }
    
    private void executeImageNode(AgentState state) {
        log.debug("Executing image node");
        try {
            var image = imageAgent.generateImage(state.getContent(), state.getImageCount(), state.getTopic());
            state.setImage(image);
            log.debug("Image node completed with {} URLs", image.openAiImageUrls().size());
            if (!image.localImagePaths().isEmpty()) {
                log.debug("Image node completed with {} local files", image.localImagePaths().size());
            }
        } catch (Exception e) {
            log.error("Image node failed", e);
            // State retains empty image result, workflow continues
        }
    }
}