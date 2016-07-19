(ns puff-puff-dash.models.links
  (:require [puff-puff-dash.db.core :refer [*db*] :as db]
            [puff-puff-dash.helpers :refer [defaction link-sources] :as helpers]
            [puff-puff-dash.query-helpers :as query-helpers]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.data.json :as json]))

(defn tag! [link-id tag]
  (db/create-tag! {:id      (helpers/gen-id)
                   :link_id link-id
                   :tag     tag}))

(defn parse-properties [link]
  (if-let [properties (:properties link)]
    (assoc link :properties (json/read-str properties :key-fn keyword))
    link))

(defaction all [& [params]]
  (let [{:keys [query limit order offset]} params
        _                                  (when-not (query-helpers/valid-query? query)
                                             (throw (Exception. "Invalid query")))
        links                              (if (:tagged query)
                                             (db/get-links-by-tag
                                              {:tag (first (:tagged query))})
                                             (db/get-links))
        links                              (map parse-properties links)
        links                              (if query
                                             (query-helpers/apply-query links query)
                                             links)
        links                              (if offset
                                             (drop offset links)
                                             links)
        links                              (if limit
                                             (take limit links)
                                             links)]
    (merge {:success true
            :links   links}
           (when query {:query query})
           (when limit {:limit limit})
           (when order {:order order})
           (when offset {:offset offset}))))

(defaction by-id [id]
  (if-let [link (db/get-link {:id id})]
    {:success true
     :link    link}
    {:success false
     :error   "Link does not exist"}))

(defaction import! [links {:keys [source tag]}]
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
                (tag! link-id tag)))
            {:success  true
             :imported total}))))
    {:success false
     :error   "Invalid source"}))

(defaction delete! [link-id]
  {:success true
   :deleted (db/delete-link! {:id link-id})})
