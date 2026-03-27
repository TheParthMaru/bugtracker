-- Create bug_comments table
CREATE TABLE bug_comments (
    id BIGSERIAL PRIMARY KEY,
    bug_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    content TEXT NOT NULL,
    author_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_bug_comments_bug FOREIGN KEY (bug_id) REFERENCES bugs(id) ON DELETE CASCADE,
    CONSTRAINT fk_bug_comments_parent FOREIGN KEY (parent_id) REFERENCES bug_comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_bug_comments_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Create indexes for performance
CREATE INDEX idx_bug_comments_bug_id ON bug_comments(bug_id);
CREATE INDEX idx_bug_comments_parent_id ON bug_comments(parent_id);
CREATE INDEX idx_bug_comments_author_id ON bug_comments(author_id);
CREATE INDEX idx_bug_comments_created_at ON bug_comments(created_at);
CREATE INDEX idx_bug_comments_updated_at ON bug_comments(updated_at);

-- Create composite indexes for common queries
CREATE INDEX idx_bug_comments_bug_created ON bug_comments(bug_id, created_at);
CREATE INDEX idx_bug_comments_bug_parent ON bug_comments(bug_id, parent_id);

-- Create trigger to automatically update updated_at timestamp
CREATE TRIGGER update_bug_comments_updated_at BEFORE UPDATE ON bug_comments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column(); 