(ns puff-puff-dash.routes
  (:require [puff-puff-dash.layout :as layout]
            [compojure.core :refer [defroutes context GET POST]]
            [ring.util.http-response :as response]
            [clojure.data.json :as json]
            [puff-puff-dash.db.core :refer [*db*] :as db]))

(def example-links
  [{:id     1
    :title  "Foo"
    :url    "http://google.com"
    :domain :soundcloud
    :source :reddit}
   {:id     2
    :title  "Your mom"
    :url    "http://blah.com"
    :domain :soundcloud
    :source :reddit}])

(def link-sources
  {:reddit {:marshal-fn
            (fn [link]
              {:title       (:title link)
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
            (db/upsert-link! link)))
        marshalled))))

(defroutes static-routes
  (GET "/" [] (layout/render "home.html")))

(def link-routes
  (context "/links" []
           (GET "/" [] {:body {:success true
                               :links   example-links}})
           (POST "/" {:keys [body params]}
                 (let [links  (-> body
                                  slurp
                                  (json/read-str :key-fn keyword))
                       links  (take 5 links)
                       result (import-links links params)]
                   {:body {:success result}}))))
