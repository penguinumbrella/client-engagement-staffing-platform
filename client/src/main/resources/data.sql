-- Reset seed data on every startup so local/dev environments stay consistent with engagement seed data,
-- which references these clients by id (1-15).
TRUNCATE TABLE clients RESTART IDENTITY CASCADE;

INSERT INTO clients (company_name, industry, primary_contact_name, primary_contact_email, relationship_status, is_active)
VALUES
    ('Fidelity', 'Financial Services', 'Jane Doe', 'jane.doe@fidelity.com', 'ACTIVE', TRUE),
    ('Vanguard', 'Financial Services', 'John Smith', 'john.smith@vanguard.com', 'ACTIVE', TRUE),
    ('BlackRock', 'Financial Services', 'Michael Bolton', 'michael.bolton@blackrock.com', 'ACTIVE', TRUE),
    ('Charles Schwab', 'Financial Services', 'Emily Chen', 'emily.chen@schwab.com', 'PROSPECTIVE', TRUE),
    ('PNC', 'Financial Services', 'Robert Lee', 'robert.lee@pnc.com', 'FORMER', TRUE),
    ('Northwell Health', 'Healthcare', 'Sarah Johnson', 'sarah.johnson@northwell.edu', 'ACTIVE', TRUE),
    ('CVS Health', 'Healthcare', 'Marcus Reed', 'marcus.reed@cvshealth.com', 'ACTIVE', TRUE),
    ('Delta Air Lines', 'Transportation', 'Denise Coleman', 'denise.coleman@delta.com', 'ACTIVE', TRUE),
    ('Union Pacific', 'Transportation', 'Kevin Brooks', 'kevin.brooks@up.com', 'PROSPECTIVE', TRUE),
    ('Procter & Gamble', 'Consumer Goods', 'Laura Simmons', 'laura.simmons@pg.com', 'ACTIVE', TRUE),
    ('Kraft Heinz', 'Consumer Goods', 'Anthony Diaz', 'anthony.diaz@kraftheinz.com', 'FORMER', TRUE),
    ('Duke Energy', 'Energy', 'Rebecca Hunt', 'rebecca.hunt@duke-energy.com', 'ACTIVE', TRUE),
    ('NextEra Energy', 'Energy', 'Patrick Ng', 'patrick.ng@nexteraenergy.com', 'PROSPECTIVE', TRUE),
    ('Salesforce', 'Technology', 'Christina Bell', 'christina.bell@salesforce.com', 'ACTIVE', TRUE),
    ('Adobe', 'Technology', 'Wei Chen', 'wei.chen@adobe.com', 'ACTIVE', TRUE);
