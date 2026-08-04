INSERT INTO clients (company_name, industry, primary_contact_name, primary_contact_email, relationship_status)
VALUES
    ('Acme Corp', 'Manufacturing', 'Jane Doe', 'jane.doe@acme.com', 'ACTIVE'),
    ('Globex Inc', 'Technology', 'John Smith', 'john.smith@globex.com', 'PROSPECTIVE'),
    ('Initech', 'Finance', 'Michael Bolton', 'michael.bolton@initech.com', 'FORMER')
ON CONFLICT (company_name) DO NOTHING;
