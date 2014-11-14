(ns posthere.app
  "PostHere.io web application."
  (:gen-class)
    (:use [net.cgrand.enlive-html])
    (:require [ring.middleware.reload :as reload]
              [compojure.core :refer :all]
              [compojure.route :as route]
              [org.httpkit.server :refer (run-server)]
              [environ.core :refer (env)]))

(defonce hot-reload (or (env :hot-reload) false))

; Should we move this out?  The only real modification will ever happen
; for the results page, but it might be cleaner...
(deftemplate results-page "posthere/templates/results.html" [] 
  [:head :title] (html-content (str "POSThere.io - Results")))

(defn- results-view []
  (apply str (results-page)))
; Results page work

(defroutes approutes
  (GET "/:uuid" [uuid] (results-view))
  (GET "/:status/:uuid" [status, uuid] (str "Foo"))
  (GET "/" [] (str "Home"))
  (route/resources "/assets/")
  (route/not-found "Page Not Found"))

(def app
  (if hot-reload
    (reload/wrap-reload approutes)
    approutes))

(defn start [port]
  (run-server app {:port port :join? false})
  (println (str "\nPostHere.io: Server running on port - " port ", hot-reload - " hot-reload)))

(defn -main []
  (start 3000))