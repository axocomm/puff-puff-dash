(ns user
  (:require [mount.core :as mount]
            [puff-puff-dash.figwheel :refer [start-fw stop-fw cljs]]
            puff-puff-dash.core))

(defn start []
  (mount/start-without #'puff-puff-dash.core/repl-server))

(defn stop []
  (mount/stop-except #'puff-puff-dash.core/repl-server))

(defn restart []
  (stop)
  (start))


