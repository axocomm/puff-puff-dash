(ns puff-puff-dash.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [puff-puff-dash.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[puff-puff-dash started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[puff-puff-dash has shut down successfully]=-"))
   :middleware wrap-dev})
