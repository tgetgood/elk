(ns ^:figwheel-hooks elk.core
  (:require [cljs.js :as cljs]
            [elk.components.code :as code]
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
  (re-frame/dispatch [:send [:elk/demo-doc]])

  (reagent/render [window/main [code/editor-page]]
                  (.getElementById js/document "app")))

;; TODO: Remove this from the main project. I want to be able to compile the
;; core of the project and any page to optimised js and only bring in the
;; compiler when needed (and as a separate load at runtime).
(defn- eval-setup! []
  ;; FIXME: This is easy because it sets up a global eval, but it's problematic
  ;; because I want to isolate interpreters from each other; i.e. have separate
  ;; compiler states. Do I need unique namespaces too?
  (set! cljs.js/*eval-fn* cljs.js/js-eval)
  (set! *ns* (create-ns (gensym)))
  (set! cljs.env/*compiler* (cljs.js/empty-state)))

(defn ^:export init []
  (eval-setup!)
  (re-frame/dispatch-sync [::events/initialise-db])
  (dev-setup)
  (mount-root))
