-- Create bug_label_mapping table for many-to-many relationship
CREATE TABLE bug_label_mapping (
    bug_id BIGINT NOT NULL,
    label_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key
    PRIMARY KEY (bug_id, label_id),
    
    -- Foreign key constraints
    CONSTRAINT fk_bug_label_mapping_bug FOREIGN KEY (bug_id) REFERENCES bugs(id) ON DELETE CASCADE,
    CONSTRAINT fk_bug_label_mapping_label FOREIGN KEY (label_id) REFERENCES bug_labels(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_bug_label_mapping_bug_id ON bug_label_mapping(bug_id);
CREATE INDEX idx_bug_label_mapping_label_id ON bug_label_mapping(label_id); 