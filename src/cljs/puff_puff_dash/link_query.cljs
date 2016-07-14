(ns puff-puff-dash.link-query
  (:require [reagent.core :as r]
            [clojure.string :as string]))

(def query (r/atom (string/join "\n" ["where domain = 'foo'"
                                      "where title  ~ 'bar'"])))
(def result (r/atom nil))

(defn clj->json
  ([x]
   (.stringify js/JSON (clj->js x)))
  ([x spacing]
   (.stringify js/JSON (clj->js x) nil spacing)))

(defn ->clause [clause-str]
  (let [[kw & tokens] (string/split clause-str #" +")
        kw            (-> kw string/lower-case keyword)
        clause-map    (case kw
                        :where
                        (let [[field cmp-str value] tokens
                              cmp                   (case cmp-str
                                                      "=" :equals
                                                      "~" :like
                                                      "/" :not-equals
                                                      nil)]
                          (if cmp
                            {:cmp   cmp
                             :field field
                             :value value}
                            {:error (str "Invalid comparison " cmp-str)}))

                        :order
                        (let [[field order] tokens
                              order         (or order "asc")
                              order         (-> order string/lower-case keyword)]
                          (if (some #{order} [:asc :desc])
                            {:field field
                             :order order}
                            {:error (str "Invalid order " order)}))

                        {:error (str "Invalid clause type " kw)})]
    (assoc clause-map :type kw)))

(defn parse-query [query]
  (let [clauses (string/split query #"\n")
        clauses (group-by :type (map ->clause clauses))]
    {:clauses clauses}))

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
       {:on-click #(reset! result (parse-query @query))}
       "Evaluate"]]]
    [:div.col-md-6
     [:div#query-result
      [:pre {:style {:background-color "#ededed"
                     :padding          20
                     :border-radius    4}}
       (clj->json @result 2)]]]]])
