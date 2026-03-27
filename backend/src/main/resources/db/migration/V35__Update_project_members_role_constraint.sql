-- Migration: Update project_members_role_check constraint to allow PENDING role
-- This migration updates the existing constraint to support the new PENDING role
-- that was added to the ProjectRole enum for handling pending join requests

-- Drop the existing constraint that only allows ADMIN and MEMBER roles
ALTER TABLE project_members DROP CONSTRAINT IF EXISTS project_members_role_check;

-- Add the new constraint that allows ADMIN, MEMBER, and PENDING roles
-- PENDING role is used for users who have requested to join but haven't been approved yet
ALTER TABLE project_members ADD CONSTRAINT project_members_role_check 
CHECK (role IN ('ADMIN', 'MEMBER', 'PENDING'));
