-- Reset seed data on every startup so local/dev environments stay consistent
TRUNCATE TABLE notifications RESTART IDENTITY CASCADE;

INSERT INTO notifications (recipient_id, title, message, type, source_service, source_id, is_read, is_active) VALUES
(1, 'Welcome', 'Your notification inbox is ready.', 'GENERAL', NULL, NULL, FALSE, TRUE),
(1, 'Engagement created', 'Engagement Q3 Enterprise Risk Audit was created.', 'ENGAGEMENT_CREATED', 'engagement', 1, FALSE, TRUE),
(2, 'Assignment created', 'You were staffed on engagement 1 as Lead.', 'ASSIGNMENT_CREATED', 'staffing', 1, FALSE, TRUE);
