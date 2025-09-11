-- Change provider column from ENUM to VARCHAR to support JPA converter
-- This allows the custom OAuthProviderConverter to handle enum values properly

ALTER TABLE connected_accounts 
MODIFY COLUMN provider VARCHAR(255) NOT NULL;