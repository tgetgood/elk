(ns elk.input
  (:require [re-frame.core :as re-frame]))

;;;;; Events

(defn t []
  (js/Date.now))

(re-frame/reg-event-fx
 ::keydown
 (fn [{:keys [db]} [_ ev]]
   ;; TODO: scan for held keys
   {:db (update db ::keys assoc (:keycode ev) (assoc ev :start-time (t)))}))

(re-frame/reg-event-fx
 ::keyup
 (fn [{:keys [db]} [_ ev]]
   (let [s (-> db ::keys (:keycode ev) :start-time)
         ev (assoc ev :start-time s :end-time (t))]
     (merge
      {:db (update db ::keys dissoc (:keycode ev))}
      (when (not (contains? #{"Alt" "Shift" "Control"} (:key ev)))
        {:dispatch [:elk.dispatch/dispatch ::keypress ev]})))))

;;;;; Handler Overrides

(defn strip-key-event
  "Only needed because react insists on recycling event objects."
  [ev]
  {:key     (.-key ev)
   :keycode (.-keyCode ev)
   :shift?  (.-shiftKey ev)
   :alt?    (.-altKey ev)
   :ctrl?   (.-ctrlKey ev)})

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
             (re-frame/dispatch [::keydown key-data])))))

  (set! js/window.onkeyup
        (prev-and
         (fn [ev]
           (let [key-data (strip-key-event ev)]
             (re-frame/dispatch [::keyup key-data])))))

  (set! js/window.onscroll
        (prev-and
         (fn [ev]
           (.log js/console "scroll!!!"))))
  ;;TODO: I'll need to override mouse and touch events eventually...
  )
