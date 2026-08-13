INSERT INTO clients (company_name, industry, primary_contact_name, primary_contact_email, relationship_status, is_active)
VALUES
    ('Acme Corp', 'Manufacturing', 'Jane Doe', 'jane.doe@acme.com', 'ACTIVE', TRUE),
    ('Globex Inc', 'Technology', 'John Smith', 'john.smith@globex.com', 'PROSPECTIVE', TRUE),
    ('Initech', 'Finance', 'Michael Bolton', 'michael.bolton@initech.com', 'FORMER', TRUE)
ON CONFLICT (company_name) DO NOTHING;
