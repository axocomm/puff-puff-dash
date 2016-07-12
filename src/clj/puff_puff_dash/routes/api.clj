(ns puff-puff-dash.routes.api
  (:require [puff-puff-dash.layout :as layout]
            [compojure.core :refer [context GET]]
            [ring.util.http-response :as response]))

(def api-routes
  (context "/api" []
           (context "/links" []
                    (GET "/" [] {:body {:success true
                                        :links   []}}))))
