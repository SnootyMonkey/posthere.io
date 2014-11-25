(ns posthere.app
  "PostHere.io web application."
  (:gen-class)
    (:require [ring.middleware.reload :as reload]
              [compojure.core :refer :all]
              [compojure.route :as route]
              [org.httpkit.server :refer (run-server)]
              [environ.core :refer (env)]
              [posthere.capture-request :refer (capture-request)]
              [posthere.results :refer (results-view)]))

(defonce hot-reload (or (env :hot-reload) false))

(defroutes approutes
  
  ; GET requests
  (GET "/" [] (str "Egads! How did you get here?")) ; Should be handled by nginx
  (GET "/:uuid" [uuid] (results-view uuid)) ; Show them the results of their requests
  
  ; POST requests
  (POST "/:uuid" [uuid :as request] (capture-request uuid request)) ; Capture their POST request

  ; Resource requests (in development only, otherwise handled by nginx)
  (route/resources "/"))

(def app
  (if hot-reload
    (reload/wrap-reload #'approutes)
    approutes))

(defn start [port]
  (run-server app {:port port :join? false})
  (println (str "\nPOSThere.io: Server running on port - " port ", hot-reload - " hot-reload)))

(defn -main []
  (start 3000))