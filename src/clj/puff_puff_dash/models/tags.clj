(ns puff-puff-dash.models.tags
  (:require [puff-puff-dash.db.core :refer [*db*] :as db]
            [puff-puff-dash.helpers :refer [defaction] :as helpers]))

(defaction counts []
  {:success true
   :tags    (->> (db/get-tags)
                 (group-by :tag)
                 (helpers/map-val count))})

(defaction for-link [link-id]
  (if (db/get-link {:id link-id})
    {:success true
     :tags    (map :tag (db/tags-for-link {:link_id link-id}))}
    {:success false
     :error   "Link does not exist"}))

(defaction tag! [link-id tag]
  (if-not (db/get-link {:id link-id})
    {:success false
     :error   "Link does not exist"}
    (let [tag-rec {:id      (helpers/gen-id)
                   :link_id link-id
                   :tag     tag}]
      (do
        (db/create-tag! tag-rec)
        {:success true
         :tag     tag-rec}))))

(defaction untag! [link-id tag]
  (if-not (db/get-link {:id link-id})
    {:success false
     :error   "Link does not exist"}
    (let [result (db/delete-tag! {:link_id link-id
                                  :tag     tag})]
      {:success true
       :deleted result})))
