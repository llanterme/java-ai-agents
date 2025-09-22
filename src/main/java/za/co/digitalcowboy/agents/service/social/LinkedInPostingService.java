package za.co.digitalcowboy.agents.service.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import za.co.digitalcowboy.agents.domain.oauth.ConnectedAccount;
import za.co.digitalcowboy.agents.domain.social.*;
import za.co.digitalcowboy.agents.service.oauth.OAuthConnectionService;
import za.co.digitalcowboy.agents.service.oauth.OAuthProviderService;
import za.co.digitalcowboy.agents.service.oauth.TokenEncryptionService;

import java.io.File;
import java.io.IOException;

@Service
public class LinkedInPostingService {
    
    private static final Logger logger = LoggerFactory.getLogger(LinkedInPostingService.class);
    
    private static final String IMAGES_API_URL = "https://api.linkedin.com/v2/assets";
    private static final String POSTS_API_URL = "https://api.linkedin.com/v2/ugcPosts";
    
    private final OAuthConnectionService oauthConnectionService;
    private final TokenEncryptionService encryptionService;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public LinkedInPostingService(OAuthConnectionService oauthConnectionService, TokenEncryptionService encryptionService) {
        this.oauthConnectionService = oauthConnectionService;
        this.encryptionService = encryptionService;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    public LinkedInPostResponse postToLinkedIn(Long userId, String text, String imagePath) {
        try {
            // Get the user's LinkedIn connection
            ConnectedAccount connection = oauthConnectionService.getActiveConnection(userId, "linkedin")
                .orElseThrow(() -> new IllegalStateException("No active LinkedIn connection found for user " + userId));
            
            // Get user info to create author URN
            OAuthProviderService.UserInfo userInfo = oauthConnectionService.getUserInfo(userId, "linkedin");
            String authorUrn = "urn:li:person:" + userInfo.getId();
            
            // Decrypt the access token before using it for LinkedIn API calls
            String decryptedAccessToken = encryptionService.decrypt(connection.getAccessToken());
            
            // DEBUG: Log decrypted token characteristics
            logger.debug("DEBUG: Using decrypted token - length: {}, starts with: {}..., ends with: ...{}", 
                decryptedAccessToken != null ? decryptedAccessToken.length() : 0, 
                decryptedAccessToken != null && decryptedAccessToken.length() > 10 ? decryptedAccessToken.substring(0, 5) : "null",
                decryptedAccessToken != null && decryptedAccessToken.length() > 10 ? decryptedAccessToken.substring(decryptedAccessToken.length() - 5) : "null");
            
            String imageUrn = null;
            if (imagePath != null && !imagePath.isEmpty()) {
                // Step 1 & 2: Upload image and get URN
                imageUrn = uploadImage(decryptedAccessToken, authorUrn, imagePath);
            }
            
            // Step 3: Create the post
            return createPost(decryptedAccessToken, authorUrn, text, imageUrn);
            
        } catch (Exception e) {
            logger.error("Failed to post to LinkedIn for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to post to LinkedIn", e);
        }
    }
    
    private String uploadImage(String accessToken, String authorUrn, String imagePath) throws IOException {
        // Initialize image upload request for v2 API
        String initRequest = String.format("""
            {
                "registerUploadRequest": {
                    "recipes": ["urn:li:digitalmediaRecipe:feedshare-image"],
                    "owner": "%s",
                    "serviceRelationships": [
                        {
                            "relationshipType": "OWNER",
                            "identifier": "urn:li:userGeneratedContent"
                        }
                    ]
                }
            }
            """, authorUrn);
        
        RequestBody initBody = RequestBody.create(
            initRequest,
            MediaType.get("application/json")
        );
        
        Request initUploadRequest = new Request.Builder()
                .url(IMAGES_API_URL + "?action=registerUpload")
                .post(initBody)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("X-Restli-Protocol-Version", "2.0.0")
                .addHeader("Content-Type", "application/json")
                .build();
        
        logger.info("Initializing LinkedIn image upload for image: {}", imagePath);
        
        // DEBUG: Log token characteristics (first/last chars for security)
        if (accessToken != null && accessToken.length() > 10) {
            logger.info("DEBUG: Access token length: {}, starts with: {}..., ends with: ...{}", 
                accessToken.length(), 
                accessToken.substring(0, 5),
                accessToken.substring(accessToken.length() - 5));
        } else {
            logger.error("DEBUG: Access token is null or too short: {}", accessToken);
        }
        
        try (Response initResponse = httpClient.newCall(initUploadRequest).execute()) {
            if (!initResponse.isSuccessful()) {
                String errorBody = initResponse.body() != null ? initResponse.body().string() : "No response body";
                logger.error("LinkedIn image upload initialization failed: {} - {}", initResponse.code(), errorBody);
                throw new RuntimeException("Failed to initialize image upload: " + initResponse.code() + " - " + errorBody);
            }
            
            String initResponseBody = initResponse.body().string();
            logger.debug("Image upload initialization response: {}", initResponseBody);
            
            com.fasterxml.jackson.databind.JsonNode initResponseObj = objectMapper.readTree(initResponseBody);
            com.fasterxml.jackson.databind.JsonNode value = initResponseObj.get("value");
            String uploadUrl = value.get("uploadMechanism")
                .get("com.linkedin.digitalmedia.uploading.MediaUploadHttpRequest")
                .get("uploadUrl").asText();
            String imageUrn = value.get("asset").asText();
            
            logger.info("Image upload initialized. URN: {}, Upload URL obtained", imageUrn);
            
            // Step 2: Upload the actual image file
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
    
    private LinkedInPostResponse createPost(String accessToken, String authorUrn, String text, String imageUrn) throws IOException {
        // Create post request for v2 API (ugcPosts)
        String postRequestBody;
        if (imageUrn != null) {
            postRequestBody = String.format("""
                {
                    "author": "%s",
                    "lifecycleState": "PUBLISHED",
                    "specificContent": {
                        "com.linkedin.ugc.ShareContent": {
                            "shareCommentary": {
                                "text": "%s"
                            },
                            "shareMediaCategory": "IMAGE",
                            "media": [
                                {
                                    "status": "READY",
                                    "description": {
                                        "text": "Image shared via API"
                                    },
                                    "media": "%s",
                                    "title": {
                                        "text": "Shared Image"
                                    }
                                }
                            ]
                        }
                    },
                    "visibility": {
                        "com.linkedin.ugc.MemberNetworkVisibility": "PUBLIC"
                    }
                }
                """, authorUrn, escapeJsonString(text), imageUrn);
        } else {
            postRequestBody = String.format("""
                {
                    "author": "%s",
                    "lifecycleState": "PUBLISHED",
                    "specificContent": {
                        "com.linkedin.ugc.ShareContent": {
                            "shareCommentary": {
                                "text": "%s"
                            },
                            "shareMediaCategory": "NONE"
                        }
                    },
                    "visibility": {
                        "com.linkedin.ugc.MemberNetworkVisibility": "PUBLIC"
                    }
                }
                """, authorUrn, escapeJsonString(text));
        }
        
        // Debug: Log the request body to verify JSON structure
        logger.debug("LinkedIn post request body: {}", postRequestBody);

        RequestBody postBody = RequestBody.create(
            postRequestBody,
            MediaType.get("application/json")
        );
        
        Request createPostRequest = new Request.Builder()
                .url(POSTS_API_URL)
                .post(postBody)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("X-Restli-Protocol-Version", "2.0.0")
                .addHeader("Content-Type", "application/json")
                .build();
        
        logger.info("Creating LinkedIn post with text: {}", text.length() > 100 ? text.substring(0, 100) + "..." : text);
        
        // DEBUG: Log token characteristics in createPost method
        logger.info("DEBUG: Access token for post creation - length: {}, starts with: {}..., ends with: ...{}", 
            accessToken != null ? accessToken.length() : 0, 
            accessToken != null && accessToken.length() > 10 ? accessToken.substring(0, 5) : "null",
            accessToken != null && accessToken.length() > 10 ? accessToken.substring(accessToken.length() - 5) : "null");
        
        try (Response postResponse = httpClient.newCall(createPostRequest).execute()) {
            if (!postResponse.isSuccessful()) {
                String errorBody = postResponse.body() != null ? postResponse.body().string() : "No response body";
                logger.error("LinkedIn post creation failed: {} - {}", postResponse.code(), errorBody);
                throw new RuntimeException("Failed to create LinkedIn post: " + postResponse.code() + " - " + errorBody);
            }
            
            String postResponseBody = postResponse.body().string();
            logger.debug("LinkedIn post creation response: {}", postResponseBody);
            
            com.fasterxml.jackson.databind.JsonNode response = objectMapper.readTree(postResponseBody);
            String postId = response.get("id").asText();
            logger.info("LinkedIn post created successfully. Post ID: {}", postId);
            
            // Create response object manually (aligned with working implementation)
            LinkedInPostResponse linkedInResponse = new LinkedInPostResponse();
            linkedInResponse.setId(postId);
            linkedInResponse.setState("PUBLISHED");
            
            return linkedInResponse;
        }
    }
    
    /**
     * Properly escapes a string for use in JSON
     */
    private String escapeJsonString(String text) {
        if (text == null) return "";

        return text
            .replace("\\", "\\\\")  // Escape backslashes first
            .replace("\"", "\\\"")   // Escape quotes
            .replace("\n", "\\n")    // Escape newlines
            .replace("\r", "\\r")    // Escape carriage returns
            .replace("\t", "\\t")    // Escape tabs
            .replace("\b", "\\b")    // Escape backspace
            .replace("\f", "\\f");   // Escape form feed
    }

    public boolean validateConnection(Long userId) {
        try {
            ConnectedAccount connection = oauthConnectionService.getActiveConnection(userId, "linkedin")
                .orElse(null);
            
            if (connection == null) {
                logger.warn("No LinkedIn connection found for user {}", userId);
                return false;
            }
            
            if (connection.isExpired()) {
                logger.warn("LinkedIn connection expired for user {}", userId);
                return false;
            }
            
            // Check if the connection has posting permissions
            if (!connection.getScopesList().contains("w_member_social")) {
                logger.warn("LinkedIn connection for user {} lacks posting permissions (w_member_social scope)", userId);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to validate LinkedIn connection for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
}