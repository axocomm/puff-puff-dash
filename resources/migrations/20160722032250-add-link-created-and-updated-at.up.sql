ALTER TABLE links ADD COLUMN created_at TIMESTAMP DEFAULT NOW();
ALTER TABLE links ADD COLUMN updated_at TIMESTAMP DEFAULT NOW();

UPDATE links SET created_at = NOW();
UPDATE links SET updated_at = NOW();

CREATE OR REPLACE FUNCTION update_last_modified() RETURNS TRIGGER AS '
       BEGIN
        NEW.updated_at = NOW();
        RETURN NEW;
       END;
' LANGUAGE 'plpgsql';

CREATE TRIGGER update_last_modified_time BEFORE UPDATE ON links
       FOR EACH ROW EXECUTE PROCEDURE update_last_modified();
