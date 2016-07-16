(ns puff-puff-dash.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [puff-puff-dash.ajax :refer [load-interceptors!]]
            [ajax.core :refer [GET POST]]
            [clojure.string :as string]
            [puff-puff-dash.link-query :as lq]
            [cognitect.transit :as t])
  (:import goog.History))

(def query-str (r/atom nil))
(def query (r/atom {}))
(def result (r/atom (session/get :links)))

(defn reset-all! []
  (do
    (reset! result (session/get :links))
    (reset! query {})
    (reset! query-str "")))

(def r (t/reader :json))

(defn from-json [s]
  (t/read r s))

;; -------------------------
;; Components
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

(defn link-meta
  "Currently just displays the domain and source of the link,
and in the case of reddit links, includes the subreddit."
  [{:keys [domain source properties]}]
  (let [link-source (case source
                      "reddit" (str source "/" (:subreddit properties))
                      source)]
    [:div.link-meta
     [:span.link-domain domain]
     " - "
     [:span.link-source link-source]]))

(defn link-item [{:keys [title url id domain] :as link}]
  [:div.link-item {:id (str "link-" id)}
   [:a {:href   url
        :title  title
        :class  (str "domain-" domain)
        :target "_blank"}
    title]
   [link-meta link]])

(defn query-container []
  [:div#query-container
   [:textarea.form-control
    {:id        "query"
     :style     {:font-family "monospace"}
     :rows      6
     :on-change (fn [e]
                  (do
                    (reset! query-str (-> e .-target .-value))
                    (reset! query (lq/query->map @query-str))))
     :value     @query-str}]])

(defn query-buttons []
  [:div#query-buttons {:style {:text-align :center}}
   [:button.btn.btn-primary
    {:style    {:margin    10
                :min-width 100}
     :on-click (fn [_]
                 (reset! result
                         (lq/apply-query
                          @query
                          (session/get :links))))}
    "Evaluate"]
   [:button.btn.btn-danger
    {:style    {:margin    10
                :min-width 100}
     :on-click #'reset-all!}
    "Reset"]])

(defn query-display []
  [:div#query-display
   [:pre {:style {:background-color "#ededed"
                  :padding          20
                  :border-radius    4
                  :height           300
                  :overflow-y       :scroll}}
    (lq/clj->json @query 2)]])

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
   [:h1 "Search"]
   [:div.row
    [:div.col-md-6
     [query-container]
     [query-buttons]]
    [:div.col-md-6
     [query-display]]]])

(defn home-page []
  [:div.container
   [query-page]
   [:div.links-container
    [:h1 "All Links"]
    (if-not (empty? @result)
      [:div.links
       (for [link @result]
         ^{:key (:id link)}
         [link-item link])]
      [:span.error "No links haha"])]])

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

;; -------------------------
;; Initialize app
(defn fetch-links! []
  (GET (str js/context "/links")
       {:handler (fn [response]
                   (if (get response "success")
                     (do
                       (session/put!
                        :links
                        (->> (get response "links")
                             (map keywordize-keys)
                             (map (fn [link]
                                    (assoc link :properties (-> link
                                                                :properties
                                                                from-json
                                                                keywordize-keys))))))
                       (reset-all!))
                     (.log js/console (get response "error"))))}))

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (load-interceptors!)
  (fetch-links!)
  (hook-browser-navigation!)
  (mount-components))
