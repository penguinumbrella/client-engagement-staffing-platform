CREATE TABLE IF NOT EXISTS consultants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    title_role VARCHAR(100) NOT NULL,
    primary_skill_area VARCHAR(100) NOT NULL, -- Audit, Tax, Risk, Technology, Strategy
    user_id VARCHAR(255) UNIQUE, -- Links to the Spring Security user login account
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS assignments (
    id BIGSERIAL PRIMARY KEY,
    consultant_id BIGINT NOT NULL REFERENCES consultants(id) ON DELETE CASCADE,
    engagement_id BIGINT NOT NULL, -- Logical reference to Engagement Service (no SQL FK)
    engagement_role VARCHAR(50) NOT NULL, -- Lead, Senior Associate, Associate
    assignment_start_date DATE NOT NULL,
    assignment_end_date DATE NOT NULL, -- Must fall within the parent engagement's date range
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- Active, Pending, Completed, Cancelled
    status_overridden BOOLEAN NOT NULL DEFAULT FALSE, -- TRUE once an EM manually sets status; blocks engagement-status cascades from touching it
    is_active BOOLEAN NOT NULL DEFAULT TRUE, -- Allows soft-deleting/unstaffing individual assignments
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Constraint to prevent assigning the same consultant to the same engagement twice concurrently
    CONSTRAINT uc_consultant_engagement UNIQUE (consultant_id, engagement_id)
);

-- Adds the end date / status columns for tables created before this change
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS assignment_end_date DATE;
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS status_overridden BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE assignments ALTER COLUMN status_overridden SET DEFAULT FALSE;

-- Backfill any pre-existing open-ended assignments before enforcing NOT NULL below
UPDATE assignments SET assignment_end_date = assignment_start_date WHERE assignment_end_date IS NULL;
ALTER TABLE assignments ALTER COLUMN assignment_end_date SET NOT NULL;

-- Indexes for fast filtering by consultant or engagement assignments
CREATE INDEX IF NOT EXISTS idx_assignments_consultant_id ON assignments(consultant_id);
CREATE INDEX IF NOT EXISTS idx_assignments_engagement_id ON assignments(engagement_id);
