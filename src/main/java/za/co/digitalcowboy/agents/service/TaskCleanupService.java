package za.co.digitalcowboy.agents.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TaskCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TaskCleanupService.class);
    
    private final AsyncGenerationService asyncGenerationService;
    
    public TaskCleanupService(AsyncGenerationService asyncGenerationService) {
        this.asyncGenerationService = asyncGenerationService;
    }
    
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void cleanupOldTasks() {
        int totalBefore = asyncGenerationService.getTotalTaskCount();
        int activeBefore = asyncGenerationService.getActiveTaskCount();
        
        asyncGenerationService.cleanupOldTasks();
        
        int totalAfter = asyncGenerationService.getTotalTaskCount();
        int activeAfter = asyncGenerationService.getActiveTaskCount();
        
        int cleaned = totalBefore - totalAfter;
        
        if (cleaned > 0) {
            log.info("Cleaned up {} old tasks. Active tasks: {} -> {}, Total tasks: {} -> {}",
                    cleaned, activeBefore, activeAfter, totalBefore, totalAfter);
        } else {
            log.debug("Task cleanup completed. Active: {}, Total: {}", activeAfter, totalAfter);
        }
    }
}