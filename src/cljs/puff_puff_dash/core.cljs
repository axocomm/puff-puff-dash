(ns puff-puff-dash.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [puff-puff-dash.ajax :refer [load-interceptors!]]
            [ajax.core :refer [GET POST]])
  (:import goog.History))

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
  [:li.link-item {:id id}
   [:a {:href url, :title title, :class (str "domain-" (name domain))}
    title]])

(defn home-page []
  [:div.container
   [:h1 "yo"]
   (if-let [links (session/get :links)]
     [:ul.links
      (for [link links]
        ^{:key (:id link)}
        [link-item link])]
     [:span.error "No links haha"])])

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
  (GET (str js/context "/links") {:handler (fn [response]
                                             (if (get response "success")
                                               (session/put!
                                                :links
                                                (map keywordize-keys (get response "links")))
                                               (.log js/console (get response "error"))))}))

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (load-interceptors!)
  (fetch-links!)
  (hook-browser-navigation!)
  (mount-components))
