-- :name upsert-link! :! :n
-- :doc insert or update a link
INSERT INTO links
(id, external_id, title, url, domain, source, properties)
VALUES (:id, :external_id, :title, :url, :domain, :source, :properties)

-- :name delete-link! :! :n
-- :doc delete a link
DELETE FROM links WHERE id = :id

-- :name get-links :? :*
-- :doc get links
SELECT * FROM links
