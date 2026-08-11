CREATE TABLE IF NOT EXISTS engagements (
    id BIGSERIAL PRIMARY KEY,
    engagement_name VARCHAR(255) NOT NULL,
    client_id BIGINT NOT NULL, -- Logical reference to Client Service (no SQL FK)
    engagement_type VARCHAR(100) NOT NULL, -- Audit, Tax Advisory, Risk Consulting, Financial Advisory
    summary TEXT,
    start_date DATE NOT NULL,
    target_end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'Planned', -- Planned, In Progress, On Hold, Completed, Cancelled
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Retrofit the summary column onto databases created before it was added, since
-- CREATE TABLE IF NOT EXISTS above is a no-op against an already-existing table.
ALTER TABLE engagements ADD COLUMN IF NOT EXISTS summary TEXT;

-- Index for searching engagements by client or active status
CREATE INDEX IF NOT EXISTS idx_engagements_client_id ON engagements(client_id);
CREATE INDEX IF NOT EXISTS idx_engagements_is_active ON engagements(is_active);
