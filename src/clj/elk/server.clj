(ns elk.server
  (:require [clojure.java.io :as io]
            [compojure.core :as c]
            [compojure.route :as route]
            [elk.env :as env]
            [org.httpkit.server :as http]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [taoensso.timbre :as log]))

;;;;; Routes

(def index
  "The SP in SPA"
  (io/resource "public/index.html"))

(c/defroutes routes
  (route/resources "/")
  (c/GET "*" req index))

(def app
  (-> routes
      (wrap-defaults
       (-> site-defaults
           (assoc-in [:session :cookie-attrs :same-site] :lax)))))

;;;;; Main server

(defonce ^:private stop-server! (atom nil))

(defn start-server! []
  (when (fn? @stop-server!)
    (@stop-server!))
  (let [port (env/port)]
    (reset! stop-server! (http/run-server #'app {:port port}))
    (log/info "Server listening on port:" port)))

(defn init! []
  (start-server!))

(defn stop! []
  (when @stop-server!
    (@stop-server!)))

(defn -main [& args]
  (try
    (init!)
    (catch Exception e
      (.printStackTrace e))))
