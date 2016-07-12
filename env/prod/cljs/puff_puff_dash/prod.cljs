(ns puff-puff-dash.app
  (:require [puff-puff-dash.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
