package za.co.digitalcowboy.agents.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.digitalcowboy.agents.domain.GeneratedContent;
import za.co.digitalcowboy.agents.domain.OrchestrationResult;
import za.co.digitalcowboy.agents.domain.TopicRequest;
import za.co.digitalcowboy.agents.domain.User;
import za.co.digitalcowboy.agents.domain.dto.GeneratedContentResponse;
import za.co.digitalcowboy.agents.repository.GeneratedContentRepository;
import za.co.digitalcowboy.agents.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GeneratedContentService {

    private static final Logger log = LoggerFactory.getLogger(GeneratedContentService.class);

    private final GeneratedContentRepository contentRepository;
    private final UserRepository userRepository;

    public GeneratedContentService(GeneratedContentRepository contentRepository,
                                  UserRepository userRepository) {
        this.contentRepository = contentRepository;
        this.userRepository = userRepository;
    }

    public GeneratedContent saveGeneratedContent(String userEmail, TopicRequest request, OrchestrationResult result) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        GeneratedContent content = GeneratedContent.fromOrchestrationResult(user, request, result);
        GeneratedContent saved = contentRepository.save(content);

        log.info("Saved generated content with ID: {} for user: {}", saved.getId(), userEmail);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<GeneratedContentResponse> getAllContentForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        List<GeneratedContent> contents = contentRepository.findByUserOrderByCreatedAtDesc(user);

        log.info("Retrieved {} content items for user: {}", contents.size(), userEmail);

        return contents.stream()
            .map(GeneratedContentResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GeneratedContentResponse getContentById(Long id, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        GeneratedContent content = contentRepository.findByIdAndUser(id, user)
            .orElseThrow(() -> new IllegalArgumentException("Content not found with ID: " + id));

        log.info("Retrieved content with ID: {} for user: {}", id, userEmail);

        return GeneratedContentResponse.from(content);
    }

    public void deleteContent(Long id, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        if (!contentRepository.existsByIdAndUser(id, user)) {
            throw new IllegalArgumentException("Content not found with ID: " + id);
        }

        contentRepository.deleteByIdAndUser(id, user);

        log.info("Deleted content with ID: {} for user: {}", id, userEmail);
    }
}