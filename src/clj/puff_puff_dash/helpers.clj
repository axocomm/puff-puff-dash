(ns puff-puff-dash.helpers)

(def link-sources
  {:reddit {:marshal-fn
            (fn [link]
              {:title       (or (:title link)
                                (:link_title link))
               :external_id (:id link)
               :url         (:url link)
               :domain      (:domain link)
               :properties  (merge
                             (select-keys
                              link
                              [:subreddit :score :media :created])
                             {:permalink (str "https://reddit.com" (:permalink link))})})}})

(defn gen-id []
  (str (java.util.UUID/randomUUID)))

(defn keywordize-keys [m]
  (reduce (fn [acc [k v]]
            (assoc acc (keyword k) v))
          {}
          m))

(defn map-val
  ([f m]
   (map-val f m {}))
  ([f m init]
   (reduce (fn [acc [k v]]
             (assoc acc k (f v)))
           init
           m)))

(defn parse-int
  "Try to parse an integer from the string or return nil."
  [s]
  (try
    (Integer/parseInt s)
    (catch Exception _
      nil)))

(defn exception-message [e]
  (try
    (str (.getNextException e))
    (catch Exception ee
      (.getMessage e))))

(defmacro defaction [name args & body]
  `(defn ~name ~args
     (try
       ~@body
       (catch Exception e#
         {:success false
          :error   (exception-message e#)}))))

(defn make-query-fn [query]
  (fn [link]
    (every? identity (map (fn [[key val]]
                            (= (get link key) val))
                          query))))

(defn query-links [links query]
  (filter (make-query-fn query) links))
