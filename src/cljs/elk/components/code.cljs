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

(defn interpret
  "Docstring"
  [text]
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
   (-> (.getElementById js/document id)
    .focus)))

(re-frame/reg-sub
 ::content
 (fn [db [_ id]]
   (get-in db [::codeblocks id :text])))

(re-frame/reg-sub
 ::codeblocks
 (fn [db]
   (::codeblocks db)))

(defn buffer-text [{:keys [id] :as opts}]
  [:div
   {:id              id
    :contentEditable "true"
    :style           {:overflow      :hidden
                      :width         :max-content
                      :max-width     "39rem"
                      :min-height    "1rem"
                      :padding       "0.5rem"
                      :border        "0.1rem inset"
                      :border-color  "rgba(100,100,100,0.1)"
                      :border-radius "0.5rem"}}])

(defn buffer [opts]
  [:div {:key (:id opts)
         :style {:margin-bottom "0.5rem"}}
   [buffer-text opts]])

(defn editor-page
  "This is the window, basically."
  []
  (let [blocks @(re-frame/subscribe [::codeblocks])]
    [:div {:style        {:width   "100%"
                          :height  "100%"}}
     [:div {:style {:padding "1rem"}}
      [:div.column
       (map buffer (sort-by :index (vals blocks)))]]]))
