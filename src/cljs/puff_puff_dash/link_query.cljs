(ns puff-puff-dash.link-query
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [clojure.string :as string]))

(defn clj->json
  ([x]
   (.stringify js/JSON (clj->js x)))
  ([x spacing]
   (.stringify js/JSON (clj->js x) nil spacing)))

(defn ->clause
  "Split a query line into its parts and try to return a map of its details.

If any error cases are encountered, just throw to catch in `query->map'"
  [clause-str]
  (let [[kw & tokens] (string/split clause-str #" +")
        kw            (-> kw string/lower-case keyword)
        clause        (case kw
                        :where
                        (let [[field cmp-str & value] tokens
                              value                   (string/join " " value)
                              cmp                     (case cmp-str
                                                        "=" :equals
                                                        "~" :like
                                                        "/" :not-equals
                                                        nil)]
                          (if cmp
                            {:cmp   cmp
                             :field field
                             :value value}
                            (throw (js/Error (str "Invalid comparison " cmp-str)))))

                        :order
                        (let [[field direction] tokens
                              direction         (or direction "asc")
                              direction         (-> direction string/lower-case keyword)]
                          (if (some #{direction} [:asc :desc])
                            {:field     (keyword field)
                             :direction direction}
                            (throw (js/Error (str "Invalid direction " (name direction))))))

                        :limit
                        (let [limit (-> tokens first js/parseInt)]
                          (if (or (nil? limit)
                                  (js/isNaN limit)
                                  (< limit 1))
                            (throw (js/Error "Invalid limit"))
                            {:limit limit}))

                        (throw (js/Error (str "Invalid clause type " (name kw)))))]
    (assoc clause :type kw)))

(defn parse-query
  "Transform the query string into a map containing keys for
clause types where, order, and limit."
  [query-string]
  (let [lines                       (string/split query-string #"\n")
        {:keys [where order limit]} (->> lines
                                         (map ->clause)
                                         (group-by :type))
        query                       {:where where
                                     :order (first order)
                                     :limit (-> limit first :limit)}]
    (into {} (filter val query))))

(defn query->map
  "Parse the query into parts and just return error if one is thrown."
  [query-string]
  (when-not (empty? query-string)
    (try
      {:clauses (parse-query query-string)}
      (catch js/Error e
        {:error (str e)}))))

(defn ->where-fn
  "Return a function that performs the given comparison on a link."
  [{:keys [cmp field value]}]
  (let [field    (keyword field)
        accessor (fn [l f]
                   (if-let [pk (->> f
                                    name
                                    (re-matches #"^([^\.]+)\.(.+)$")
                                    last
                                    keyword)]
                     (get-in l [:properties pk])
                     (get l f)))]
    (case cmp
      :equals     #(= (accessor % field) value)
      :not-equals #(not= (accessor % field) value)
      :like       (fn [link]
                    (re-find (re-pattern (str "(?i)" value))
                             (or (accessor link field) ""))))))

(defn matches-all?
  "Determine if the link satisfies all given predicates."
  [fns link]
  (every? #(% link) fns))

(defn apply-query [query links]
  (let [{:keys [where order limit]} (:clauses query)
        where-fns                   (when where
                                      (map ->where-fn where))

        links                       (if where-fns
                                      (filter
                                       #(matches-all? where-fns %)
                                       links)
                                      links)
        links                       (if order
                                      (let [{:keys [field direction]} order
                                            ordered                   (sort-by field links)]
                                        (if (= direction :desc)
                                          (reverse ordered)
                                          ordered))
                                      links)
        links                       (if limit
                                      (take limit links)
                                      links)]
    links))
