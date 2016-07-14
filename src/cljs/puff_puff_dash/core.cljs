(ns puff-puff-dash.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [puff-puff-dash.ajax :refer [load-interceptors!]]
            [ajax.core :refer [GET POST]]
            [clojure.string :as string])
  (:import goog.History))

(declare set-search!)
(declare query-links)

(defn nav-link [uri title page collapsed?]
  [:li.nav-item
   {:class (when (= page (session/get :page)) "active")}
   [:a.nav-link
    {:href uri
     :on-click #(reset! collapsed? true)} title]])

(defn navbar []
  (let [collapsed? (r/atom true)]
    (fn []
      [:nav.navbar.navbar-light.bg-faded
       [:button.navbar-toggler.hidden-sm-up
        {:on-click #(swap! collapsed? not)} "☰"]
       [:div.collapse.navbar-toggleable-xs
        (when-not @collapsed? {:class "in"})
        [:a.navbar-brand {:href "#/"} "puff-puff-dash"]
        [:ul.nav.navbar-nav
         [nav-link "#/" "Home" :home collapsed?]
         [nav-link "#/about" "About" :about collapsed?]]]])))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     "this is the story of puff-puff-dash... work in progress"]]])

(defn link-item [{:keys [title url id domain] :as link}]
  [:div.link-item {:id (str "link-" id)}
   [:a {:href   url
        :title  title
        :class  (str "domain-" domain)
        :target "_blank"}
    title]
   [:div.link-meta
    [:span.link-domain domain]]])

(defn link-search-form []
  [:form.link-search
   [:input {:type        "text"
            :id          "search-term"
            :placeholder "Search"
            :required    true
            :on-change   #(set-search! (-> % .-target .-value))}]
   [:input {:type  "submit"
            :value "Search"}]])

(defn home-page []
  [:div.container
   [:div.search-container
    [link-search-form]]
   [:div.links-container
    [:h1 "All Links"]
    (let [links (or (query-links
                     (session/get :links)
                     (session/get :search-query))
                    [])]
      (if-not (empty? links)
        [:div.links
         (for [link links]
           ^{:key (:id link)}
           [link-item link])]
        [:span.error "No links haha"]))]])

(def pages
  {:home  #'home-page
   :about #'about-page})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/about" []
  (session/put! :page :about))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     HistoryEventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Helpers
(defn keywordize-keys [mp]
  (reduce (fn [acc [k v]]
            (assoc acc (keyword k) v))
          {}
          mp))

(defn query-str->query [query-str]
  (reduce
       (fn [acc term]
         (let [[k v] (string/split term #":")]
           (if-not (empty? v)
             (assoc acc (keyword k) (name v))
             acc)))
       {}
       (string/split query-str #" +")))

(defn query-links [links query]
  (.log js/console (str query))
  (filter (fn [link]
            (every? (fn [[k v]]
                      (= (get link k) v))
                    query))
          links))

;; -------------------------
;; Initialize app
(defn fetch-links! []
  (GET (str js/context "/links")
       {:handler (fn [response]
                   (if (get response "success")
                     (session/put!
                      :links
                      (map keywordize-keys (get response "links")))
                     (.log js/console (get response "error"))))}))

(defn set-search!
  ([]
   (set-search! ""))
  ([query-str]
   (session/put! :search-query (query-str->query query-str))))

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (load-interceptors!)
  (fetch-links!)
  (set-search!)
  (hook-browser-navigation!)
  (mount-components))
