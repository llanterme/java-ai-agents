package za.co.digitalcowboy.agents.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.digitalcowboy.agents.domain.dto.GeneratedContentResponse;
import za.co.digitalcowboy.agents.service.GeneratedContentService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/content")
public class ContentController {

    private static final Logger log = LoggerFactory.getLogger(ContentController.class);

    private final GeneratedContentService generatedContentService;

    public ContentController(GeneratedContentService generatedContentService) {
        this.generatedContentService = generatedContentService;
    }

    @GetMapping
    public ResponseEntity<List<GeneratedContentResponse>> getAllContent(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Fetching all content for user: {}", userDetails.getUsername());

        List<GeneratedContentResponse> contents = generatedContentService.getAllContentForUser(
            userDetails.getUsername());

        log.info("Retrieved {} content items for user: {}", contents.size(), userDetails.getUsername());
        return ResponseEntity.ok(contents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GeneratedContentResponse> getContentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Fetching content with ID: {} for user: {}", id, userDetails.getUsername());

        try {
            GeneratedContentResponse content = generatedContentService.getContentById(
                id, userDetails.getUsername());
            return ResponseEntity.ok(content);
        } catch (IllegalArgumentException e) {
            log.warn("Content not found with ID: {} for user: {}", id, userDetails.getUsername());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Deleting content with ID: {} for user: {}", id, userDetails.getUsername());

        try {
            generatedContentService.deleteContent(id, userDetails.getUsername());
            log.info("Successfully deleted content with ID: {} for user: {}", id, userDetails.getUsername());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Content not found with ID: {} for user: {}", id, userDetails.getUsername());
            return ResponseEntity.notFound().build();
        }
    }
}