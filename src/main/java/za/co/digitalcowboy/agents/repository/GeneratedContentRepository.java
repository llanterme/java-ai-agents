package za.co.digitalcowboy.agents.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.digitalcowboy.agents.domain.GeneratedContent;
import za.co.digitalcowboy.agents.domain.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface GeneratedContentRepository extends JpaRepository<GeneratedContent, Long> {

    List<GeneratedContent> findByUserOrderByCreatedAtDesc(User user);

    Optional<GeneratedContent> findByIdAndUser(Long id, User user);

    void deleteByIdAndUser(Long id, User user);

    boolean existsByIdAndUser(Long id, User user);
}