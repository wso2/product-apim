SELECT DISTINCT constraint_name FROM information_schema.constraints WHERE table_name = 'AM_APP_KEY_DOMAIN_MAPPING';
ALTER TABLE AM_APP_KEY_DOMAIN_MAPPING DROP CONSTRAINT <temp_key_name>;
