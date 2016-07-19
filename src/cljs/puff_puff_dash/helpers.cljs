(ns puff-puff-dash.helpers
  (:require [cognitect.transit :as t]))

(def r (t/reader :json))

(defn from-json [s]
  (t/read r s))
