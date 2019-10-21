(ns ^:figwheel-hooks elk.core
  (:require [elk.components.code :as code]
            [elk.components.window :as window]
            [elk.config :as config]
            [elk.events :as events]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [taoensso.timbre :as log]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (log/info "dev mode")))

(defn ^:after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [window/main [code/unit {:id :a}]]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialise-db])
  (dev-setup)
  (mount-root))
