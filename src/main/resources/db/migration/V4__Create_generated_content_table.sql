CREATE TABLE generated_content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    topic VARCHAR(200) NOT NULL,
    platform VARCHAR(50) NOT NULL,
    tone VARCHAR(50) NOT NULL,
    image_count INT DEFAULT 1,

    -- Research data
    research_points JSON,

    -- Content data
    content_headline VARCHAR(500),
    content_body TEXT,
    content_cta VARCHAR(500),
    content_hashtags JSON,

    -- Image data
    image_prompt TEXT,
    image_openai_urls JSON,
    image_local_paths JSON,
    image_local_urls JSON,

    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Foreign key constraint
    CONSTRAINT fk_generated_content_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,

    -- Indexes
    INDEX idx_generated_content_user_id (user_id),
    INDEX idx_generated_content_created_at (created_at),
    INDEX idx_generated_content_platform (platform)
);