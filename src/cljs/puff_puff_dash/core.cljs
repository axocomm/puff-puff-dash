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
            [puff-puff-dash.dashboards :as dashboards]
            [puff-puff-dash.helpers :as helpers]
            [cljs-react-material-ui.core :as ui]
            [cljs-react-material-ui.reagent :as rui]
            [cljs-react-material-ui.icons :as ic])
  (:import goog.History))

(def query-str (r/atom nil))
(def query (r/atom {}))
(def result (r/atom (session/get :links)))

(defn reset-all! []
  (do
    (reset! result (session/get :links))
    (reset! query {})
    (reset! query-str "")))

;; -------------------------
;; Components
(defn nav-link [uri title page collapsed?]
  [rui/menu-item
   {:class (when (= page (session/get :page)) "active")}
   [:a {:href     uri
        :title    title
        :on-click #(reset! collapsed? true)
        :style    {:text-decoration :none
                   :color           "#000"}}
    title]])

(defn navbar []
  (let [collapsed? (r/atom true)]
    (fn []
      [rui/mui-theme-provider
       {:mui-theme (ui/get-mui-theme)}
       [:div.navbar
        [rui/drawer
         {:open              (not @collapsed?)
          :docked            false
          :on-request-change #(reset! collapsed? (not %))}
         [nav-link "#/" "Home" :home collapsed?]
         [nav-link "#/about" "About" :about collapsed?]
         [nav-link "#/dashboards/videos" "Videos" :videos-dashboard collapsed?]
         [nav-link "#/dashboards/images" "Images" :images-dashboard collapsed?]]
        [rui/app-bar {:title                         "puff-puff-dash"
                      :on-left-icon-button-touch-tap #(swap! collapsed? not)}]]])))

(defn about-page []
  [:div.content
   [:div.row
    [:div.col-md-12
     "this is the story of puff-puff-dash... work in progress"]]])

;; TODO maybe add embed in here?
(defn link-display [link]
  [:div
   [:pre (lq/clj->json link 2)]]) ;; TODO helpers namespace for this kind of thing

(defn link-item [{:keys [title url id domain source properties] :as link}]
  (let [class-domain (if domain
                       (string/replace domain #"\." "-")
                       "none")
        link-source  (case source
                       "reddit" (str source "/" (:subreddit properties))
                       source)]
    [rui/card
     [rui/card-header {:title                  title
                       :subtitle               link-source
                       :act-as-expander        true
                       :show-expandable-button true}]
     [rui/card-text {:expandable true}
      [link-display link]]
     [rui/card-actions {:expandable true}
      [rui/flat-button {:label    "Open"
                        :on-click #(.open js/window url "_blank")}]]]))

(defn query-input []
  [:div.query-input
   [rui/text-field
    {:rows                8
     :style               {:width "100%"}
     :multi-line          true
     :input-style         {:font-family :monospace}
     :floating-label-text "Query"
     :on-change           (fn [e]
                            (do
                              (reset! query-str (-> e .-target .-value))
                              (reset! query (lq/query->map @query-str))))
     :value               @query-str}]])

(defn query-buttons []
  [:div.query-buttons {:style {:text-align :center}}
   [rui/raised-button
    {:style    {:margin    10
                :min-width 100}
     :on-click (fn [_]
                 (reset! result
                         (lq/apply-query
                          @query
                          (session/get :links))))
     :label    "Evaluate"
     :primary  true}]
   [rui/raised-button
    {:style     {:margin    10
                 :min-width 100}
     :on-click  #'reset-all!
     :secondary true
     :label     "Reset"}]])

(defn query-display []
  [:div.query-display
   [:h3 "Parsed Query"]
   [:pre {:style {:height 300}}
    (lq/clj->json @query 2)]])

(defn query-matches []
  [:ul#matches
   (for [link @result]
     ^{:key (:id link)}
     [:li
      [:ul
       [:li [:strong "Title: "] (:title link)]
       [:li [:strong "Domain: "] (:domain link)]]])])

(defn query-container []
  [:div.query-container {:style {:display :inline}}
   [:h1 "Search"]
   [:div.row
    [:div {:style {:float         :left
                   :width         "45%"
                   :padding-right 20}}
     [query-input]
     [query-buttons]]]
   [:div
    [query-display]]])

(defn home-page []
  [rui/mui-theme-provider
   {:mui-theme (ui/get-mui-theme)}
   [:main.content
    [query-container]
    [:div.links-container
     [:h1 "All Links"]
     (if-not (empty? @result)
       [:div.links
        (for [link @result]
          ^{:key (:id link)}
          [link-item link])]
       [:span.error "No links haha"])]]])

(defn page-not-found []
  [:div.container
   [:span.error (str "Page " (name (session/get :page)) " not found")]])

(def pages
  {:home             #'home-page
   :about            #'about-page
   :videos-dashboard #'dashboards/videos-dashboard
   :images-dashboard #'dashboards/images-dashboard
   :not-found        #'page-not-found})

(defn page []
  [(or (pages (session/get :page))
       (:not-found pages))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/about" []
  (session/put! :page :about))

(secretary/defroute "/dashboards/:dashboard" {:as params}
  (session/put! :page (-> (:dashboard params)
                          (str "-dashboard")
                          keyword)))

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
;; Initialize app
(defn fetch-links! []
  (GET (str js/context "/links")
      {:params  {:limit 30}
       :handler (fn [response]
                  (if (get response "success")
                    (do
                      (session/put!
                       :links
                       (->> (get response "links")
                            (map helpers/keywordize-keys)
                            (map (fn [link]
                                   (assoc link :properties (-> link
                                                               :properties
                                                               helpers/from-json
                                                               helpers/keywordize-keys))))))
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
