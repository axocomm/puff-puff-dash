-- :name create-link! :! :n
-- :doc insert a link
INSERT INTO links
(id, external_id, title, url, domain, source, properties)
VALUES (:id, :external_id, :title, :url, :domain, :source, :properties)

-- :name update-link! :! :n
-- :doc update a link
UPDATE links
SET external_id = :external_id,
    title = :title,
    url = :url,
    domain = :domain,
    source = :source,
    properties = :properties
WHERE id = :id

-- :name delete-link! :! :n
-- :doc delete a link
DELETE FROM links WHERE id = :id

-- :name get-links :? :*
-- :doc get links
SELECT * FROM links

-- :name get-link :? :1
-- :doc get a link
SELECT * FROM links
WHERE id = :id

-- :name create-tag! :! :n
-- :doc create a tag for a link
INSERT INTO tags
(id, link_id, tag)
VALUES (:id, :link_id, :tag)

-- :name delete-tag! :! :n
-- :doc delete a tag given a name and link ID
DELETE FROM tags
WHERE link_id = :link_id
AND tag = :tag

-- :name get-tags :? :*
-- :doc get tags
SELECT * FROM tags

-- :name tags-for-link :? :*
-- :doc get all tags for a link given its ID
SELECT * FROM tags
WHERE link_id = :link_id
