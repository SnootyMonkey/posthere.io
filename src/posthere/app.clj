(ns posthere.app
  "PostHere.io web application."
  (:gen-class)
    (:require [clojure.string :as s]
              [ring.middleware.reload :as reload]
              [ring.util.response :refer (response status)]
              [compojure.core :refer (GET POST defroutes)]
              [compojure.route :as route]
              [org.httpkit.server :refer (run-server)]
              [environ.core :refer (env)]
              [posthere.capture-request :refer (capture-request)]
              [posthere.storage :refer (requests-for)]
              [posthere.examples :as examples :refer (example-results)]
              [posthere.results :refer (results-view)]))

(defonce hot-reload (or (env :hot-reload) false))

(defn- strip-prefix-slash [relative-url]
  (s/replace relative-url #"^\/" ""))

(defn- uuid-for [request]
  "Extract everything after the initial / in the request URL as the UUID, not including the /."
  (strip-prefix-slash (get-in request [:route-params :*])))

(defroutes approutes

  ; Resource requests (in development only, otherwise handled by nginx)
  (route/resources "/")
  
  ;; Home page for development, handled by nginx in production
  (GET "/" [] (slurp "./resources/public/index"))
  ;; Show canned results as an example 
  (GET examples/example-url [:as request] (results-view (example-results) (strip-prefix-slash examples/example-url)))
  ;; Show them the results stored from their requests
  (GET "*" [:as request] (let [uuid (uuid-for request)] (results-view (requests-for uuid) uuid)))

  ;; Error if POST is to the example URL
  (POST examples/example-url [] (status (response examples/post-to-example-body) examples/post-to-example-status))
  ;; Capture their POST request
  (POST "*" [:as request] (capture-request (uuid-for request) request)))

(def app
  (if hot-reload
    (reload/wrap-reload #'approutes)
    approutes))

(defn start [port]
  (run-server app {:port port :join? false})
  (println (str "\nPOSThere.io: Server running on port - " port ", hot-reload - " hot-reload)))

(defn -main []
  (start 3000))