(ns elk.input
  (:require [elk.dispatch :as d]
            [re-frame.core :as re-frame]))

(defn strip-key-event
  "Only needed because react insists on recycling event objects."
  [ev]
  {:key     (.-key ev)
   :code    (.-code ev)
   :keycode (.-keyCode ev)
   :shift?  (.-shiftKey ev)
   :alt?    (.-altKey ev)
   :ctrl?   (.-ctrlKey ev)
   :inst    (js/Date.)})

(defn prev-and [f]
  (fn [ev]
    (.preventDefault ev)
    (.stopPropagation ev)
    (f ev)))

(defn nerf-browser!
  "You can come if you want to, but you can never leave."
  []
  (set! js/window.onkeydown
        (prev-and
         (fn [ev]
           (let [key-data (strip-key-event ev)]
             (re-frame/dispatch [::d/dispatch ::keydown key-data])))))

  (set! js/window.onkeyup
        (prev-and
         (fn [ev]
           (let [key-data (strip-key-event ev)]
             (re-frame/dispatch [::d/dispatch ::keyup key-data])))))

  (set! js/window.onscroll
        (prev-and
         (fn [ev]
           (.log js/console "scroll!!!")))))

  ;;TODO: I'll need to override mouse and touch events eventually...
