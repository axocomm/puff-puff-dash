(ns puff-puff-dash.helpers
  (:require [cognitect.transit :as t]
            [clojure.string :as string]))

(def r (t/reader :json))

(defn from-json [s]
  (t/read r s))

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

                        :tagged
                        (let [tags tokens]
                          {:tags tags})

                        (throw (js/Error (str "Invalid clause type " (name kw)))))]
    (assoc clause :type kw)))

(defn parse-query
  "Transform the query string into a map containing keys for
clause types where, order, and limit."
  [query-string]
  (let [lines                              (string/split query-string #"\n")
        {:keys [where order limit tagged]} (->> lines
                                                (map ->clause)
                                                (group-by :type))
        query                              (merge {}
                                                  (when where {:where where})
                                                  (when tagged
                                                    {:tagged (-> tagged first :tags)}))
        params                             {:order (first order)
                                            :limit (-> limit first :limit)
                                            :query query}]
    (into {} (filter val params))))

(defn query->map
  "Parse the query into parts and just return error if one is thrown."
  [query-string]
  (when-not (empty? query-string)
    (try
      (parse-query query-string)
      (catch js/Error e
        {:error (str e)}))))
