(ns puff-puff-dash.routes
  (:require [puff-puff-dash.layout :as layout]
            [compojure.core :refer [defroutes context GET]]
            [ring.util.http-response :as response]))

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

(defroutes static-routes
  (GET "/" [] (layout/render "home.html")))

(def link-routes
  (context "/links" []
           (GET "/" [] {:body {:success true
                               :links   example-links}})))
