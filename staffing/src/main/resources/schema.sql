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
    is_active BOOLEAN NOT NULL DEFAULT TRUE, -- Allows soft-deleting/unstaffing individual assignments
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Constraint to prevent assigning the same consultant to the same engagement twice concurrently
    CONSTRAINT uc_consultant_engagement UNIQUE (consultant_id, engagement_id)
);

-- Indexes for fast filtering by consultant or engagement assignments
CREATE INDEX IF NOT EXISTS idx_assignments_consultant_id ON assignments(consultant_id);
CREATE INDEX IF NOT EXISTS idx_assignments_engagement_id ON assignments(engagement_id);
