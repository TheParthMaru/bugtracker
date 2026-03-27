-- Create bug_attachments table
CREATE TABLE bug_attachments (
    id BIGSERIAL PRIMARY KEY,
    bug_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    uploaded_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_bug_attachments_bug FOREIGN KEY (bug_id) REFERENCES bugs(id) ON DELETE CASCADE,
    CONSTRAINT fk_bug_attachments_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE RESTRICT
);

-- Create indexes for performance
CREATE INDEX idx_bug_attachments_bug_id ON bug_attachments(bug_id);
CREATE INDEX idx_bug_attachments_uploaded_by ON bug_attachments(uploaded_by);
CREATE INDEX idx_bug_attachments_created_at ON bug_attachments(created_at);

-- Create composite index for common queries
CREATE INDEX idx_bug_attachments_bug_created ON bug_attachments(bug_id, created_at); 