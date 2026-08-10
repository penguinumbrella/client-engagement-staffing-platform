-- Reset seed data on every startup so local/dev environments stay consistent
TRUNCATE TABLE engagements RESTART IDENTITY CASCADE;

INSERT INTO engagements (engagement_name, client_id, engagement_type, summary, start_date, target_end_date, status, is_active) VALUES
('Q3 Enterprise Risk Audit', 1, 'Audit', 'Comprehensive review of enterprise risk controls and financial reporting processes ahead of year-end close.', '2026-06-01', '2026-09-30', 'In Progress', TRUE),
('Tax Optimization & Compliance Review', 1, 'Tax Advisory', 'Assessment of tax positions and filing processes to identify optimization opportunities and ensure regulatory compliance.', '2026-07-15', '2026-11-15', 'In Progress', TRUE),
('Supply Chain Cloud Migration', 2, 'Risk Consulting', 'Risk-focused guidance for migrating supply chain systems to a cloud-based infrastructure.', '2026-05-01', '2026-10-31', 'In Progress', TRUE),
('Cybersecurity Risk Assessment', 3, 'Risk Consulting', 'Evaluation of network, application, and data security controls to identify vulnerabilities and recommend remediations.', '2026-08-01', '2026-12-15', 'Planned', TRUE),
('ESG Financial Strategy Advisory', 4, 'Financial Advisory', 'Advisory engagement to align financial strategy with environmental, social, and governance reporting goals.', '2026-09-01', '2027-02-28', 'Planned', TRUE),
('Legacy Systems Decommissioning Audit', 5, 'Audit', 'Audit of decommissioning procedures for legacy systems to confirm data retention and compliance requirements were met.', '2025-01-10', '2025-06-30', 'Completed', TRUE);
