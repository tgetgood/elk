(ns elk.components.code
 (:require [cljsjs.codemirror]
           [cljsjs.codemirror.mode.clojure]
           [cljs.js :as cljs]
           [cljs.reader :as r]
           [hasch.core :as h]
           [reagent.core :as reagent]
           [re-frame.core :as re-frame]))

(re-frame/reg-event-db
 ::edit
 (fn [db [_ id text]]
   (assoc-in db [::codeblocks id :text] text)))

(defn resolve-binding [x] x)

(defn interpret [text]
  ;; It's either a def, a defn, or some other form. If def of defn we have a
  ;; desired name, Otherwise the name needs to be added separately. Is there any
  ;; reason to go by def / defn? I really like Maria's method of seemless
  ;; forms. If we could let people type as they will and automatically infer
  ;; names / extract bodies as you go, that would be ideal.
  (let [form (cljs.reader/read-string text)]
    ;; TODO: parse this via spec. I think that's a winning use case.
    (if (and (list? form) (= (first form) 'def))
      (let [v    (second form)
            body (first (drop 2 form))]
        [(resolve-binding v) (h/b64-hash body) body]))))

(re-frame/reg-event-fx
 ::new-block
 (fn [cofx [_ pre]]
   (let [s  (->> (:db cofx)
                 ::codeblocks
                 vals
                 (sort-by :index)
                 (drop-while #(not= (:index %) (:index pre)))
                 (take 2)
                 (map :index))
         i  (if (= 1 (count s))
              (inc (first s))
              (/ (reduce + s) 2))
         id (gensym)]
     {:db (update (:db cofx) ::codeblocks assoc id {:id id :index i :text ""})
      ;; HACK: What a kludge
      :dispatch-later [{:ms 100 :dispatch [::refocus id]}]})))

(re-frame/reg-event-fx
 ::refocus
 (fn [_ id]
   {::focus id}))

(re-frame/reg-fx
 ::focus
 (fn [[_ id]]
   (println id (.getElementById js/document id))
   (-> (.getElementById js/document id)
    .-childNodes
    (aget 0)
    .-CodeMirror
    .focus)))

(re-frame/reg-event-fx
 ::unfocus
 (fn [{:keys [db]} [_ id]]
   (try
     (let [text (get-in db [::codeblocks id])
           [v h body] (interpret text)])
     (catch js/Error e
       (do
         (println e)
         {:dispatch [::read-error id]})))))

(re-frame/reg-sub
 ::content
 (fn [db [_ id]]
   (get-in db [::codeblocks id :text])))

(re-frame/reg-sub
 ::codeblocks
 (fn [db]
   (::codeblocks db)))

(defn codearea [{:keys [id] :as opts}]
  (let [editor  (atom nil)
        content @(re-frame/subscribe [::content id])]
    (reagent/create-class
     {:reagent-render      (fn [] [:div
                                   {:id    id
                                    :style {:border        "0.1rem"
                                            :height        :max-content
                                            :margin-top    "1rem"
                                            :padding       "0.5rem"
                                            :border-style  "outset"
                                            :border-color  "rgba(100,100,100,0.1)"
                                            :border-radius "0.5rem"}}])
      :component-did-mount (fn [this]
                             (reset! editor
                                     (js/CodeMirror (reagent/dom-node this)
                                                    (clj->js
                                                     {:mode        :clojure
                                                      :lineNumbers false
                                                      :value       content})))
                             (.on @editor "keypress"
                                  (fn [ed ev]
                                    (when (and (.-shiftKey ev)
                                               (= (.-key ev) "Enter"))
                                      (.preventDefault ev)
                                      (.stopPropagation ev)
                                      (re-frame/dispatch [::new-block opts]))))
                             (.on @editor "change"
                                  (fn [ed change]
                                    (re-frame/dispatch
                                     [::edit id (.getValue ed)])))
                             (.on @editor "blur"
                                  (fn [ed]
                                    (re-frame/dispatch
                                     [::unfocus id]))))})))

(defn unit [{:keys [id] :as opts}]
  [:div.column {:key id
                :style {:width :max-content
                        :max-width "39rem"
                        :height :max-content
                        :margin-bottom "1rem"}}
   [:div.row
    (let [id (gensym)]
      [:div
       [:label {:for id} "name"]
       [:input {:type :text
                :id id}]])
    (let [id (gensym)]
      [:div
       [:label {:for id} "metadata"]
       [:input {:id id :type :text}]])]
   [codearea opts]])

(defn editor-page []
  (let [blocks @(re-frame/subscribe [::codeblocks])]
    [:div.column
     (map unit (sort-by :index (vals blocks)))]))
