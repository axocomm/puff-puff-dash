(ns puff-puff-dash.routes
  (:require [puff-puff-dash.layout :as layout]
            [compojure.core :refer [defroutes context GET]]
            [ring.util.http-response :as response]))

(defroutes home-routes
  (GET "/" [] (layout/render "home.html")))

(def api-routes
  (context "/api" []
           (context "/links" []
                    (GET "/" [] {:body {:success true
                                        :links   []}}))))
