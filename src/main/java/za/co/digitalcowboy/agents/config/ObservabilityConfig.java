package za.co.digitalcowboy.agents.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    public Timer researchAgentTimer(MeterRegistry meterRegistry) {
        return Timer.builder("agent.research.duration")
                .description("Time taken by research agent")
                .register(meterRegistry);
    }

    @Bean
    public Timer contentAgentTimer(MeterRegistry meterRegistry) {
        return Timer.builder("agent.content.duration")
                .description("Time taken by content agent")
                .register(meterRegistry);
    }

    @Bean
    public Timer imageAgentTimer(MeterRegistry meterRegistry) {
        return Timer.builder("agent.image.duration")
                .description("Time taken by image agent")
                .register(meterRegistry);
    }

    @Bean
    public Timer orchestrationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("orchestration.duration")
                .description("Total orchestration time")
                .register(meterRegistry);
    }
}