CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(100) NOT NULL DEFAULT 'GENERAL',
    source_service VARCHAR(100),
    source_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
@@@@

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_id ON notifications(recipient_id);
@@@@
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(is_read);
@@@@
CREATE INDEX IF NOT EXISTS idx_notifications_is_active ON notifications(is_active);
@@@@

-- Retrofit recipient_id from BIGINT (an old staffing/client id scheme) to UUID (the auth-service
-- user id) onto databases created before this change. Existing rows reference ids from the old
-- scheme with no valid mapping to a user UUID, so they're cleared rather than converted. Guarded
-- so it only runs once per database (skipped once the column is already UUID).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'notifications' AND column_name = 'recipient_id' AND udt_name = 'int8'
    ) THEN
        DELETE FROM notifications;
        ALTER TABLE notifications ALTER COLUMN recipient_id TYPE UUID USING NULL;
    END IF;
END $$;
@@@@
