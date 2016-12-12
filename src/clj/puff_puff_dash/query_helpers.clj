(ns puff-puff-dash.query-helpers)

;; TODO this
(defn valid-query? [query]
  true)

;; TODO portable accessor creator
(defn ->where-fn
  "Retrun a function that performs the given comparison on a link."
  [{:keys [cmp field value]}]
  (let [field    (keyword field)
        cmp      (keyword cmp)
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
  "Determine if the given link satisfies all predicates."
  [link fns]
  (every? #(% link) fns))

(defn apply-query
  "Filter links based on the given query.

Currently only handles `where' clauses"
  [links query]
  (let [{:keys [where]} query
        links (if where
                (filter
                 #(matches-all? % (map ->where-fn where))
                 links)
                links)]
    links))
