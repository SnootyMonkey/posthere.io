(ns posthere.app
  "PostHere.io web application."
  (:gen-class)
    (:require [clojure.string :as s]
              [ring.middleware.reload :refer (wrap-reload)]
              [raven-clj.ring :refer (wrap-sentry)]
              [ring.util.response :refer (response status)]
              [compojure.core :refer (GET POST PUT PATCH DELETE defroutes)]
              [compojure.route :as route]
              [org.httpkit.server :refer (run-server)]
              [ring.middleware.cors :refer (wrap-cors)]
              [environ.core :refer (env)]
              [posthere.capture-request :refer (capture-request)]
              [posthere.storage :refer (requests-for delete-requests)]
              [posthere.examples :as examples :refer (example-results)]
              [posthere.results :refer (results-view)]))

;; DSN for error reporting to Sentry
(defonce dsn (or (env :raven-dsn) false))

;; Reload code dynamically with each web request in development
(defonce hot-reload (or (env :hot-reload) false))

(defn- route-for [request]
  "Get the URL for the request from a ring request map."
  (get-in request [:route-params :*]))

(defn- strip-prefix-slash
  "Remove the / from the start of a URL."
  [relative-url]
  (s/replace relative-url #"^/" ""))

(defn- uuid-for
  "Extract everything after the initial / in the request URL as the UUID, not including the /."
  [request]
  (strip-prefix-slash (route-for request)))

(defroutes approutes

  ; Resource requests (in development only, otherwise handled by nginx)
  (route/resources "/")

  ;; Home page for development, handled by nginx in production
  (GET "/" [] (slurp "./resources/public/index"))

  ;; Show canned results as an example
  (GET examples/example-url [:as request] (results-view
                                            (example-results)
                                            (strip-prefix-slash examples/example-url)
                                            (:headers request)))

  ;; Show the results stored from their requests
  (GET "*" [:as request] (let [uuid (uuid-for request)] (results-view
                                                          (requests-for uuid)
                                                          uuid
                                                          (:headers request))))

  ;; Error if request is to the example URL
  (POST examples/example-url [] (status (response examples/post-to-example-body) examples/post-to-example-status))
  (PUT examples/example-url [] (status (response examples/post-to-example-body) examples/post-to-example-status))
  (PATCH examples/example-url [] (status (response examples/post-to-example-body) examples/post-to-example-status))

  ;; Capture the PUT/PATCH/POST request
  (POST "*" [:as request] (capture-request (uuid-for request) request))
  (PUT "*" [:as request] (capture-request (uuid-for request) request))
  (PATCH "*" [:as request] (capture-request (uuid-for request) request))

  ;; Delete a stored requests
  (DELETE "*" [:as request] (do (delete-requests (uuid-for request))(status {} 204))))

(defonce cors-routes (wrap-cors approutes #".*"))

(defonce hot-reload-routes
  (if hot-reload
      (wrap-reload #'cors-routes)
      cors-routes))

(def app
  (if dsn
    (wrap-sentry hot-reload-routes dsn)
    hot-reload-routes))

(defn start [port]
  (run-server app {:port port :join? false})
  (println (str "\nPOSThere.io: Server running on port - " port ", hot-reload - " hot-reload)))

(defn -main []
  (start 3000))