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

(defroutes static-routes
  (GET "/" [] (layout/render "home.html")))

(def link-routes
  (context "/links" []
           (GET "/" [] (layout/render-json {:success true
                                            :links   (db/get-links)}))
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
                    (GET "/" [] (layout/render-json {:id id}))
                    (GET "/tags" []
                         (layout/render-json {:tags
                                              (map :tag (db/tags-for-link {:link_id id}))}))
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
                (layout/render-json {:tags (db/get-tags)}))))
