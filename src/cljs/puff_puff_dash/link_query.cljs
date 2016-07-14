(ns puff-puff-dash.link-query
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [clojure.string :as string]))

(def query (r/atom nil))
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
                        (let [[field direction] tokens
                              direction         (or direction "asc")
                              direction         (-> direction string/lower-case keyword)]
                          (if (some #{direction} [:asc :desc])
                            {:field     (keyword field)
                             :direction direction}
                            {:error (str "Invalid direction " (name direction))}))

                        :limit
                        (let [limit (-> tokens first js/parseInt)]
                          (if (or (nil? limit)
                                  (js/isNaN limit)
                                  (< limit 1))
                            {:error "Invalid limit"}
                            {:limit limit}))

                        {:error (str "Invalid clause type " (name kw))})]
    (assoc clause-map :type kw)))

;; TODO better handling of errors
(defn parse-query [query-string]
  (let [clauses (string/split query-string #"\n")]
    (group-by :type (map ->clause clauses))))

(defn query->map [query-string]
  (when-not (empty? query-string)
    (try
      (let [clauses (parse-query query-string)]
        {:clauses clauses})
      (catch js/Error e
        {:error (str e)}))))

(defn ->where-fn [{:keys [cmp field value]}]
  (let [field (keyword field)]
    (case cmp
      :equals     #(= (get % field) value)
      :not-equals #(not= (get % field) value)
      :like       #(re-find (re-pattern value) (or (get % field) "")))))

(defn where-matches? [fns link]
  (every? #(apply % [link]) fns))

(defn apply-query [query links]
  (let [{:keys [where order limit]} (:clauses query)
        where-fns                   (when where
                                      (map ->where-fn where))

        links (if [where-fns]
                (filter
                 #(where-matches? where-fns %)
                 links)
                links)
        links (if order
                (let [{:keys [field direction]} (first order)
                      ordered                   (sort-by field links)]
                  (if (= direction :desc)
                    (reverse ordered)
                    ordered))
                links)
        links (if limit
                (take (-> limit first :limit) links)
                links)]
    links))

(defn query-container []
  [:div#query-container
   [:textarea.form-control
    {:id        "query"
     :style     {:font-family "monospace"}
     :rows      6
     :on-change #(reset! query (-> % .-target .-value query->map))}
    (string/join "\n" ["where domain = soundcloud.com"])]])

(defn query-buttons []
  [:div#query-buttons
   [:button.btn.btn-primary
    {:on-click (fn [_]
                 (reset! result
                         (apply-query
                          @query
                          (session/get :links))))}
    "Evaluate"]])

(defn query-display []
  [:div#query-result
   [:pre {:style {:background-color "#ededed"
                  :padding          20
                  :border-radius    4}}
    (clj->json @query 2)]])

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
     [query-display]]
    [:div.col-md-6
     [query-matches]]]])
