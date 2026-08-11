-- Reset seed data on every startup so local/dev environments stay consistent
TRUNCATE TABLE assignments, consultants RESTART IDENTITY CASCADE;

INSERT INTO consultants (name, title_role, primary_skill_area, user_id, is_active) VALUES
('Gina Vance', 'Senior Associate', 'Technology', 'user_gina_vance', TRUE),
('David Sterling', 'Manager', 'Audit', 'user_david_sterling', TRUE),
('Aisha Patel', 'Senior Consultant', 'Tax', 'user_aisha_patel', TRUE),
('Liam O''Connor', 'Risk Analyst', 'Risk', 'user_liam_oconnor', TRUE),
('Chloe Zhao', 'Engagement Manager', 'Strategy', 'user_chloe_zhao', TRUE),
('Marcus Webb', 'Associate', 'Audit', 'user_marcus_webb', TRUE),
('Priya Nair', 'Senior Associate', 'Tax', 'user_priya_nair', TRUE),
('Ethan Brooks', 'Consultant', 'Risk', 'user_ethan_brooks', TRUE),
('Hannah Kim', 'Senior Manager', 'Strategy', 'user_hannah_kim', TRUE),
('Noah Fischer', 'Associate', 'Technology', 'user_noah_fischer', TRUE),
('Isabella Rossi', 'Senior Consultant', 'Audit', 'user_isabella_rossi', TRUE),
('Tyler Brennan', 'Consultant', 'Tax', 'user_tyler_brennan', TRUE),
('Maya Chandra', 'Risk Analyst', 'Risk', 'user_maya_chandra', TRUE),
('Jordan Lee', 'Manager', 'Technology', 'user_jordan_lee', TRUE),
('Olivia Bennett', 'Senior Associate', 'Strategy', 'user_olivia_bennett', TRUE),
('Ravi Kapoor', 'Associate', 'Audit', 'user_ravi_kapoor', TRUE),
('Zoe Palmer', 'Senior Consultant', 'Tax', 'user_zoe_palmer', TRUE),
('Diego Fuentes', 'Consultant', 'Risk', 'user_diego_fuentes', TRUE),
('Grace Thompson', 'Manager', 'Strategy', 'user_grace_thompson', TRUE),
('Ahmed Farouk', 'Associate', 'Technology', 'user_ahmed_farouk', TRUE);

-- Assignment start/end dates fall within their parent engagement's start_date/target_end_date range
-- (see engagement service's data.sql, engagements 1-32 in insertion order).
-- Status follows the parent engagement's lifecycle: In Progress/On Hold -> Active, Planned -> Pending,
-- Completed -> Completed, Cancelled -> Cancelled.
INSERT INTO assignments (consultant_id, engagement_id, engagement_role, assignment_start_date, assignment_end_date, status, is_active) VALUES
-- Engagement 1: Q3 Enterprise Risk Audit (In Progress)
(2, 1, 'Lead', '2026-06-01', '2026-09-30', 'Active', TRUE),
(1, 1, 'Associate', '2026-06-01', '2026-09-30', 'Active', TRUE),
(6, 1, 'Senior Associate', '2026-06-15', '2026-09-30', 'Active', TRUE),
-- Engagement 2: Tax Optimization & Compliance Review (In Progress)
(3, 2, 'Lead', '2026-07-15', '2026-11-15', 'Active', TRUE),
(7, 2, 'Senior Associate', '2026-08-01', '2026-11-15', 'Active', TRUE),
(20, 2, 'Associate', '2026-07-15', '2026-11-15', 'Active', TRUE),
-- Engagement 3: Supply Chain Cloud Migration Risk (In Progress)
(1, 3, 'Senior Associate', '2026-05-01', '2026-10-31', 'Active', TRUE),
(4, 3, 'Associate', '2026-05-15', '2026-10-31', 'Active', TRUE),
-- Engagement 4: Cybersecurity Risk Assessment (Planned)
(4, 4, 'Senior Associate', '2026-08-01', '2026-12-15', 'Pending', TRUE),
(8, 4, 'Associate', '2026-08-15', '2026-12-15', 'Pending', TRUE),
-- Engagement 5: ESG Financial Strategy Advisory (Planned)
(5, 5, 'Lead', '2026-09-01', '2027-02-28', 'Pending', TRUE),
(9, 5, 'Senior Associate', '2026-09-15', '2027-02-28', 'Pending', TRUE),
-- Engagement 6: Legacy Systems Decommissioning Audit (Completed)
(2, 6, 'Lead', '2025-01-10', '2025-06-30', 'Completed', TRUE),
(6, 6, 'Associate', '2025-01-10', '2025-06-30', 'Completed', TRUE),
-- Engagement 7: Vanguard Internal Controls Audit (Completed)
(11, 7, 'Lead', '2025-03-01', '2025-08-15', 'Completed', TRUE),
(16, 7, 'Associate', '2025-03-15', '2025-08-15', 'Completed', TRUE),
-- Engagement 8: BlackRock Tax Structuring Advisory (Completed)
(3, 8, 'Lead', '2026-02-01', '2026-07-31', 'Completed', TRUE),
(12, 8, 'Senior Associate', '2026-02-15', '2026-07-31', 'Completed', TRUE),
-- Engagement 9: Schwab M&A Due Diligence (Planned)
(5, 9, 'Lead', '2026-10-01', '2027-01-31', 'Pending', TRUE),
-- Engagement 10: PNC Regulatory Compliance Audit (Completed)
(2, 10, 'Lead', '2024-09-01', '2025-02-28', 'Completed', TRUE),
(16, 10, 'Associate', '2024-09-01', '2025-02-28', 'Completed', TRUE),
-- Engagement 11: Northwell Health Claims Audit (In Progress)
(11, 11, 'Lead', '2026-04-01', '2026-09-15', 'Active', TRUE),
(6, 11, 'Senior Associate', '2026-04-15', '2026-09-15', 'Active', TRUE),
-- Engagement 12: Northwell Revenue Cycle Advisory (Planned)
(9, 12, 'Lead', '2026-11-01', '2027-03-31', 'Pending', TRUE),
-- Engagement 13: CVS Health Risk Consulting (In Progress)
(4, 13, 'Lead', '2026-06-15', '2026-12-31', 'Active', TRUE),
(13, 13, 'Senior Associate', '2026-07-01', '2026-12-31', 'Active', TRUE),
(15, 13, 'Senior Associate', '2026-06-15', '2026-12-31', 'Active', TRUE),
-- Engagement 14: CVS Tax Advisory Engagement (Completed)
(7, 14, 'Lead', '2025-05-01', '2025-10-31', 'Completed', TRUE),
-- Engagement 15: Delta Fleet Financial Audit (In Progress)
(2, 15, 'Lead', '2026-07-01', '2026-11-30', 'Active', TRUE),
(17, 15, 'Associate', '2026-07-15', '2026-11-30', 'Active', TRUE),
-- Engagement 16: Delta Fuel Hedging Risk Advisory (Completed)
(8, 16, 'Lead', '2026-01-15', '2026-06-30', 'Completed', TRUE),
-- Engagement 17: Union Pacific Rail Safety Risk Review (Planned)
(13, 17, 'Senior Associate', '2026-09-15', '2027-01-15', 'Pending', TRUE),
-- Engagement 18: P&G Tax Compliance Review (In Progress)
(3, 18, 'Lead', '2026-03-01', '2026-08-31', 'Active', TRUE),
(12, 18, 'Associate', '2026-03-15', '2026-08-31', 'Active', TRUE),
-- Engagement 19: P&G Supply Chain Financial Advisory (Completed)
(5, 19, 'Lead', '2025-11-01', '2026-04-30', 'Completed', TRUE),
-- Engagement 20: Kraft Heinz Divestiture Audit (Completed)
(2, 20, 'Lead', '2024-06-01', '2024-12-31', 'Completed', TRUE),
-- Engagement 21: Duke Energy Regulatory Audit (In Progress)
(6, 21, 'Lead', '2026-05-15', '2026-10-15', 'Active', TRUE),
(16, 21, 'Senior Associate', '2026-06-01', '2026-10-15', 'Active', TRUE),
-- Engagement 22: Duke Energy Risk Consulting (Planned)
(4, 22, 'Lead', '2026-12-01', '2027-04-30', 'Pending', TRUE),
-- Engagement 23: NextEra Energy Tax Advisory (Planned)
(7, 23, 'Lead', '2026-09-01', '2027-01-31', 'Pending', TRUE),
-- Engagement 24: Salesforce Internal Audit (Completed)
(14, 24, 'Lead', '2026-02-15', '2026-07-15', 'Completed', TRUE),
(10, 24, 'Associate', '2026-02-15', '2026-07-15', 'Completed', TRUE),
-- Engagement 25: Salesforce Risk Consulting Engagement (In Progress)
(14, 25, 'Lead', '2026-07-20', '2026-12-20', 'Active', TRUE),
(18, 25, 'Senior Associate', '2026-08-01', '2026-12-20', 'Active', TRUE),
-- Engagement 26: Adobe Financial Advisory Engagement (Planned)
(19, 26, 'Lead', '2026-10-15', '2027-03-15', 'Pending', TRUE),
-- Engagement 27: Adobe Tax Structuring Review (Completed)
(17, 27, 'Lead', '2025-08-01', '2026-01-31', 'Completed', TRUE),
-- Engagement 28: Fidelity Digital Assets Risk Review (Completed)
(4, 28, 'Lead', '2026-01-01', '2026-05-31', 'Completed', TRUE),
(8, 28, 'Associate', '2026-01-01', '2026-05-31', 'Completed', TRUE),
-- Engagement 29: Vanguard ESG Reporting Advisory (In Progress)
(5, 29, 'Lead', '2026-08-15', '2027-01-15', 'Active', TRUE),
(9, 29, 'Senior Associate', '2026-09-01', '2027-01-15', 'Active', TRUE),
-- Engagement 30: BlackRock Cybersecurity Audit (Planned)
(2, 30, 'Lead', '2026-11-01', '2027-04-30', 'Pending', TRUE),
-- Engagement 31: Schwab Cloud Migration Risk Review (Cancelled)
(13, 31, 'Lead', '2026-04-01', '2026-09-30', 'Cancelled', TRUE),
-- Engagement 32: PNC Compliance Review (On Hold)
(3, 32, 'Lead', '2026-06-01', '2026-11-30', 'Active', TRUE);
