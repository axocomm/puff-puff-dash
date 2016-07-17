(ns puff-puff-dash.dashboards
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [puff-puff-dash.link-query :as lq]
            [cognitect.transit :as t]
            [clojure.string :as string]))

(def dashboards
  {:videos {:queries [{:where [{:cmp   :equals
                                :field :properties.subreddit
                                :value "videos"}]}
                      {:where [{:cmp   :like
                                :field :domain
                                :value "youtube"}]}]}
   :images {:queries [{:where [{:cmp   :equals
                                :field :properties.subreddit
                                :value "pics"}]}
                      {:where [{:cmp   :like
                                :field :domain
                                :value "imgur"}]}]}})

(defn query-for-dashboard [links type]
  (when-let [queries (:queries (get dashboards type))]
    (->> queries
         (map #(lq/apply-query {:clauses %} links))
         (apply concat)
         set)))

(defn image-link [{:keys [domain url] :as link}]
  (if (= domain "i.imgur.com")
    [:img {:src url}]
    (let [imgur-id (last (string/split url #"/"))]
      [:blockquote {:class "imgur-embed-pub"
                    :lang  "en"
                    :data  {:id      imgur-id
                            :context false}}])))

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
     (for [link (take 10 (filter :url links))]
       ^{:key (:id link)}
       [image-link link])
     [:p "No links"])])
