-- Reset seed data on every startup so local/dev environments stay consistent.
-- No rows seeded: recipient_id is now the auth-service user UUID, which isn't known here.
-- Real notifications arrive via Kafka events published by the engagement/staffing services.
TRUNCATE TABLE notifications RESTART IDENTITY CASCADE;
