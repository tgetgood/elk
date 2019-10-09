(ns elk.dev
  (:require [elk.routes :as routes]
            [taoensso.timbre :as log]))

(defmulti offline-dispatch :id)

(defmethod offline-dispatch :elk/verify-login
  [req]
  (routes/respond-with-fallback
   req [:elk/identity {:name "t" :orcid-id "xyzzy"}]))

(defmethod offline-dispatch :default
  [msg]
  (log/warn "Unhandled message:" (:id msg)))

(defmethod offline-dispatch :elk/search
  [{:keys [event] :as req}]
  (routes/respond-with-fallback
   req
   [:elk/search-response
    ;; REVIEW: This is coupling. A search result is something independent of the
    ;; component that displays it. This naming forces us to keep them bound.
    #:elk.components.search
    {:results [{:text "asdasd"}]
     :nonce   (:elk.components.search/nonce (second event))}]))

(defn set-offline-mode! []
  (alter-var-root #'elk.server/dispatch-fn (constantly offline-dispatch)))
