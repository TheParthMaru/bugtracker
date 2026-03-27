-- Create bug_labels table
CREATE TABLE bug_labels (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    color VARCHAR(7) NOT NULL DEFAULT '#3B82F6', -- Hex color code
    description TEXT,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_bug_labels_name ON bug_labels(name);
CREATE INDEX idx_bug_labels_is_system ON bug_labels(is_system);

-- Insert system labels
-- INSERT INTO bug_labels (name, color, description, is_system) VALUES
-- ('Frontend', '#3B82F6', 'Frontend related issues', TRUE),
-- ('Backend', '#EF4444', 'Backend related issues', TRUE),
-- ('Database', '#8B5CF6', 'Database related issues', TRUE),
-- ('API', '#F59E0B', 'API related issues', TRUE),
-- ('UI/UX', '#10B981', 'User interface and experience issues', TRUE),
-- ('Security', '#DC2626', 'Security related issues', TRUE),
-- ('Performance', '#7C3AED', 'Performance related issues', TRUE),
-- ('Testing', '#059669', 'Testing related issues', TRUE),
-- ('Documentation', '#6B7280', 'Documentation related issues', TRUE),
-- ('DevOps', '#1F2937', 'DevOps and deployment issues', TRUE),
-- ('Bug', '#DC2626', 'General bug issues', TRUE),
-- ('Feature', '#059669', 'Feature requests and enhancements', TRUE),
-- ('Enhancement', '#7C3AED', 'Enhancement requests', TRUE),
-- ('Task', '#F59E0B', 'General tasks', TRUE); 