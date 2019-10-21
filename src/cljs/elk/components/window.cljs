(ns elk.components.window
  (:require [elk.events :as events]
            [re-frame.core :as re-frame]))

(defn main [content]
  [:div {:style {:padding "1rem"}}
   content])
