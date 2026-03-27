-- Create project_members table for managing project membership
-- This is a junction table that handles the many-to-many relationship
-- between users and projects with additional metadata
CREATE TABLE project_members (
    -- Primary key for each membership record
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Foreign key to projects table
    -- CASCADE DELETE ensures when a project is deleted, all memberships are removed
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    
    -- Foreign key to users table
    -- When a user is deleted, we'll need to handle this in application logic
    user_id UUID NOT NULL REFERENCES users(id),
    
    -- Role of the user within the project
    -- ADMIN: Can manage project settings and members
    -- MEMBER: Can participate in project activities
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    
    -- Status of the membership for approval workflow
    -- PENDING: User requested to join, waiting for admin approval
    -- ACTIVE: User is approved and active member
    -- REJECTED: User request was rejected by admin
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    -- Timestamp when user actually joined (after approval)
    -- NULL for PENDING/REJECTED status
    joined_at TIMESTAMP,
    
    -- Timestamp when user first requested to join
    -- Always populated when membership record is created
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Reference to admin user who approved the membership
    -- NULL for PENDING/REJECTED status
    approved_by UUID REFERENCES users(id),
    
    -- Timestamp when membership was approved
    -- NULL for PENDING/REJECTED status
    approved_at TIMESTAMP,
    
    -- Standard audit timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints to ensure data integrity
    
    -- Prevent duplicate memberships for same user-project combination
    UNIQUE(project_id, user_id),
    
    -- Validate role values at database level
    CONSTRAINT project_members_role_check CHECK (role IN ('ADMIN', 'MEMBER')),
    
    -- Validate status values at database level
    CONSTRAINT project_members_status_check CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED')),
    
    -- Ensure approval data consistency for ACTIVE members
    -- If status is ACTIVE, then approved_by, approved_at, and joined_at must be set
    CONSTRAINT project_members_approved_consistency CHECK (
        (status = 'ACTIVE' AND approved_by IS NOT NULL AND approved_at IS NOT NULL AND joined_at IS NOT NULL)
        OR (status != 'ACTIVE')
    )
); 