(ns posthere.app
  "PostHere.io web application."
  (:gen-class)
    (:require [ring.middleware.reload :as reload]
              [compojure.core :refer :all]
              [compojure.route :as route]
              [org.httpkit.server :refer (run-server)]
              [environ.core :refer (env)]
              [posthere.capture-request :refer (capture-request)]
              [posthere.storage :refer (requests-for)]
              [posthere.results :refer (results-view)]))

(defonce hot-reload (or (env :hot-reload) false))

(defroutes approutes

  ; GET requests
  (GET "/" [] (slurp "./resources/public/index")) ; This is for development, should be handled by nginx in production
  ;; TODO update the timestamps from the example so they won't grow old
  (GET "/_/example" [] (results-view (read-string (slurp "example.edn")) "_/example"))
  (GET "/:uuid" [uuid] (results-view (requests-for uuid) uuid)) ; Show them the results of their requests

  ; POST requests
  ;; TODO return error status if they try to POST to the example
  ;;(POST "/_/example" [] ()) ; return a status and a message
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