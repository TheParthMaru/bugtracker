-- Create bugs table
CREATE TABLE bugs (
    id BIGSERIAL PRIMARY KEY,
    project_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('ISSUE', 'TASK', 'SPEC')),
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'ASSIGNED', 'FIXED', 'CLOSED', 'REOPENED')),
    priority VARCHAR(10) NOT NULL CHECK (priority IN ('CRASH', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    reporter_id UUID NOT NULL,
    assignee_id UUID,
    close_reason VARCHAR(15) CHECK (close_reason IN ('FIXED', 'INVALID', 'DUPLICATE', 'WONT_FIX', 'WORKS_FOR_ME')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL,
    
    -- Foreign key constraints
    CONSTRAINT fk_bugs_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_bugs_reporter FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_bugs_assignee FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for performance
CREATE INDEX idx_bugs_project_id ON bugs(project_id);
CREATE INDEX idx_bugs_status ON bugs(status);
CREATE INDEX idx_bugs_priority ON bugs(priority);
CREATE INDEX idx_bugs_assignee_id ON bugs(assignee_id);
CREATE INDEX idx_bugs_reporter_id ON bugs(reporter_id);
CREATE INDEX idx_bugs_created_at ON bugs(created_at);
CREATE INDEX idx_bugs_updated_at ON bugs(updated_at);

-- Create composite indexes for common queries
CREATE INDEX idx_bugs_project_status ON bugs(project_id, status);
CREATE INDEX idx_bugs_project_priority ON bugs(project_id, priority);
CREATE INDEX idx_bugs_project_assignee ON bugs(project_id, assignee_id);
CREATE INDEX idx_bugs_project_created ON bugs(project_id, created_at);

-- Create trigger to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_bugs_updated_at BEFORE UPDATE ON bugs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column(); 