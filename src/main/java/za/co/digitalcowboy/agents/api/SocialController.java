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
import za.co.digitalcowboy.agents.repository.UserRepository;
import za.co.digitalcowboy.agents.service.social.LinkedInPostingService;
import za.co.digitalcowboy.agents.service.social.DirectLinkedInPostingService;

@RestController
@RequestMapping("/api/v1/social")
public class SocialController {
    
    private static final Logger logger = LoggerFactory.getLogger(SocialController.class);
    
    private final LinkedInPostingService linkedInPostingService;
    private final DirectLinkedInPostingService directLinkedInPostingService;
    private final UserRepository userRepository;
    
    public SocialController(LinkedInPostingService linkedInPostingService, 
                           DirectLinkedInPostingService directLinkedInPostingService,
                           UserRepository userRepository) {
        this.linkedInPostingService = linkedInPostingService;
        this.directLinkedInPostingService = directLinkedInPostingService;
        this.userRepository = userRepository;
    }
    
    @PostMapping("/linkedin/post")
    public ResponseEntity<?> postToLinkedIn(
            @Valid @RequestBody SocialPostRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        logger.info("LinkedIn post request from user: {}", userDetails.getUsername());
        
        try {
            // Get user ID from authenticated user
            Long userId = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
            
            // Try OAuth approach first
            try {
                if (linkedInPostingService.validateConnection(userId)) {
                    LinkedInPostResponse response = linkedInPostingService.postToLinkedIn(
                        userId, 
                        request.getText(), 
                        request.getImagePath()
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
                    String postId = directLinkedInPostingService.postToLinkedIn(request.getText(), request.getImagePath());
                    
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