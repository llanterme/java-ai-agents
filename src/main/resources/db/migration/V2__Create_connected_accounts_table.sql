CREATE TABLE connected_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider ENUM('linkedin', 'facebook', 'twitter', 'instagram') NOT NULL,
    provider_user_id VARCHAR(255),
    provider_username VARCHAR(255),
    access_token TEXT NOT NULL,         -- Encrypted
    refresh_token TEXT,                 -- Encrypted
    token_expires_at TIMESTAMP,
    scopes TEXT,                        -- JSON array of granted scopes
    status ENUM('ACTIVE', 'EXPIRED', 'REVOKED') DEFAULT 'ACTIVE',
    connected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_provider (user_id, provider),
    INDEX idx_user_id (user_id),
    INDEX idx_provider (provider),
    INDEX idx_status (status),
    INDEX idx_token_expiry (token_expires_at)
);