(ns posthere.app
  "PostHere.io web application."
  (:gen-class)
    (:require [ring.middleware.reload :as reload]
              [compojure.core :refer (defroutes ANY)]
              [compojure.route :as route]
              [org.httpkit.server :refer (run-server)]
              [environ.core :refer (env)]))

(defonce hot-reload (or (env :hot-reload) false))

(defroutes routes
  (route/resources "/media/"))

(def app
  (if hot-reload
    (reload/wrap-reload routes)
    routes))

(defn start [port]
  (run-server app {:port port :join? false})
  (println (str "\nPostHere.io: Server running on port - " port ", hot-reload - " hot-reload)))

(defn -main []
  (start 3000))