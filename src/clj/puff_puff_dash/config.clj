(ns puff-puff-dash.config
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [args defstate]]
            [clojure.data.json :as json]
            [clojure.java.io :as io]))

(defn from-json-config [filename]
  (-> (io/resource filename)
      slurp
      (json/read-str :key-fn keyword)))

(defstate env :start (load-config
                      :merge
                      [(args)
                       (source/from-system-props)
                       (source/from-env)
                       (from-json-config "config.json")]))
