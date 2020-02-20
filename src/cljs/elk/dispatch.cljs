(ns elk.dispatch
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-event-db
 ::listen
 (fn [db [_ ev redirect]]
   (update-in db [::handler-redirects ev]
              #(if %
                 (conj % redirect)
                 #{redirect}))))

(re-frame/reg-event-db
 ::drop
 (fn [db [_ ev redirect]]
   (update-in db [::handler-redirects ev] disj redirect)))

(re-frame/reg-event-fx
 ::dispatch
 (fn [cofx [_ ev & args]]
   (println ev args)
   (let [redirects (get-in cofx [:db ::handler-redirects ev])
         events (mapv (fn [ev] (into [ev] args)) redirects)]
     {:dispatch-n events})))

(defn vivifier
  "Creates and returns a runtime in which transduction topologies can be
  created."
  []
  (let [network-state (atom {})]
    (fn [{:keys [names network outputs] :as in}]
      ;; Creating a network from a topology is a side effect. The new network is
      ;; composed of pure transducers, but the fact that it can interarct with
      ;; the outside world prohibits it being a result (data). We must leave the
      ;; medium of computation to build a computer.
      (let [transducers (into {} (map (fn [[k v]] [k (eval (:code v))])) names)]
        ;; Hoboy...
        (run!
         (fn [[xform wire-map]]
           (let [uniq   (keyword (gensym))
                 uxform (keyword uniq xform)]
             (run!
              (fn [[from to]]
                (let [uto (keyword uniq to)]
                  (re-frame/dispatch [::listen from uto])
                  (re-frame/reg-event-fx
                   uto
                   (fn [_ [_ msg]]
                     (let [{:keys [emit emit-n state]}
                           ((get-in transducers [xform to])
                            (get @network-state xform)
                            msg)]
                       (when state
                         (swap! network-state assoc xform state))
                       (if emit-n
                         {:dispatch-n (mapv (fn [x] [::dispatch xform x]) emit-n)}
                         (when emit
                           {:dispatch [::dispatch xform emit]})))))))
              wire-map)))
         network)

        (run! (fn [[from to]]
                (re-frame/dispatch [::listen from to]))
              outputs)))))

(def shift-enter
  "Test transducer. Is this the best way to multiplex? Etymologically I think
  this is more of a plexus than a multiplex."
  {:interpreter :cljs
   :code        '{:down (fn [state ev]
                          (cond
                            (= 13 (:keycode ev))
                            (when (:shift? state)
                              {:state (-> state
                                          (dissoc :shift?)
                                          (update :index inc))
                               :emit (:index state)})

                            (= 16 (:keycode ev)) {:state (assoc state :shift? true)}

                            :else nil))
                  :up (fn [state ev]
                        (when (= 16 (:keycode ev))
                          {:state (dissoc state :shift?)}))}})

(def doc-aggregator
  "The thing which emits a document to be rendered (sent to react)"
  {:interpreter :cljs

   :code '{:init        (fn [state initial]
                          {:state initial
                           :emit  initial})
           :shift-enter (fn [state initial]
                          (let [id   (gensym)
                                next (update state :codeblocks assoc
                                             id {:id    id
                                                 :index 0
                                                 :text  ""})]
                            {:state next
                             :emit  next}))}})

(def html-renderer
  {:interpreter :cljs

   :code '{:render (fn [_ doc]
                     {:emit doc})}})

;;TODO: The running network isn't being destroyed on reload.
(def network-topology
  {:outputs {:re-frame-render :elk.components.code/codeblocks}
   :names   {:shift-enter     shift-enter
             :doc-aggregator  doc-aggregator
             :re-frame-render html-renderer}
   :network {:shift-enter     {:elk.input/keydown :down
                               :elk.input/keyup   :up}
             :doc-aggregator  {:shift-enter          :shift-enter
                               :elk.core/initial-doc :init}
             :re-frame-render {:doc-aggregator :render}}})

(re-frame/reg-event-fx
 ::start-initial-nexus
 (fn [{:keys [db]} _]
   {:db      (assoc db ::tlv (vivifier))
    ::vivify network-topology}))

(re-frame/reg-sub
 ::tlv
 (fn [db]
   (::tlv db)))

(re-frame/reg-fx
 ::vivify
 (fn [topo]
   (let [tlv @(re-frame/subscribe [::tlv])]
     (tlv topo))))
