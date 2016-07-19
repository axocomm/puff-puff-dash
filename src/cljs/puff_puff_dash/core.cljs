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
            [puff-puff-dash.helpers :as helpers]
            [cljs-react-material-ui.core :as ui]
            [cljs-react-material-ui.reagent :as rui]
            [cljs-react-material-ui.icons :as ic]
            [clojure.walk :refer [keywordize-keys]])
  (:import goog.History))

(declare fetch-links!)

(def query-str (r/atom nil))
(def params (r/atom {}))
(def current-page (r/atom 1))
(def more-links? (r/atom true))
(def link-details (r/atom {}))

(def page-size 20)

(defn reset-all! []
  (do
    (reset! params {})
    (reset! current-page 1)
    (reset! query-str "")
    (reset! link-details {})
    (reset! more-links? true)))

(defn load-link-details!
  "For now, just load link tags into `link-details'."
  [id]
  (GET (str js/context "/links/" id "/tags")
      {:handler (fn [response]
                  (if (get response "success")
                    (swap! link-details assoc id {:tags (get response "tags")})
                    (.log js/console (str "Could not get link details: " (or (get response "error")
                                                                             "unknown error")))))}))

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
       [:div.navbar {:style {:position :fixed
                             :width    "100%"
                             :z-index  999}}
        [rui/drawer
         {:open              (not @collapsed?)
          :docked            false
          :on-request-change #(reset! collapsed? (not %))}
         [nav-link "#/" "Home" :home collapsed?]
         [nav-link "#/about" "About" :about collapsed?]]
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

(defn link-tags [tags]
  [:div.link-tags
   (str "Tags: " (string/join ", " tags))])

(defn link-item [{:keys [title url id domain source properties tags] :as link}]
  (let [class-domain (if domain
                       (string/replace domain #"\." "-")
                       "none")
        link-source  (case source
                       "reddit" (str source "/" (:subreddit properties))
                       source)]
    [rui/card {:on-expand-change (fn [_]
                                   (when-not (contains? @link-details id)
                                     (load-link-details! id)))}
     [rui/card-header {:title                  title
                       :subtitle               link-source
                       :act-as-expander        true
                       :show-expandable-button true}]
     [rui/card-text {:expandable true}
      [link-display link]]
     [rui/card-actions {:expandable true}
      [rui/flat-button {:label    "Open"
                        :on-click #(.open js/window url "_blank")}]
      [rui/flat-button {:label "Dead"
                        :style {:color "#a00"}}]
      (when-let [details (get @link-details id)]
        [link-tags (:tags details)])]]))

(defn query-input []
  [:div.query-input
   [rui/text-field
    {:rows                8
     :style               {:width "100%"}
     :multi-line          true
     :input-style         {:font-family "Roboto Mono, monospace"}
     :floating-label-text "Query"
     :on-change           (fn [e]
                            (do
                              (reset! query-str (-> e .-target .-value))
                              (reset! params (lq/query->map @query-str))))
     :value               @query-str}]])

(defn query-buttons []
  [:div.query-buttons {:style {:text-align :center}}
   [rui/raised-button
    {:style    {:margin    10
                :min-width 100}
     :on-click (fn [_]
                 (.log js/console (str @params))
                 (when-not (:error @params)
                   (reset! current-page 1)
                   (reset! more-links? true)
                   (fetch-links!)))
     :label    "Evaluate"
     :primary  true}]
   [rui/raised-button
    {:style     {:margin    10
                 :min-width 100}
     :on-click  (fn [_]
                  (reset-all!)
                  (fetch-links!))
     :secondary true
     :label     "Reset"}]])

(defn query-display []
  [:div.query-display
   [rui/tabs
    [rui/tab {:label "JSON"}
     [:pre {:style {:height 300}}
      (lq/clj->json @params 2)]]
    [rui/tab {:label "EDN"}
     [:pre {:style {:height 300}}
      (with-out-str (cljs.pprint/pprint @params))]]]])

(defn query-container []
  [:div.query-container {:style {:display :inline}}
   [:h1 "Search"]
   [:div.row
    [:div {:style {:float         :left
                   :width         "45%"
                   :padding-right 20}}
     [query-input]
     [query-buttons]]]
   [:div {:style {:width "45%"
                  :float :right}}
    [query-display]]])

(defn home-page []
  [rui/mui-theme-provider
   {:mui-theme (ui/get-mui-theme)}
   [:main.content
    [query-container]
    [:div.links-container {:style {:clear :both}}
     [:h1 "All Links"]
     (if-not (empty? (session/get :links))
       [:div.links
        (for [link (session/get :links)]
          ^{:key (:id link)}
          [link-item link])
        [rui/raised-button {:label      "Next Page"
                            :full-width true
                            :style      {:margin-top    20
                                         :margin-bottom 20}
                            :disabled   (not @more-links?)
                            :on-click   (fn [_]
                                          (swap! current-page inc)
                                          (.log js/console (str @current-page))
                                          (fetch-links!))}]]
       [:span.error "No links haha"])]]])

(defn page-not-found []
  [:div.container
   [:span.error (str "Page " (name (session/get :page)) " not found")]])

(def pages
  {:home      #'home-page
   :about     #'about-page
   :not-found #'page-not-found})

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
  (POST (str js/context "/links")
      {:params  (merge {:limit  page-size
                        :offset (* page-size (dec @current-page))}
                       @params)
       :format  :json
       :handler (fn [response]
                  (if (get response "success")
                    (do
                      (let [links (->> (get response "links")
                                       (map keywordize-keys))]
                        (if (= @current-page 1)
                          (session/put! :links links)
                          (session/put!
                           :links
                           (concat (session/get :links) links)))
                        (when (< (count links) page-size)
                          reset! more-links? false)))
                    (.log js/console (get response "error"))))}))

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (load-interceptors!)
  (fetch-links!)
  (hook-browser-navigation!)
  (mount-components))
