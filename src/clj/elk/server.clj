(ns elk.server
  (:require clj-http.client
            [clojure.java.io :as io]
            [compojure.core :as c]
            [compojure.route :as route]
            [elk.env :as env]
            [elk.routes :as routes]
            [org.httpkit.server :as http]
            ring.middleware.anti-forgery
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :as sente-http-kit]
            [taoensso.timbre :as log]))

;;;;; Websocket server

(defonce socket
  (sente/make-channel-socket-server!
   (sente-http-kit/get-sch-adapter)
   {:user-id-fn (fn [req]
                  (-> req
                      :oauth2/access-tokens
                      :orcid
                      :orcid-id))}))

;;;;; Routes

(def index
  "The SP in SPA"
  (io/resource "public/index.html"))

(c/defroutes routes
  (route/resources "/")
  (c/GET "/elmyr" req (force ring.middleware.anti-forgery/*anti-forgery-token*))
  (c/GET "/chsk" req ((:ajax-get-or-ws-handshake-fn socket) req))
  (c/POST "/chsk" req ((:ajax-post-fn socket) req))

  ;; REVIEW: defer to the SPA code to route everything else. Are there any
  ;; problems with this? Particularly regarding security?
  (c/GET "*" req index))

(def app
  (-> routes
      (wrap-defaults
       (-> site-defaults
           (assoc-in [:session :cookie-attrs :same-site] :lax)))))

;;;;; Main server

(defonce ^:private stop-server! (atom nil))
(defonce ^:private router (atom nil))

(defn stop-router! []
  (when (fn? @router)
    (@router)))

(defn clean-req [msg]
  (select-keys msg [:event :id :?reply-fn :send-fn :client-id :uid]))

(def dispatch-fn
  routes/dispatch)

(defn dispatch-msg [msg]
  (-> msg clean-req dispatch-fn))

(defn start-router! []
  (stop-router!)
  (reset! router
          (sente/start-server-chsk-router! (:ch-recv socket) #'dispatch-msg)))


(defn start-server! []
  (when (fn? @stop-server!)
    (@stop-server!))
  (let [port (env/port)]
    (reset! stop-server! (http/run-server #'app {:port port}))
    (log/info "Server listening on port:" port)))

(defn init! []
  (start-server!)
  (start-router!))

(defn stop! []
  (stop-router!)
  (when @stop-server!
    (@stop-server!)))

(defn -main [& args]
  (try
    (init!)
    (catch Exception e
      (.printStackTrace e))))
