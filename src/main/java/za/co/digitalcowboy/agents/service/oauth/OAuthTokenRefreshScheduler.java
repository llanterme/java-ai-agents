package za.co.digitalcowboy.agents.service.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.digitalcowboy.agents.domain.oauth.ConnectedAccount;
import za.co.digitalcowboy.agents.domain.oauth.ConnectionStatus;
import za.co.digitalcowboy.agents.repository.ConnectedAccountRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service to proactively refresh OAuth tokens before they expire.
 * This ensures uninterrupted service by refreshing tokens in the background.
 */
@Service
@ConditionalOnProperty(
    value = "oauth.token-refresh.scheduled.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class OAuthTokenRefreshScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OAuthTokenRefreshScheduler.class);

    private final OAuthConnectionService oAuthConnectionService;
    private final ConnectedAccountRepository connectedAccountRepository;

    @Value("${oauth.token-refresh.days-before-expiry:7}")
    private int daysBeforeExpiry;

    @Value("${oauth.token-refresh.scheduled.enabled:true}")
    private boolean scheduledRefreshEnabled;

    public OAuthTokenRefreshScheduler(
            OAuthConnectionService oAuthConnectionService,
            ConnectedAccountRepository connectedAccountRepository) {
        this.oAuthConnectionService = oAuthConnectionService;
        this.connectedAccountRepository = connectedAccountRepository;
    }

    /**
     * Scheduled task to refresh tokens that are expiring soon.
     * Runs daily at 2 AM by default (configurable via cron expression).
     */
    @Scheduled(cron = "${oauth.token-refresh.scheduled.cron:0 0 2 * * *}")
    @Transactional
    public void refreshExpiringTokens() {
        if (!scheduledRefreshEnabled) {
            logger.debug("Scheduled token refresh is disabled");
            return;
        }

        logger.info("Starting scheduled OAuth token refresh check");

        try {
            // Find all active connections with tokens expiring within N days
            LocalDateTime expiryThreshold = LocalDateTime.now().plusDays(daysBeforeExpiry);
            List<ConnectedAccount> expiringAccounts = connectedAccountRepository
                .findByStatusAndTokenExpiresAtBefore(ConnectionStatus.ACTIVE, expiryThreshold);

            if (expiringAccounts.isEmpty()) {
                logger.info("No tokens found expiring within {} days", daysBeforeExpiry);
                return;
            }

            logger.info("Found {} tokens expiring within {} days", expiringAccounts.size(), daysBeforeExpiry);

            int successCount = 0;
            int failureCount = 0;

            for (ConnectedAccount account : expiringAccounts) {
                try {
                    Long userId = account.getUser().getId();
                    String providerName = account.getProvider().getValue();

                    logger.info("Attempting to refresh {} token for user {} (expires: {})",
                        providerName, userId, account.getTokenExpiresAt());

                    boolean refreshed = oAuthConnectionService.refreshTokenIfNeeded(
                        userId, providerName, daysBeforeExpiry);

                    if (refreshed) {
                        successCount++;
                        logger.info("Successfully refreshed {} token for user {}",
                            providerName, userId);
                    } else {
                        failureCount++;
                        logger.warn("Failed to refresh {} token for user {}",
                            providerName, userId);
                    }

                } catch (Exception e) {
                    failureCount++;
                    logger.error("Error refreshing token for account {}: {}",
                        account.getId(), e.getMessage(), e);
                }
            }

            logger.info("Scheduled token refresh completed. Success: {}, Failed: {}",
                successCount, failureCount);

            // Optional: Send notification if failures exceed threshold
            if (failureCount > 0) {
                logger.warn("Token refresh failures detected: {} accounts failed to refresh", failureCount);
                // TODO: Implement notification service to alert administrators
            }

        } catch (Exception e) {
            logger.error("Error during scheduled token refresh", e);
        }
    }

    /**
     * Manually trigger token refresh for all expiring tokens.
     * Can be called via an admin endpoint for immediate refresh.
     */
    public RefreshSummary refreshAllExpiringTokens() {
        logger.info("Manual token refresh triggered");

        LocalDateTime expiryThreshold = LocalDateTime.now().plusDays(daysBeforeExpiry);
        List<ConnectedAccount> expiringAccounts = connectedAccountRepository
            .findByStatusAndTokenExpiresAtBefore(ConnectionStatus.ACTIVE, expiryThreshold);

        int successCount = 0;
        int failureCount = 0;

        for (ConnectedAccount account : expiringAccounts) {
            try {
                Long userId = account.getUser().getId();
                String providerName = account.getProvider().getValue();

                boolean refreshed = oAuthConnectionService.refreshTokenIfNeeded(
                    userId, providerName, daysBeforeExpiry);

                if (refreshed) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                failureCount++;
                logger.error("Error refreshing token for account {}", account.getId(), e);
            }
        }

        return new RefreshSummary(successCount, failureCount, expiringAccounts.size());
    }

    /**
     * Summary of token refresh operation.
     */
    public static class RefreshSummary {
        private final int successCount;
        private final int failureCount;
        private final int totalCount;

        public RefreshSummary(int successCount, int failureCount, int totalCount) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.totalCount = totalCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public int getTotalCount() {
            return totalCount;
        }

        @Override
        public String toString() {
            return String.format("RefreshSummary{success=%d, failed=%d, total=%d}",
                successCount, failureCount, totalCount);
        }
    }
}