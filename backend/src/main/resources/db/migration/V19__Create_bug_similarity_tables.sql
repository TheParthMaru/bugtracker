-- V19__Create_bug_similarity_tables.sql
-- Create tables for duplicate bug classification feature
-- Implements similarity cache and duplicate relationships as per specification

-- Ensure UUID extension is available
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create bug similarity cache table for performance optimization
CREATE TABLE bug_similarity_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bug_id BIGINT NOT NULL,
    similar_bug_id BIGINT NOT NULL,
    similarity_score NUMERIC(5,4) NOT NULL CHECK (similarity_score >= 0.0 AND similarity_score <= 1.0),
    algorithm_used VARCHAR(50) NOT NULL DEFAULT 'COSINE',
    text_fingerprint VARCHAR(64) NOT NULL, -- SHA-256 hash of combined title+description
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '7 days'),
    
    -- Constraints
    CONSTRAINT uk_bug_similarity_cache_bugs UNIQUE(bug_id, similar_bug_id),
    CONSTRAINT chk_bug_similarity_cache_different_bugs CHECK (bug_id != similar_bug_id),
    
    -- Foreign key constraints
    CONSTRAINT fk_bug_similarity_cache_bug FOREIGN KEY (bug_id) REFERENCES bugs(id) ON DELETE CASCADE,
    CONSTRAINT fk_bug_similarity_cache_similar_bug FOREIGN KEY (similar_bug_id) REFERENCES bugs(id) ON DELETE CASCADE
);

-- Create bug duplicates tracking table
CREATE TABLE bug_duplicates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_bug_id BIGINT NOT NULL,
    duplicate_bug_id BIGINT NOT NULL,
    marked_by_user_id UUID NOT NULL,
    confidence_score NUMERIC(5,4) NOT NULL CHECK (confidence_score >= 0.0 AND confidence_score <= 1.0),
    detection_method VARCHAR(50) NOT NULL DEFAULT 'MANUAL' CHECK (detection_method IN ('MANUAL', 'AUTOMATIC', 'HYBRID')),
    marked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    additional_context TEXT,
    
    -- Constraints
    CONSTRAINT uk_bug_duplicates_bugs UNIQUE(original_bug_id, duplicate_bug_id),
    CONSTRAINT chk_bug_duplicates_different_bugs CHECK (original_bug_id != duplicate_bug_id),
    
    -- Foreign key constraints
    CONSTRAINT fk_bug_duplicates_original FOREIGN KEY (original_bug_id) REFERENCES bugs(id) ON DELETE CASCADE,
    CONSTRAINT fk_bug_duplicates_duplicate FOREIGN KEY (duplicate_bug_id) REFERENCES bugs(id) ON DELETE CASCADE,
    CONSTRAINT fk_bug_duplicates_marked_by FOREIGN KEY (marked_by_user_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Create similarity algorithm configuration table
CREATE TABLE similarity_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    algorithm_name VARCHAR(50) NOT NULL CHECK (algorithm_name IN ('COSINE', 'JACCARD', 'LEVENSHTEIN')),
    weight NUMERIC(3,2) NOT NULL DEFAULT 1.0 CHECK (weight >= 0.0 AND weight <= 1.0),
    threshold NUMERIC(3,2) NOT NULL DEFAULT 0.75 CHECK (threshold >= 0.0 AND threshold <= 1.0),
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT uk_similarity_config_project_algorithm UNIQUE(project_id, algorithm_name),
    
    -- Foreign key constraints
    CONSTRAINT fk_similarity_config_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Performance indexes for bug_similarity_cache
CREATE INDEX idx_bug_similarity_cache_bug_id ON bug_similarity_cache(bug_id);
CREATE INDEX idx_bug_similarity_cache_score ON bug_similarity_cache(similarity_score DESC);
CREATE INDEX idx_bug_similarity_cache_expires ON bug_similarity_cache(expires_at);
CREATE INDEX idx_bug_similarity_cache_fingerprint ON bug_similarity_cache(text_fingerprint);
CREATE INDEX idx_bug_similarity_cache_bug_score ON bug_similarity_cache(bug_id, similarity_score DESC);
-- Removed problematic partial index: CREATE INDEX idx_bug_similarity_cache_expired ON bug_similarity_cache(expires_at) WHERE expires_at < CURRENT_TIMESTAMP;

-- Performance indexes for bug_duplicates
CREATE INDEX idx_bug_duplicates_original ON bug_duplicates(original_bug_id);
CREATE INDEX idx_bug_duplicates_duplicate ON bug_duplicates(duplicate_bug_id);
CREATE INDEX idx_bug_duplicates_marked_by ON bug_duplicates(marked_by_user_id);
CREATE INDEX idx_bug_duplicates_method ON bug_duplicates(detection_method);
CREATE INDEX idx_bug_duplicates_marked_at ON bug_duplicates(marked_at);
CREATE INDEX idx_bug_duplicates_original_method ON bug_duplicates(original_bug_id, detection_method);

-- Performance indexes for similarity_config
CREATE INDEX idx_similarity_config_project_id ON similarity_config(project_id);
CREATE INDEX idx_similarity_config_enabled ON similarity_config(is_enabled) WHERE is_enabled = true;

-- Create trigger to automatically update updated_at timestamp
CREATE TRIGGER update_similarity_config_updated_at
    BEFORE UPDATE ON similarity_config
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert default configurations for all existing projects
-- INSERT INTO similarity_config (project_id, algorithm_name, weight, threshold, is_enabled)
-- SELECT id, 'COSINE', 0.6, 0.75, true FROM projects WHERE is_active = true;

-- INSERT INTO similarity_config (project_id, algorithm_name, weight, threshold, is_enabled)
-- SELECT id, 'JACCARD', 0.3, 0.5, true FROM projects WHERE is_active = true;

-- INSERT INTO similarity_config (project_id, algorithm_name, weight, threshold, is_enabled)
-- SELECT id, 'LEVENSHTEIN', 0.1, 0.8, true FROM projects WHERE is_active = true;