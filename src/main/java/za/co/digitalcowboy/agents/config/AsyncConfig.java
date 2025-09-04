package za.co.digitalcowboy.agents.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - number of threads to keep alive
        executor.setCorePoolSize(5);
        
        // Maximum pool size - maximum number of threads
        executor.setMaxPoolSize(20);
        
        // Queue capacity - number of tasks that can be queued
        executor.setQueueCapacity(100);
        
        // Thread name prefix for easy identification in logs
        executor.setThreadNamePrefix("AsyncGeneration-");
        
        // Graceful shutdown - wait for tasks to complete before shutting down
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        // Rejection policy - what to do when queue is full
        // CALLER_RUNS means the calling thread will execute the task
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        return executor;
    }
}