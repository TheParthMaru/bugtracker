-- Create teams table
CREATE TABLE teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    slug VARCHAR(100) UNIQUE NOT NULL,
    is_public BOOLEAN DEFAULT true,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT teams_name_check CHECK (length(trim(name)) > 0),
    CONSTRAINT teams_slug_check CHECK (slug ~ '^[a-z0-9-]+$')
);

-- Create indexes for teams table
CREATE INDEX idx_teams_created_by ON teams(created_by);
CREATE INDEX idx_teams_slug ON teams(slug);
CREATE INDEX idx_teams_name ON teams(name); 