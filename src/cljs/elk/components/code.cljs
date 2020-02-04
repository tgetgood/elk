(ns elk.components.code
 (:require [cljsjs.codemirror]
           [cljsjs.codemirror.mode.clojure]
           [cljs.js :as cljs]
           [cljs.reader :as r]
           [hasch.core :as h]
           [reagent.core :as reagent]
           [re-frame.core :as re-frame]))

(defn fake-load-fn
  "No op loader"
  [_ cb]
  (cb {:lang   :js
       :source ""}))

(def compiler-opts
  {:eval cljs/js-eval
   :load fake-load-fn})

(def out (atom nil))

(defn ev [form]
  (binding [cljs.js/*eval-fn* cljs.js/js-eval
            *ns* (create-ns (gensym))
            cljs.env/*compiler* (cljs.js/empty-state)]
    (eval form)))


(re-frame/reg-event-db
 ::edit
 (fn [db [_ id text]]
   (assoc-in db [::codeblocks id] text)))

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
 ::unfocus
 (fn [{:keys [db]} [_ id]]
   (try
     (let [text (get-in db [::codeblocks id])
           [v h body] (interpret text)]
       (println (ev (cljs.reader/read-string text) )))
     (catch js/Error e
       (do
         (println e)
         {:dispatch [::read-error id]})))))

(re-frame/reg-sub
 ::codeblock
 (fn [db [_ id]]
   (or (get-in db [::codeblocks id]) "")))

(defn codearea [{:keys [id]}]
  (let [editor  (atom nil)
        content @(re-frame/subscribe [::codeblock id])]
    (reagent/create-class
     {:reagent-render      (fn [] [:div])
      :component-did-mount (fn [this]
                             (reset! editor
                                     (js/CodeMirror (reagent/dom-node this)
                                                    (clj->js
                                                     {:mode        :clojure
                                                      :lineNumbers false
                                                      :value       content})))
                             (.on @editor "change"
                                  (fn [ed change]
                                    (re-frame/dispatch
                                     [::edit id (.getValue ed)])))
                             (.on @editor "blur"
                                  (fn [ed]
                                    (re-frame/dispatch
                                     [::unfocus id]))))})))

(defn unit [{:keys [id] :as opts}]
  [:div.column
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
