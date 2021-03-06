(ns puff-puff-dash.routes
  (:require [puff-puff-dash.layout :as layout]
            [compojure.core :refer [defroutes context GET POST DELETE ANY]]
            [ring.util.http-response :as response]
            [clojure.data.json :as json]
            [puff-puff-dash.models.links :as links]
            [puff-puff-dash.models.tags :as tags]))

(defroutes static-routes
  (GET "/" [] (layout/render "home.html")))

(def link-routes
  (context "/links" []
    (ANY "/" {:keys [body]}
      (let [input  (slurp body)
            params (if-not (empty? input)
                     (json/read-str input :key-fn keyword)
                     {})
            links  (links/all params)]
        (layout/render-json links)))

    (GET "/:source" [source]
      (layout/render-json (links/all {:source source})))
    (POST "/:source" {:keys [body params]}
      (let [links (-> body slurp (json/read-str :key-fn keyword))]
        (layout/render-json (links/import! links params))))

    (context "/:id" [id]
      (GET "/" []
        (layout/render-json (links/by-id id)))
      (DELETE "/" []
        (layout/render-json (links/delete! id)))

      (GET "/tags" []
        (layout/render-json (tags/for-link id)))
      (POST "/tags/:tag" [tag]
        (layout/render-json (tags/tag! id tag)))
      (DELETE "/tags/:tag" [tag]
        (layout/render-json (tags/untag! id tag))))))

(def tag-routes
  (context "/tags" []
    (GET "/" []
      (layout/render-json (tags/counts)))))
