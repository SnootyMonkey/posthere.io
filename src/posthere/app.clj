(ns posthere.app
  "PostHere.io web application."
  (:gen-class)
    (:use     [net.cgrand.enlive-html]
              [json-html.core])
    (:require [ring.middleware.reload :as reload]
              [compojure.core :refer :all]
              [compojure.route :as route]
              [org.httpkit.server :refer (run-server)]
              [environ.core :refer (env)]
              [posthere.storage :refer [save-request, requests-for]]))

(defonce hot-reload (or (env :hot-reload) true))

(defn template-for [name] (str "../resources/html/" name))

; Results page work
(deftemplate results-page (template-for "results.html") [results] 
  [:head :title] (html-content (str "POSThere.io - Results"))

  ; any idea how we would clean this up?
  [:#results] (if (> (count results) 0) 
                #(do 
                  (remove-attr :style)
                  (html-content (str results))))
  [:#empty-results] (if (> (count results) 0) nil))

(defn- results-view [uuid]
  (let [results (requests-for uuid)]
    (apply str (results-page results))))

; POST results
(defn- post-results [uuid, request-hash]
  (save-request uuid request-hash)
  (str "Post from " uuid " with request-hash " request-hash))
; POST results

(defroutes approutes
  ; GET requests
  (GET "/:uuid" [uuid] (results-view uuid))
  (GET "/" [] (str "Home From Nginx"))

  ; POST requests
  (POST "/:uuid" [uuid :as request]
    (let [body (:body request)]
      (post-results uuid (slurp body))))

  ; Standard requests
  (route/resources "/assets/")
  (route/not-found "Page Not Found"))

(def app
  (if hot-reload
    (reload/wrap-reload #'approutes)
    approutes))

(defn start [port]
  (run-server app {:port port :join? false})
  (println (str "\nPOSThere.io: Server running on port - " port ", hot-reload - " hot-reload)))

(defn -main []
  (start 3000))