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

(defn template-for [name] (str "../resources/html/" name))

; Results page work
(deftemplate results-page (template-for "results.html") [] 
  [:head :title] (html-content (str "POSThere.io - Results")))

(defn- results-view []
  (apply str (results-page)))
; Results page work

; POST results
(defn- post-results [uuid, status]
  (try
    (str "Post from " uuid " for status " status)
    (catch Exception e (str "caught exception: " (.getMessage e)))))
; POST results

(defroutes approutes
  ; GET requests
  (GET "/:uuid" [uuid] (results-view))
  (GET "/:status/:uuid" [status, uuid] (results-view))
  (GET "/" [] (str "Home From Nginx"))

  ; POST requests
  (POST "/:uuid" [uuid] (post-results uuid 200))
  (POST "/:status/:uuid" [status, uuid] (post-results uuid status))

  ; Standard requests
  (route/resources "/assets/")
  (route/not-found "Page Not Found"))

(def app
  (if hot-reload
    (reload/wrap-reload approutes)
    approutes))

(defn start [port]
  (run-server app {:port port :join? false})
  (println (str "\nPOSThere.io: Server running on port - " port ", hot-reload - " hot-reload)))

(defn -main []
  (start 3000))