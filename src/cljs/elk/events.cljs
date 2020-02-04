(ns elk.events
  (:require [cljs.core.async :as async]
            [cljs.spec.alpha :as s]
            [clojure.edn :as edn]
            goog.net.XhrIo
            [elk.db :as db]
            [re-frame.core :as re-frame]
            [taoensso.sente :as sente]
            [taoensso.timbre :as log])
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Connection management
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; This is the external API call
(re-frame/reg-event-fx
 :send
 (fn [{{:keys [chsk]} :db} [_ ev]]
   (if (and (satisfies? IDeref (:state chsk))
            (:open? @(:state chsk)))
     {::send! {:ev ev :send-fn (:send-fn chsk)}}
     {::connect-chsk! true
      :dispatch       [::enqueue-request ev]})))

(re-frame/reg-event-db
 ::enqueue-request
 (fn [db [_ ev]]
   (update db :request-queue conj ev)))

(re-frame/reg-fx
 ::send!
 (fn [{:keys [send-fn ev]}]
   (send-fn ev 10000 re-frame/dispatch)))

(re-frame/reg-event-fx
 ::server-connection
 (fn [{:keys [db]} [_ chsk]]
   (let [pending (:request-queue db)]
     (merge {:db (assoc db :chsk chsk :request-queue [] :connecting? false)
             :dispatch-n
             (into [[::login-check]]
                   (when (seq pending)
                     (map (fn [ev] [:send ev]) pending)))}))))

(let [connecting? (atom false)]
  (re-frame/reg-fx
   ::connect-chsk!
   (fn [_]
     (when-not @connecting?
       (reset! connecting? true)
       (log/info "Connecting to server...")
       (let [csrf-ch (async/promise-chan)]
         (goog.net.XhrIo/send "/elmyr"
                              (fn [e]
                                (->> e
                                     .-target
                                     .getResponseText
                                     (async/put! csrf-ch))))
         ;; TODO: timeout, retry, backoff.
         (go
           (let [token (async/<! csrf-ch)
                 chsk  (sente/make-channel-socket-client!
                        "/chsk" token {:type :auto})]
             ;; Wait for a message so that we know the channel is open.
             (async/<! (:ch-recv chsk))
             (reset! connecting? false)
             (sente/start-client-chsk-router! (:ch-recv chsk)
              (fn [message]
                (re-frame/dispatch (:event message))))
             (re-frame/dispatch-sync [::server-connection chsk]))))))))

;;;;; Sente internal events.

(re-frame/reg-event-fx
 :chsk/handshake
 (fn [_ _]))

(re-frame/reg-event-fx
 :chsk/state
 (fn [_ _]))

(re-frame/reg-event-fx
 :chsk/ping
 (fn [_ _]))

(re-frame/reg-event-fx
 :chsk/timeout
 (fn [_ _]
   (log/warn "Server websocker connection timed out.")))

(re-frame/reg-event-fx
 :chsk/recv
 (fn [_ e]
   (log/error "Received broadcast message from server"
              e
              "Broadcast support is not currently implemented.")))

;; HACK:
(re-frame/reg-event-db
 :elk/demo-doc
 (fn [db [_ doc]]
   (assoc db ::demo doc)))
