package za.co.digitalcowboy.agents.tools;

import za.co.digitalcowboy.agents.config.OpenAiConfig.OpenAiProperties;
import za.co.digitalcowboy.agents.domain.ImageResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenAiImageTool {
    
    private static final Logger log = LoggerFactory.getLogger(OpenAiImageTool.class);
    private static final String OPENAI_IMAGES_URL = "https://api.openai.com/v1/images/generations";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties openAiProperties;
    private final ImageDownloadService imageDownloadService;
    private final boolean keepRemoteUrl;
    private final String baseUrl;
    
    public OpenAiImageTool(@Qualifier("imageHttpClient") OkHttpClient httpClient, ObjectMapper objectMapper, 
                          OpenAiProperties openAiProperties, ImageDownloadService imageDownloadService,
                          @Value("${images.keep-remote-url}") boolean keepRemoteUrl,
                          @Value("${images.base-url}") String baseUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.openAiProperties = openAiProperties;
        this.imageDownloadService = imageDownloadService;
        this.keepRemoteUrl = keepRemoteUrl;
        this.baseUrl = baseUrl;
    }
    
    public ImageResult generateImage(String prompt, int count) {
        return generateImage(prompt, count, prompt); // Use prompt as default topic
    }
    
    public ImageResult generateImage(String prompt, int count, String topic) {
        try {
            log.debug("Generating {} image(s) for prompt: {}", count, prompt);
            
            var requestBody = new ImageGenerationRequest(
                openAiProperties.imageModel(),
                prompt,
                count,
                "1024x1024"
            );
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            RequestBody body = RequestBody.create(
                jsonBody, 
                MediaType.get("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                    .url(OPENAI_IMAGES_URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + openAiProperties.apiKey())
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    log.error("OpenAI Images API error: {} - {}", response.code(), errorBody);
                    throw new RuntimeException("OpenAI Images API failed: " + response.code());
                }
                
                String responseBody = response.body().string();
                ImageGenerationResponse apiResponse = objectMapper.readValue(responseBody, ImageGenerationResponse.class);
                
                List<String> imageUrls = apiResponse.data().stream()
                        .map(ImageData::url)
                        .toList();
                
                log.debug("Successfully generated {} image(s)", imageUrls.size());
                
                // Download images locally and build HTTP URLs
                List<String> localPaths = new ArrayList<>();
                List<String> localImageUrls = new ArrayList<>();
                
                for (String imageUrl : imageUrls) {
                    ImageDownloadService.ImageDownloadResult downloadResult = 
                            imageDownloadService.downloadImageDetailed(imageUrl, topic);
                    localPaths.add(downloadResult.localPath());
                    
                    // Build HTTP URL if image was downloaded locally
                    if (downloadResult.filename() != null) {
                        String httpUrl = baseUrl + "/generated-image/" + downloadResult.filename();
                        localImageUrls.add(httpUrl);
                    }
                }
                
                // Return based on configuration
                if (keepRemoteUrl) {
                    return new ImageResult(prompt, imageUrls, localPaths, localImageUrls);
                } else {
                    return new ImageResult(prompt, List.of(), localPaths, localImageUrls);
                }
            }
        } catch (IOException e) {
            log.error("Error generating image", e);
            throw new RuntimeException("Failed to generate image: " + e.getMessage(), e);
        }
    }
    
    private record ImageGenerationRequest(
            @JsonProperty("model") String model,
            @JsonProperty("prompt") String prompt,
            @JsonProperty("n") int n,
            @JsonProperty("size") String size
    ) {}
    
    private record ImageGenerationResponse(
            @JsonProperty("data") List<ImageData> data
    ) {}
    
    private record ImageData(
            @JsonProperty("url") String url
    ) {}
}