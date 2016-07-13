CREATE TABLE links (
       id CHARACTER VARYING(32) PRIMARY KEY,
       external_id CHARACTER VARYING(32) NOT NULL,
       title CHARACTER VARYING(128) NOT NULL,
       url TEXT,
       domain VARCHAR(32),
       source VARCHAR(32),
       properties TEXT DEFAULT '{}'
);
