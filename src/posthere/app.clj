(ns posthere.app
  "PostHere.io web application."
  (:gen-class)
    (:use     [net.cgrand.enlive-html])
    (:require [ring.middleware.reload :as reload]
              [compojure.core :refer :all]
              [compojure.route :as route]
              [org.httpkit.server :refer (run-server)]
              [environ.core :refer (env)]
              [posthere.storage :refer [save-request, requests-for]]
              [clojure.data.json :as json]))

(defonce hot-reload (or (env :hot-reload) false))

(defn template-for [name] (str "../resources/html/" name))

; Results page work
(defsnippet result-rows (template-for "result-table.html")
  [:.result-group]
  [replacements]
  [:.result-group any-node] (replace-vars replacements))

(deftemplate results-page (template-for "results.html") [results, uuid] 
  [:head :title] (html-content (str "POSThere.io - Results"))

  ; any idea how we would clean this up?
  [:#results] (if (not-empty results) 
                  (remove-attr :style))
  
  [:#empty-results] (if (not-empty results)
                      nil
                      (append (str "")))

  [:#data] (if (not-empty results)
              (html-content (str "<script type='text/javascript'>var resultData = " (json/write-str results) "</script>"))
              (html-content (str "<script type='text/javascript'>var resultData = []; </script>")))

  [:.uuid-value] (html-content (str uuid)))

(defn- results-view [uuid]
  (let [results (requests-for uuid)]
    ; (doseq [item results] (prn (:headers item)))
    (apply str (results-page results uuid))))

(defn- update-request-body-too-big [uuid, request-hash]
  (save-request 
    uuid 
    (assoc (dissoc request-hash :body) :overflow true)))

(defn- post-results [uuid, request-hash]
  (let [body (slurp (:body request-hash))]
    ; if our content length is greater than 1MB
    (if (> (read-string ((request-hash :headers) "content-length")) (* 1024 1024))
      ; greater than 1MB
      (update-request-body-too-big uuid request-hash)

      ; less than 1MB, but we don't believe them
      (if (> (count (slurp (request-hash :body))) (* 1024 1024))
        ; checked the content body, it's too big.  liars.
        (update-request-body-too-big uuid request-hash)

        ; wow, it's not too big, let's just save it
        (save-request uuid (assoc request-hash :body body)))))

  (str "Post response to " uuid " with request" request-hash))

(defroutes approutes
  ; GET requests
  (GET "/:uuid" [uuid] (results-view uuid))
  (GET "/" [] (str "Home From Nginx"))

  ; POST requests
  (POST "/:uuid" [uuid :as request]
    (let [body request]
      (post-results uuid request)))

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