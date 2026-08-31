ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users
    ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';

ALTER TABLE users
    ADD CONSTRAINT chk_users_auth_provider
        CHECK (
            auth_provider IN (
                'LOCAL',
                'GOOGLE'
            )
        );
