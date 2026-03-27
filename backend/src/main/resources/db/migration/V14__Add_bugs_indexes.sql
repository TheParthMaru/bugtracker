-- Add additional performance indexes for bugs module

-- Full-text search index for bug title and description
CREATE INDEX idx_bugs_search ON bugs USING gin(to_tsvector('english', title || ' ' || description));

-- Index for status transitions and analytics
CREATE INDEX idx_bugs_status_priority ON bugs(status, priority);
CREATE INDEX idx_bugs_project_status_priority ON bugs(project_id, status, priority);

-- Index for date range queries
CREATE INDEX idx_bugs_created_range ON bugs(created_at, project_id);
CREATE INDEX idx_bugs_updated_range ON bugs(updated_at, project_id);

-- Index for close reason analytics
CREATE INDEX idx_bugs_close_reason ON bugs(close_reason, project_id);

-- Index for label analytics
CREATE INDEX idx_bug_labels_usage ON bug_labels(id, name);

-- Index for comment analytics
CREATE INDEX idx_bug_comments_analytics ON bug_comments(bug_id, author_id, created_at);

-- Index for attachment analytics
CREATE INDEX idx_bug_attachments_analytics ON bug_attachments(bug_id, uploaded_by, created_at); 