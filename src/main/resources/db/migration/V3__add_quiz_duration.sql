-- V3: Add duration_minutes column to quiz_sets table
-- Allows users to customize exam duration (default 45 minutes)

ALTER TABLE quiz_sets 
ADD COLUMN IF NOT EXISTS duration_minutes INTEGER NOT NULL DEFAULT 45;

-- Add comment for documentation
COMMENT ON COLUMN quiz_sets.duration_minutes IS 'Exam duration in minutes (5-180, default 45)';
