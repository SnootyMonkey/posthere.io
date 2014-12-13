(ns posthere.app
  "PostHere.io web application."
  (:gen-class)
    (:require [clojure.string :as s]
              [ring.middleware.reload :as reload]
              [ring.util.response :refer (response status)]
              [compojure.core :refer (GET POST)]
              [compojure.route :as route]
              [org.httpkit.server :refer (run-server)]
              [environ.core :refer (env)]
              [posthere.capture-request :refer (capture-request)]
              [posthere.storage :refer (requests-for)]
              [posthere.results :refer (results-view)]))

(def example-url "/_/example")
(def post-to-example (str "The POSThere.io URL " example-url " is reserved for internal use."))

(defonce hot-reload (or (env :hot-reload) false))

(defn- example-results
  "Read in canned results as an example."
  []
  (read-string (slurp (clojure.java.io/resource "example.edn"))))

(defn- uuid-for [request]
  "Extract everything after the initial / in the request URL as the UUID, not including the /."
  (s/replace (get-in request [:route-params :*]) #"^\/" ""))

(defn- results-for
  "Get the UUID from the request, get the results from storage for the UUID,
  then pass them on to the results view rendering."
  [request]
  (let [uuid (uuid-for request)]
    (results-view (requests-for uuid) uuid)))

(defroutes approutes

  ; Resource requests (in development only, otherwise handled by nginx)
  (route/resources "/")
  
  ; GET requests
  (GET "/" [] (slurp "./resources/public/index")) ; This is for development, should be handled by nginx in production
  ;; TODO update the timestamps from the example so they won't grow old
  (GET example-url [] (results-view (example-results) "_/example"))
  (GET "*" [:as request] (results-for request)) ; Show them the results of their requests

  ; POST requests
  (POST example-url [] (status (response post-to-example) 403)) ; return a status and a message
  (POST "*" [:as request] (capture-request (uuid-for request) request)) ; Capture their POST request

  )

(def app
  (if hot-reload
    (reload/wrap-reload #'approutes)
    approutes))

(defn start [port]
  (run-server app {:port port :join? false})
  (println (str "\nPOSThere.io: Server running on port - " port ", hot-reload - " hot-reload)))

(defn -main []
  (start 3000))