(ns puff-puff-dash.routes
  (:require [puff-puff-dash.layout :as layout]
            [compojure.core :refer [defroutes context GET POST DELETE]]
            [ring.util.http-response :as response]
            [clojure.data.json :as json]
            [puff-puff-dash.db.core :refer [*db*] :as db]
            [clojure.string :as string])
  (:import java.sql.SQLException))

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

(defn import-links [links {:keys [source]}]
  (when-let [source-opts (get link-sources (keyword source))]
    (let [marshal-fn (:marshal-fn source-opts)
          marshalled (map marshal-fn links)]
      (do
        (doseq [link marshalled]
          (let [properties-str (json/write-str (:properties link))
                id             (gen-id)
                link           (merge link {:id         id
                                            :properties properties-str
                                            :source     source})]
            (db/create-link! link)))
        marshalled))))

(defn tag-link! [link-id tag]
  (let [tag-map {:link_id link-id
                 :tag     tag
                 :id      (gen-id)}]
    (db/create-tag! tag-map)))

(defmacro defaction [name args & body]
  `(defn ~name ~args
     (try
       ~@body
       (catch SQLException e#
         {:success false
          :error   (.getNextException e#)})
       (catch Exception e#
         {:success false
          :error   (str (.getMessage e#))}))))

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

(defroutes static-routes
  (GET "/" [] (layout/render "home.html")))

(def link-routes
  (context "/links" []
           (GET "/" {:keys [params]} (layout/render-json (get-links params)))
           (POST "/" {:keys [body params]}
                 (let [links  (-> body
                                  slurp
                                  (json/read-str :key-fn keyword))
                       result (try
                                (do
                                  (import-links links params)
                                  {:success  true
                                   :imported (count links)})
                                (catch Exception e
                                  {:success false
                                   :error   (str (.getNextException e))}))]
                   (layout/render-json result)))
           (DELETE "/" {:keys [params]}
                   (if-let [ids (:ids params)]
                     (do
                       (doseq [id (string/split ids #",")]
                         (db/delete-link! {:id id}))
                       (layout/render-json {:success true
                                            :deleted (string/split ids #",")}))
                     (layout/render-json {:success false
                                          :error   "Not implemented yet"})))
           (context "/:id" [id]
                    (GET "/" [] (layout/render-json (get-link id)))
                    (GET "/tags" [] (layout/render-json (get-tags-for-link id)))
                    (POST "/tag/:tag" [tag]
                          (layout/render-json
                           (try
                             (do
                               (tag-link! id tag)
                               {:success true
                                :tagged  [id tag]})
                             (catch Exception e
                               {:success false
                                :error   (str (.getNextException e))})))))))

(def tag-routes
  (context "/tags" []
           (GET "/" []
                (layout/render-json (get-tag-counts)))))
