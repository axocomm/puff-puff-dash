(ns puff-puff-dash.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [puff-puff-dash.layout :refer [error-page]]
            [puff-puff-dash.routes :refer [api-routes home-routes]]
            [compojure.route :as route]
            [puff-puff-dash.env :refer [defaults]]
            [mount.core :as mount]
            [puff-puff-dash.middleware :as middleware]))

(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop  ((or (:stop defaults) identity)))

(def app-routes
  (routes
   (-> #'api-routes
       (wrap-routes middleware/wrap-csrf)
       (wrap-routes middleware/wrap-formats))
   (-> #'home-routes
       (wrap-routes middleware/wrap-csrf)
       (wrap-routes middleware/wrap-formats))
   (route/not-found
    (:body
     (error-page {:status 404
                  :title  "page not found"})))))

(defn app [] (middleware/wrap-base #'app-routes))
