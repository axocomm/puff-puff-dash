ALTER TABLE tags ADD COLUMN created_at TIMESTAMP DEFAULT NOW();

UPDATE tags SET created_at = NOW();
