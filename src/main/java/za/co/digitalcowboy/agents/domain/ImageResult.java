package za.co.digitalcowboy.agents.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ImageResult(
    @JsonProperty("prompt")
    String prompt,
    
    @JsonProperty("openAiImageUrls")
    List<String> openAiImageUrls,
    
    @JsonProperty("localImagePaths")
    List<String> localImagePaths,
    
    @JsonProperty("localImageUrls")
    List<String> localImageUrls
) {
    public ImageResult(String prompt, List<String> openAiImageUrls) {
        this(prompt, openAiImageUrls, List.of(), List.of());
    }
    
    public ImageResult(String prompt, List<String> openAiImageUrls, List<String> localImagePaths) {
        this(prompt, openAiImageUrls, localImagePaths, List.of());
    }
    
    public ImageResult {
        if (openAiImageUrls == null) {
            openAiImageUrls = List.of();
        }
        if (localImagePaths == null) {
            localImagePaths = List.of();
        }
        if (localImageUrls == null) {
            localImageUrls = List.of();
        }
    }
    
    public static ImageResult empty() {
        return new ImageResult("", List.of(), List.of(), List.of());
    }
}