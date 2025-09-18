package za.co.digitalcowboy.agents.api;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.digitalcowboy.agents.domain.ErrorResponse;
import za.co.digitalcowboy.agents.domain.social.LinkedInPostResponse;
import za.co.digitalcowboy.agents.domain.social.dto.SocialConnectionStatus;
import za.co.digitalcowboy.agents.domain.social.dto.SocialPostRequest;
import za.co.digitalcowboy.agents.domain.social.dto.SocialPostResponse;
import za.co.digitalcowboy.agents.domain.dto.GeneratedContentResponse;
import za.co.digitalcowboy.agents.repository.UserRepository;
import za.co.digitalcowboy.agents.service.GeneratedContentService;
import za.co.digitalcowboy.agents.service.social.LinkedInPostingService;
import za.co.digitalcowboy.agents.service.social.DirectLinkedInPostingService;

@RestController
@RequestMapping("/api/v1/social")
public class SocialController {
    
    private static final Logger logger = LoggerFactory.getLogger(SocialController.class);
    
    private final LinkedInPostingService linkedInPostingService;
    private final DirectLinkedInPostingService directLinkedInPostingService;
    private final UserRepository userRepository;
    private final GeneratedContentService generatedContentService;

    public SocialController(LinkedInPostingService linkedInPostingService,
                           DirectLinkedInPostingService directLinkedInPostingService,
                           UserRepository userRepository,
                           GeneratedContentService generatedContentService) {
        this.linkedInPostingService = linkedInPostingService;
        this.directLinkedInPostingService = directLinkedInPostingService;
        this.userRepository = userRepository;
        this.generatedContentService = generatedContentService;
    }
    
    @PostMapping("/linkedin/post")
    public ResponseEntity<?> postToLinkedIn(
            @Valid @RequestBody SocialPostRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        logger.info("LinkedIn post request for content ID: {} from user: {}", request.getId(), userDetails.getUsername());

        try {
            // Get user ID from authenticated user
            Long userId = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

            // Fetch content from database
            GeneratedContentResponse content;
            try {
                content = generatedContentService.getContentById(request.getId(), userDetails.getUsername());
            } catch (IllegalArgumentException e) {
                logger.warn("Content not found or access denied for content ID: {} by user: {}", request.getId(), userDetails.getUsername());
                return ResponseEntity.badRequest().body(new ErrorResponse(
                    "Content not found or you don't have permission to access it",
                    400
                ));
            }

            // Extract text and image path from content
            String postText = content.content().body();
            String imagePath = null;

            // Get the first local image path if available
            if (content.image() != null && content.image().localImagePaths() != null && !content.image().localImagePaths().isEmpty()) {
                imagePath = content.image().localImagePaths().get(0);
            }

            logger.info("Posting content '{}' to LinkedIn for user: {}", content.topic(), userDetails.getUsername());

            // Try OAuth approach first
            try {
                if (linkedInPostingService.validateConnection(userId)) {
                    LinkedInPostResponse response = linkedInPostingService.postToLinkedIn(
                        userId,
                        postText,
                        imagePath
                    );

                    logger.info("LinkedIn post created successfully via OAuth for user {}: {}", userId, response.getId());

                    return ResponseEntity.ok(new SocialPostResponse(
                        response.getId(),
                        response.getState(),
                        response.getLinkedInPostUrl(),
                        "Post created successfully via OAuth"
                    ));
                }
            } catch (Exception oauthError) {
                logger.warn("OAuth posting failed for user {}, falling back to direct token approach: {}", userId, oauthError.getMessage());
            }

            // Fallback to direct token approach if configured for testing
            if (directLinkedInPostingService != null && directLinkedInPostingService.isConfigured()) {
                try {
                    logger.info("Attempting direct token fallback for user {}", userId);
                    String postId = directLinkedInPostingService.postToLinkedIn(postText, imagePath);

                    return ResponseEntity.ok(new SocialPostResponse(
                        postId,
                        "PUBLISHED",
                        "https://www.linkedin.com/feed/update/" + postId,
                        "Post created successfully via direct token fallback"
                    ));
                } catch (Exception directError) {
                    logger.error("Direct token fallback also failed for user {}: {}", userId, directError.getMessage());
                }
            }

            return ResponseEntity.badRequest().body(new ErrorResponse(
                "LinkedIn connection not found or expired. Please reconnect your LinkedIn account.",
                400
            ));

        } catch (Exception e) {
            logger.error("Failed to post to LinkedIn for user {}: {}", userDetails.getUsername(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new ErrorResponse(
                "Failed to create LinkedIn post: " + e.getMessage(),
                500
            ));
        }
    }
    
    @GetMapping("/linkedin/status")
    public ResponseEntity<?> getLinkedInStatus(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
            
            boolean isConnected = linkedInPostingService.validateConnection(userId);
            
            return ResponseEntity.ok(new SocialConnectionStatus(
                isConnected,
                isConnected ? "LinkedIn connection is active and ready for posting" : "LinkedIn connection required"
            ));
            
        } catch (Exception e) {
            logger.error("Failed to check LinkedIn status for user {}: {}", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.internalServerError().body(new ErrorResponse(
                "Failed to check LinkedIn status", 
                500
            ));
        }
    }
}