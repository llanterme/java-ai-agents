-- Add research sources column to generated_content table
ALTER TABLE generated_content
ADD COLUMN research_sources JSON COMMENT 'Research sources and references';