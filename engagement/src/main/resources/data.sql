-- Reset seed data on every startup so local/dev environments stay consistent
TRUNCATE TABLE engagements RESTART IDENTITY CASCADE;

INSERT INTO engagements (engagement_name, client_id, engagement_type, start_date, target_end_date, status, is_active) VALUES
('Q3 Enterprise Risk Audit', 1, 'Audit', '2026-06-01', '2026-09-30', 'In Progress', TRUE),
('Tax Optimization & Compliance Review', 1, 'Tax Advisory', '2026-07-15', '2026-11-15', 'In Progress', TRUE),
('Supply Chain Cloud Migration', 2, 'Risk Consulting', '2026-05-01', '2026-10-31', 'In Progress', TRUE),
('Cybersecurity Risk Assessment', 3, 'Risk Consulting', '2026-08-01', '2026-12-15', 'Planned', TRUE),
('ESG Financial Strategy Advisory', 4, 'Financial Advisory', '2026-09-01', '2027-02-28', 'Planned', TRUE),
('Legacy Systems Decommissioning Audit', 5, 'Audit', '2025-01-10', '2025-06-30', 'Completed', TRUE);
