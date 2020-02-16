(ns elk.dispatch
  (:require [re-frame.core :as re-frame]))

(def shift-enter
  "Test transducer. Is this the best way to multiplex? Etymologically I think
  this is more of a plexus than a multiplex."
  {:interpreter :cljs
   :code        '{:down (fn [state ev]
                          (cond
                            (= 13 (:keycode ev))
                            (when (:shift? state)
                              {:state {}
                               :emit  [:elk.components.code/new-block 0]})

                            (= 16 (:keycode ev)) {:state {:shift? true}}

                            :else nil))
                  :up (fn [_ ev]
                        (when (= 16 (:keycode ev))
                          {:state {}}))}})

(def network-topology
  {shift-enter {:elk.input/keydown :down
                :elk.input/keyup   :up}})

(def state-map (atom {}))

(defn transduction-runner [trans ev msg]

  (when (contains? trans ev)
    (let [{:keys [state emit]} ((get trans ev) (get @state-map trans) msg)]
      (when state
        (swap! state-map assoc trans state))
      (when emit
        (re-frame/dispatch emit)))))

(re-frame/reg-event-fx
 ::dispatch
 (fn [cofx [_ ev msg]]
   (transduction-runner shift-enter ev msg)
   ;; So here both trans and msg are messages. We need to vivify trans and then
   ;; apply it to msg
   ))
