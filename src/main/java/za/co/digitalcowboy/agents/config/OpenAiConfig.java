package za.co.digitalcowboy.agents.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenAiConfig {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.text-model}")
    private String textModel;

    @Value("${openai.image-model}")
    private String imageModel;

    @Value("${openai.timeout-ms}")
    private int timeoutMs;

    @Value("${openai.image-timeout-ms}")
    private int imageTimeoutMs;

    @Value("${openai.temperature}")
    private double temperature;

    @Value("${openai.max-tokens}")
    private int maxTokens;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured. Set OPENAI_API_KEY environment variable.");
        }
        
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(textModel)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Bean
    public OkHttpClient httpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .readTimeout(Duration.ofMillis(timeoutMs))
                .writeTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Bean("imageHttpClient")
    public OkHttpClient imageHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(imageTimeoutMs))
                .readTimeout(Duration.ofMillis(imageTimeoutMs))
                .writeTimeout(Duration.ofMillis(imageTimeoutMs))
                .build();
    }

    @Bean
    public OpenAiProperties openAiProperties() {
        return new OpenAiProperties(apiKey, textModel, imageModel, timeoutMs, imageTimeoutMs, temperature, maxTokens);
    }

    public record OpenAiProperties(
            String apiKey,
            String textModel,
            String imageModel,
            int timeoutMs,
            int imageTimeoutMs,
            double temperature,
            int maxTokens
    ) {}
}