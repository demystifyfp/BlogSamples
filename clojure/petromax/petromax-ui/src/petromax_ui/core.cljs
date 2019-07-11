(ns petromax-ui.core
  (:require [reagent.core :as reagent :refer [atom]]))

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))

(defn play-data [& names]
  (for [n names
        i (range 20)]
    {:time i :item n :quantity (+ (Math/pow (* i (count n)) 0.8) (rand-int (count n)))}))

(defn hello-world []
  [:div
   [:h1 (:text @app-state)]
   [:vega-lite {:data {:values (play-data "monkey" "slipper" "broom")}
                :encoding {:x {:field "time"}
                           :y {:field "quantity"}
                           :color {:field "item" :type "nominal"}}
                :mark "line"}]])

(defn start []
  (reagent/render-component [hello-world]
                            (. js/document (getElementById "app"))))

(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))
