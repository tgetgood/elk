(ns elk.components.code
 (:require [cljsjs.codemirror]
           [cljsjs.codemirror.mode.clojure]
           [cljs.js :as cljs]
           [cljs.reader :as r]
           [hasch.core :as h]
           [reagent.core :as reagent]
           [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
 ::new-block
 (fn [cofx [_ i]]
   (let [id (gensym)]
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
