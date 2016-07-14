(ns puff-puff-dash.link-query
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [clojure.string :as string]))

(def query (r/atom (string/join "\n" ["where domain = soundcloud.com"])))
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

                        {:error (str "Invalid clause type " (name kw))})]
    (assoc clause-map :type kw)))

(defn parse-query [query]
  (let [clauses (string/split query #"\n")]
    (group-by :type (map ->clause clauses))))

(defn query->map [query]
  (try
    (let [clauses (parse-query query)]
      {:clauses clauses})
    (catch js/Error e
      {:error (.getMessage e)})))

(defn ->where-fn [{:keys [cmp field value]}]
  (let [field (keyword field)]
    (case cmp
      :equals     #(= (get % field) value)
      :not-equals #(not= (get % field) value)
      :like       #(re-find (re-pattern value) (get % field)))))

(defn where-matches? [fns link]
  (every? #(apply % [link]) fns))

(defn apply-query [query links]
  (let [{:keys [where order]} (:clauses query)
        where-fns             (when where
                                (map ->where-fn where))
        matching-links        (if [where-fns]
                                (filter
                                 #(where-matches? where-fns %)
                                 links)
                                links)]
    matching-links))

(defn query-container []
  [:div#query-container
   [:textarea.form-control
    {:id        "query"
     :style     {:font-family "monospace"}
     :rows      6
     :on-change #(reset! query (-> % .-target .-value))}
    @query]])

(defn query-buttons []
  [:div#query-buttons
   [:button.btn.btn-primary
    {:on-click (fn [_]
                 (reset! result
                         (apply-query
                          (query->map @query)
                          (session/get :links))))}
    "Evaluate"]])

(defn query-results []
  [:div#query-result
   [:pre {:style {:background-color "#ededed"
                  :padding          20
                  :border-radius    4}}
    (clj->json (query->map @query) 2)]])

(defn query-matches []
  [:ul#matches
   (for [link @result]
     ^{:key (:id link)}
     [:li
      [:ul
       [:li [:strong "Title: "] (:title link)]
       [:li [:strong "Domain: "] (:domain link)]]])])

(defn query-page []
  [:div.container
   [:div.row
    [:div.col-md-6
     [query-container]
     [query-buttons]
     [query-results]]
    [:div.col-md-6
     [query-matches]]]])
