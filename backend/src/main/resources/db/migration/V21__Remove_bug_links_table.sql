-- V21__Remove_bug_links_table.sql
-- Remove the bug_links table and related functionality

-- Drop the bug_links table
DROP TABLE IF EXISTS bug_links CASCADE;

-- Note: This migration removes the bug linking feature entirely
-- All related indexes and constraints are automatically dropped with the table 