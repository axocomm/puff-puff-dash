(ns puff-puff-dash.dashboards
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [puff-puff-dash.link-query :as lq]
            [cognitect.transit :as t]))

(def dashboards
  {:videos {:query {:where [{:cmp   :equals
                             :field :properties.subreddit
                             :value "videos"}]}}})

(defn query-for-dashboard [links type]
  (when-let [query (:query (get dashboards type))]
    (lq/apply-query {:clauses query} links)))

(defn videos-dashboard []
  [:div.container
   (if-let [links (query-for-dashboard (session/get :links)
                                       :videos)]
     [:p (str "Got " (count links) " links")]
     [:p "No links"])])

(defn images-dashboard []
  [:div.container
   (if-let [links (query-for-dashboard (session/get :links)
                                       :images)]
     [:p (str "Got " (count links) " links")]
     [:p "No links"])])
