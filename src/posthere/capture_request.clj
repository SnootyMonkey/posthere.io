(ns posthere.capture-request
  "Capture the request to a particular URL in the storage so they can be retrieved later."
  (:require [ring.util.codec :refer (form-decode)]
            [ring.util.response :refer (header response)]
            [posthere.storage :refer (save-request)]
            [clj-time.core :as t]))

(def max-body-size (* 1024 1024)) ; 1 megabyte

(def form-urlencoded "application/x-www-form-urlencoded")

(defn post-response-body [url-uuid]
  (str "We got your POST request! View your results at: http://posthere.io/" url-uuid "\n"))

(defn- post-response [url-uuid]
  (header (response (post-response-body url-uuid)) "Content-Type" "text/plain"))

(defn- update-request-body-too-big [uuid request-hash]
  (save-request 
    uuid 
    (assoc (dissoc request-hash :body) :body-overflow true)))

(defn- add-time-stamp [request] 
  (assoc request :timestamp (str (t/now))))

(defn- content-length-OK?
  "Check if the content length header is < max-body-size"
  [request]
  (let [content-length-header (read-string (get-in request [:headers "content-length"])) ; content-length header as int
        content-length (or content-length-header 0)] ; 0 if we have no content-length header
    (< content-length max-body-size)))

; TODO check body length after slurp
; TODO set overflow flags
(defn- limit-body-request-size [request]
  (if (and (:body request) (content-length-OK? request))
    (assoc request :body (slurp (:body request)))
    (dissoc request :body)))

(defn- parse-query-string [request] request)

(defn- parse-form-fields
  "Parse the form fields string into a map if the content type indicates the body is form encoded."
  [request]
  (if (= (:content-type request) form-urlencoded)
    (assoc request :parsed-body (form-decode (:body request)))
    request))

(defn capture-request
  "Save the processed request, respond to the POST."
  [url-uuid request]
  ;; Process the request
  (save-request url-uuid 
    (-> request
      (add-time-stamp)
      (limit-body-request-size)
      (parse-query-string)
      (parse-form-fields)))
  ;; Respond to the HTTP client
  (post-response url-uuid))