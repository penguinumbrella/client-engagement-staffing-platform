-- Reset seed data on every startup so local/dev environments stay consistent
TRUNCATE TABLE clients RESTART IDENTITY CASCADE;

-- IDs here are relied on by engagement/data.sql's client_id values (logical
-- cross-service reference, no SQL FK) and must stay in this insertion order.
INSERT INTO clients (company_name, industry, primary_contact_name, primary_contact_email, relationship_status, is_active) VALUES
('Acme Corp', 'Manufacturing', 'Jane Doe', 'jane.doe@acme.com', 'ACTIVE', TRUE),
('Globex Inc', 'Technology', 'John Smith', 'john.smith@globex.com', 'ACTIVE', TRUE),
('Initech', 'Finance', 'Michael Bolton', 'michael.bolton@initech.com', 'ACTIVE', TRUE),
('Meridian Capital Partners', 'Financial Services', 'Sarah Whitfield', 'sarah.whitfield@meridiancapital.com', 'ACTIVE', TRUE),
('Harborview Logistics', 'Logistics', 'Robert Chen', 'robert.chen@harborviewlogistics.com', 'FORMER', TRUE),
('Solstice Health Partners', 'Healthcare', 'Elena Marsh', 'elena.marsh@solsticehealth.com', 'PROSPECTIVE', TRUE);
