(ns puff-puff-dash.link-query
  (:require [reagent.core :as r]))

(def query (r/atom nil))
(def result (r/atom nil))

(defn query-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:div#query-container
      [:textarea.form-control
       {:id        "query"
        :style     {:font-family "monospace"}
        :rows      6
        :on-change #(reset! query (-> % .-target .-value))}
       @query]]]]
   [:div.row
    [:div.col-md-6
     [:div#query-buttons
      [:button.btn.btn-primary
       {:on-click #(.log js/console (str {:query @query, :result @result}))}
       "Evaluate"]]]
    [:div.col-md-6
     [:div#query-result
      [:pre {:style {:background-color "#ededed"
                     :padding          20
                     :border-radius    4}}
       @query]]]]])
