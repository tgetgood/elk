(ns elk.components.window
  (:require [elk.events :as events]
            [re-frame.core :as re-frame]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Page Level
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;; Login

(re-frame/reg-fx
 ::nav-out
 (fn [url]
   (-> js/document
       .-location
       (set! url))))

(defn main [content]
  [:div.padded
   [:div.vspacer]
   [content]])

(defn hi []
  [:div "hell0!"])
