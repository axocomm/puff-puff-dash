CREATE TABLE tags (
       id CHARACTER VARYING(32) PRIMARY KEY,
       link_id CHARACTER VARYING(32) NOT NULL,
       tag VARCHAR(32) NOT NULL,
       FOREIGN KEY (link_id) REFERENCES links(id)
);