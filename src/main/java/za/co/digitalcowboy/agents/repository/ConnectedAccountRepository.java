package za.co.digitalcowboy.agents.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.digitalcowboy.agents.domain.oauth.ConnectedAccount;
import za.co.digitalcowboy.agents.domain.oauth.OAuthProvider;
import za.co.digitalcowboy.agents.domain.oauth.ConnectionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectedAccountRepository extends JpaRepository<ConnectedAccount, Long> {
    
    Optional<ConnectedAccount> findByUserIdAndProvider(Long userId, OAuthProvider provider);
    
    List<ConnectedAccount> findByUserId(Long userId);
    
    List<ConnectedAccount> findByUserIdAndStatus(Long userId, ConnectionStatus status);
    
    @Query("SELECT ca FROM ConnectedAccount ca WHERE ca.tokenExpiresAt IS NOT NULL AND ca.tokenExpiresAt < :now")
    List<ConnectedAccount> findExpiredTokens(@Param("now") LocalDateTime now);
    
    @Query("SELECT ca FROM ConnectedAccount ca WHERE ca.tokenExpiresAt IS NOT NULL AND ca.tokenExpiresAt < :threshold")
    List<ConnectedAccount> findTokensExpiringBefore(@Param("threshold") LocalDateTime threshold);

    List<ConnectedAccount> findByStatusAndTokenExpiresAtBefore(ConnectionStatus status, LocalDateTime threshold);

    void deleteByUserIdAndProvider(Long userId, OAuthProvider provider);
    
    boolean existsByUserIdAndProvider(Long userId, OAuthProvider provider);
    
    long countByProvider(OAuthProvider provider);
}