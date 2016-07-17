(ns puff-puff-dash.models.links
  (:require [puff-puff-dash.db.core :refer [*db*] :as db]
            [puff-puff-dash.helpers :refer [defaction link-sources] :as helpers]))

(defn tag-link [link-id tag]
  (db/create-tag! {:id      (helpers/gen-id)
                   :link_id link-id
                   :tag     tag}))

(defaction get-links [& [query]]
  (let [links (db/get-links)

        limit (helpers/parse-int (:limit query))
        query (dissoc (helpers/keywordize-keys query) :limit)

        links (if query
                (helpers/query-links links query)
                links)
        links (if limit
                (take limit links)
                links)]
    (merge
     {:success true
      :links   links}
     (when query {:query query})
     (when limit {:limit limit}))))

(defaction get-link [id]
  (if-let [link (db/get-link {:id id})]
    {:success true
     :link    link}
    {:success false
     :error   "Link does not exist"}))

(defaction import-links [links {:keys [source tag]}]
  (if-let [source-opts (get helpers/link-sources (keyword source))]
    (let [marshal-fn (:marshal-fn source-opts)
          marshalled (map marshal-fn links)]
      (loop [[link & more] marshalled
             total         0
             ids           []]
        (if link
          (let [id      (helpers/gen-id)
                link    (merge link {:id     id
                                     :source source})
                created (db/create-link! link)]
            (recur
             more
             (+ total created)
             (conj ids id)))
          (do
            (when tag
              (doseq [link-id ids]
                (tag-link link-id tag)))
            {:success  true
             :imported total}))))
    {:success false
     :error   "Invalid source"}))

(defaction delete-link [link-id]
  {:success true
   :deleted (db/delete-link! {:id link-id})})
