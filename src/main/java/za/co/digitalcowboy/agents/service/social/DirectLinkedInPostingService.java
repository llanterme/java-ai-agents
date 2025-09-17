package za.co.digitalcowboy.agents.service.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class DirectLinkedInPostingService {
    
    private static final Logger logger = LoggerFactory.getLogger(DirectLinkedInPostingService.class);
    
    private static final String IMAGES_API_URL = "https://api.linkedin.com/rest/images";
    private static final String POSTS_API_URL = "https://api.linkedin.com/rest/posts";
    private static final String LINKEDIN_VERSION = "202407";
    
    @Value("${linkedin.access-token:}")
    private String accessToken;
    
    @Value("${linkedin.person-id:}")
    private String personId;
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public DirectLinkedInPostingService() {
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    public boolean isConfigured() {
        return accessToken != null && !accessToken.isEmpty() && 
               personId != null && !personId.isEmpty();
    }
    
    public String postToLinkedIn(String text, String imagePath) {
        if (!isConfigured()) {
            throw new IllegalStateException("LinkedIn not configured. Set LINKEDIN_ACCESS_TOKEN and LINKEDIN_PERSON_ID environment variables.");
        }
        
        try {
            String authorUrn = "urn:li:person:" + personId;
            
            String imageUrn = null;
            if (imagePath != null && !imagePath.isEmpty()) {
                imageUrn = uploadImage(accessToken, authorUrn, imagePath);
            }
            
            return createPost(accessToken, authorUrn, text, imageUrn);
            
        } catch (Exception e) {
            logger.error("Failed to post to LinkedIn: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to post to LinkedIn", e);
        }
    }
    
    private String uploadImage(String accessToken, String authorUrn, String imagePath) throws IOException {
        // Initialize image upload request
        String initRequest = String.format("""
            {
                "initializeUploadRequest": {
                    "owner": "%s"
                }
            }
            """, authorUrn);
        
        RequestBody initBody = RequestBody.create(
            initRequest,
            MediaType.get("application/json")
        );
        
        Request initUploadRequest = new Request.Builder()
                .url(IMAGES_API_URL + "?action=initializeUpload")
                .post(initBody)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("LinkedIn-Version", LINKEDIN_VERSION)
                .addHeader("X-Restli-Protocol-Version", "2.0.0")
                .addHeader("Content-Type", "application/json")
                .build();
        
        logger.info("Initializing LinkedIn image upload for: {}", imagePath);
        
        try (Response initResponse = httpClient.newCall(initUploadRequest).execute()) {
            if (!initResponse.isSuccessful()) {
                String errorBody = initResponse.body() != null ? initResponse.body().string() : "No response body";
                logger.error("LinkedIn image upload initialization failed: {} - {}", initResponse.code(), errorBody);
                throw new RuntimeException("Failed to initialize image upload: " + initResponse.code() + " - " + errorBody);
            }
            
            String initResponseBody = initResponse.body().string();
            logger.debug("Image upload initialization response: {}", initResponseBody);
            
            JsonNode initResponseObj = objectMapper.readTree(initResponseBody);
            JsonNode value = initResponseObj.get("value");
            String uploadUrl = value.get("uploadUrl").asText();
            String imageUrn = value.get("image").asText();
            
            logger.info("Image upload initialized. URN: {}, Upload URL obtained", imageUrn);
            
            // Upload the actual image file
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                throw new RuntimeException("Image file not found: " + imagePath);
            }
            
            RequestBody imageBody = RequestBody.create(imageFile, MediaType.get("application/octet-stream"));
            
            Request uploadRequest = new Request.Builder()
                    .url(uploadUrl)
                    .put(imageBody)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();
            
            logger.info("Uploading image file to LinkedIn: {}", imageFile.getName());
            
            try (Response uploadResponse = httpClient.newCall(uploadRequest).execute()) {
                if (!uploadResponse.isSuccessful()) {
                    String errorBody = uploadResponse.body() != null ? uploadResponse.body().string() : "No response body";
                    logger.error("LinkedIn image file upload failed: {} - {}", uploadResponse.code(), errorBody);
                    throw new RuntimeException("Failed to upload image file: " + uploadResponse.code() + " - " + errorBody);
                }
                
                logger.info("Image successfully uploaded to LinkedIn. URN: {}", imageUrn);
                return imageUrn;
            }
        }
    }
    
    private String createPost(String accessToken, String authorUrn, String text, String imageUrn) throws IOException {
        // Create post request
        String postRequestBody;
        if (imageUrn != null) {
            postRequestBody = String.format("""
                {
                    "author": "%s",
                    "commentary": "%s",
                    "visibility": "PUBLIC",
                    "content": {
                        "media": {
                            "title": "Shared Image",
                            "id": "%s"
                        }
                    }
                }
                """, authorUrn, text.replace("\"", "\\\""), imageUrn);
        } else {
            postRequestBody = String.format("""
                {
                    "author": "%s",
                    "commentary": "%s",
                    "visibility": "PUBLIC"
                }
                """, authorUrn, text.replace("\"", "\\\""));
        }
        
        RequestBody postBody = RequestBody.create(
            postRequestBody,
            MediaType.get("application/json")
        );
        
        Request createPostRequest = new Request.Builder()
                .url(POSTS_API_URL)
                .post(postBody)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("LinkedIn-Version", LINKEDIN_VERSION)
                .addHeader("X-Restli-Protocol-Version", "2.0.0")
                .addHeader("Content-Type", "application/json")
                .build();
        
        logger.info("Creating LinkedIn post with text: {}", text.length() > 100 ? text.substring(0, 100) + "..." : text);
        
        try (Response postResponse = httpClient.newCall(createPostRequest).execute()) {
            if (!postResponse.isSuccessful()) {
                String errorBody = postResponse.body() != null ? postResponse.body().string() : "No response body";
                logger.error("LinkedIn post creation failed: {} - {}", postResponse.code(), errorBody);
                throw new RuntimeException("Failed to create LinkedIn post: " + postResponse.code() + " - " + errorBody);
            }
            
            String postResponseBody = postResponse.body().string();
            logger.debug("LinkedIn post creation response: {}", postResponseBody);
            
            JsonNode response = objectMapper.readTree(postResponseBody);
            String postId = response.get("id").asText();
            logger.info("LinkedIn post created successfully. Post ID: {}", postId);
            
            return postId;
        }
    }
}