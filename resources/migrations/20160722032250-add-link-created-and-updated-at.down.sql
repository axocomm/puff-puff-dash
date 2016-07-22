DROP TRIGGER IF EXISTS update_last_modified_time;
DROP FUNCTION IF EXISTS update_last_modified;

ALTER TABLE links DROP COLUMN created_at;
ALTER TABLE links DROP COLUMN updated_at;
