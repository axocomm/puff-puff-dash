(ns puff-puff-dash.routes
  (:require [puff-puff-dash.layout :as layout]
            [compojure.core :refer [defroutes context GET POST DELETE]]
            [ring.util.http-response :as response]
            [clojure.data.json :as json]
            [puff-puff-dash.db.core :refer [*db*] :as db]
            [clojure.string :as string]))

(def link-sources
  {:reddit {:marshal-fn
            (fn [link]
              {:title       (or (:title link)
                                (:link_title link))
               :external_id (:id link)
               :url         (:url link)
               :domain      (:domain link)
               :properties  (select-keys
                             link
                             [:subreddit :score])})}})

(defn gen-id []
  (str (java.util.UUID/randomUUID)))

(defn keywordize-keys [m]
  (reduce (fn [acc [k v]]
            (assoc acc (keyword k) v))
          {}
          m))

(defn map-val
  ([f m]
   (map-val f m {}))
  ([f m init]
   (reduce (fn [acc [k v]]
             (assoc acc k (f v)))
           init
           m)))

(defn exception-message [e]
  (try
    (str (.getNextException e))
    (catch Exception ee
      (.getMessage e))))

(defmacro defaction [name args & body]
  `(defn ~name ~args
     (try
       ~@body
       (catch Exception e#
         {:success false
          :error   (exception-message e#)}))))

(defn make-query-fn [query]
  (fn [link]
    (every? identity (map (fn [[key val]]
                            (= (get link key) val))
                          query))))

(defn query-links [links query]
  (filter (make-query-fn query) links))

;; --------
;; Handlers
(defaction get-links [& [query]]
  (let [links (db/get-links)]
    (if query
      {:success true
       :links   (query-links links (keywordize-keys query))
       :query   query}
      {:success true
       :links   links})))

(defaction get-link [id]
  (if-let [link (db/get-link {:id id})]
    {:success true
     :link    link}
    {:success false
     :error   "Link does not exist"}))

(defaction import-links [links {:keys [source tag]}]
  (if-let [source-opts (get link-sources (keyword source))]
    (let [marshal-fn (:marshal-fn source-opts)
          marshalled (map marshal-fn links)]
      (loop [[link & more] marshalled
             total         0
             ids           []]
        (if link
          (let [id      (gen-id)
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

(defaction get-tag-counts []
  {:success true
   :tags    (->> (db/get-tags)
                 (group-by :tag)
                 (map-val count))})

(defaction get-tags-for-link [link-id]
  (if (:success (get-link link-id))
    {:success true
     :tags    (map :tag (db/tags-for-link {:link_id link-id}))}
    {:success false
     :error   "Link does not exist"}))

(defaction tag-link [link-id tag]
  (if-not (db/get-link {:id link-id})
    {:success false
     :error   "Link does not exist"}
    (let [tag-rec {:id      (gen-id)
                   :link_id link-id
                   :tag     tag}]
      (do
        (db/create-tag! tag-rec)
        {:success true
         :tag     tag-rec}))))

(defaction untag-link [link-id tag]
  (if-not (db/get-link {:id link-id})
    {:success false
     :error   "Link does not exist"}
    (let [result (db/delete-tag! {:link_id link-id
                                  :tag     tag})]
      {:success true
       :deleted result})))

(defroutes static-routes
  (GET "/" [] (layout/render "home.html")))

(def link-routes
  (context "/links" []
    (GET "/" {:keys [params]}
      (layout/render-json (get-links params)))

    (GET "/:source" [source]
      (layout/render-json (get-links {:source source})))
    (POST "/:source" {:keys [body params]}
      (let [links (-> body slurp (json/read-str :key-fn keyword))]
        (layout/render-json (import-links links params))))

    (context "/:id" [id]
      (GET "/" []
        (layout/render-json (get-link id)))
      (DELETE "/" []
        (layout/render-json (delete-link id)))

      (GET "/tags" []
        (layout/render-json (get-tags-for-link id)))
      (POST "/tags/:tag" [tag]
        (layout/render-json (tag-link id tag)))
      (DELETE "/tags/:tag" [tag]
        (layout/render-json (untag-link id tag))))))

(def tag-routes
  (context "/tags" []
    (GET "/" []
      (layout/render-json (get-tag-counts)))))
