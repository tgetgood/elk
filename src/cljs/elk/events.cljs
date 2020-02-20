(ns elk.events
  (:require [cljs.core.async :as async]
            [cljs.spec.alpha :as s]
            [clojure.edn :as edn]
            goog.net.XhrIo
            [elk.db :as db]
            [re-frame.core :as re-frame])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Other
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-db
 ::initialise-db
 (fn [_ _]
   db/default-db))

;;;;; sessionStorage

(re-frame/reg-fx
 :storage/set
 (fn [[k v]]
   (.setItem (.-sessionStorage js/window) (str k) (str v))))

(re-frame/reg-fx
 :storage/remove
 (fn [k]
   (.removeItem (.-sessionStorage js/window) (str k))))

(re-frame/reg-cofx
 :storage/get
 (fn [cofx k]
   (assoc cofx
          :storage/get
          (-> js/window
              .-sessionStorage
              (.getItem (str k))
              edn/read-string))))

;; HACK:
(re-frame/reg-event-db
 :elk/demo-doc
 (fn [db [_ doc]]
   (assoc db ::demo doc)))
