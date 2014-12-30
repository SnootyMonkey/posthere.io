(ns posthere.capture-request
  "
  Capture the request to a particular URL in the storage so they can be retrieved later.
  Respond to the request.
  "
  (:require [ring.util.codec :refer (form-decode)]
            [ring.util.response :refer (header response status)]
            [clojure.string :as s]
            [clj-time.core :as t]
            [defun :refer (defun-)]
            [posthere.storage :refer (save-request)]
            [posthere.pretty-print :refer (pretty-print-json pretty-print-xml pretty-print-urlencoded)]))

(def max-body-size 1000000) ; number of bytes in 1 megabyte for content-length header

(def default-http-status-code 200)

(def too-big "TOO BIG")

;; http://en.wikipedia.org/wiki/List_of_HTTP_status_codes
(def http-status-codes #{
  100 101 102
  200 201 202 203 204 205 206 207 208 226
  300 301 302 303 304 305 306 307 308
  400 401 402 403 404 405 406 407 408 409 410 411 412 413 414 415 416 417 418 419 420
  422 423 424 426 428 429 431 440 444 449 450 451 494 495 496 497 498 499
  500 501 502 503 504 505 506 507 508 509 510 511 520 521 522 523 524 598 599
})

(defn- add-time-stamp [request]
  (assoc request :timestamp (str (t/now))))

;; ----- HTTP Response -----

(defn post-response-body [url-uuid host]
  (str "We got your request! View your results at: http://" host "/" url-uuid "\n"))

(defn- post-response
  "Create the response to the stored POST request."
  [url-uuid request]
  (-> (post-response-body url-uuid (get-in request [:headers "host"])) ; string with directions
    (response) ; make the string the body of our response
    (header "Content-Type" "text/plain") ; add a content type header
    (status (:status request)))) ; add the HTTP status of the response

;; ----- HTTP Status -----

(defn- valid-status
  "Esure the requested status is an integer in our set of valid HTTP statuses,
   default the status if it's not or none was requested."
  [requested-status]
  (let [status (read-string (or requested-status "-1"))] ; turn status string into an integer
    (if (contains? http-status-codes status) ; make sure it's in our valid set
      status ; use the valid status they provided
      default-http-status-code))) ; they provided an invalid status (or none at all), so use the default status

(defn- handle-response-status
  "Move the requested status from the query string params to the root of the request map."
  [request]
  (let [requested-status-path [:parsed-query-string "status"]
        requested-status (get-in request requested-status-path)
        status (valid-status requested-status)] ; ensure the requested status is valid
    (-> request
      (assoc :status status)
      (update-in [:parsed-query-string] dissoc "status"))))

;; ----- Limit Body Size -----

(defn- content-length-OK?
  "Check if the content-length header is <= max-body-size"
  [request]
  (let [content-length-header (read-string (get-in request [:headers "content-length"])) ; content-length header as int
        content-length (or content-length-header 0)] ; 0 if we have no content-length header
    (<= content-length max-body-size)))

(defn- body-overflow 
  "The body in the request was too big so remove it, and set the body overflow flag."
  [request]
  (-> request
    (dissoc :body)
    (assoc :derived-content-type too-big)
    (assoc :body-overflow true)))

(defn- read-body
  "Read in from the body InputStream 1 byte more than the max body size."
  [request]
  (let [bis (java.io.BufferedInputStream. (:body request))
        buffer (make-array Byte/TYPE (+ max-body-size 1))
        size (.read bis buffer)] ; read up to 1 byte more than our max body size
    (String. buffer 0 size "UTF-8"))) ; return a String of what we read

(defn- body-length-OK?
  "True if the body content that was read is <= the max body size."
  [body]
  (<= (count (.getBytes body "UTF-8")) max-body-size))

(defun- limit-body-request-size
  "
  If there's a body in the request, read it in if it's less than the max size,
  and set an overflow flag if it's bigger than the max size.
  "
  ([request :guard #(:body %)]
    (if (content-length-OK? request) ; indicated content-length is small enough, so try reading in the body
      (let [body (read-body request)] ; read the body from the input stream
        (if (body-length-OK? body)
          (assoc request :body body) ; body was indeed small enough so keep it
          (body-overflow request))) ; they lied! the body was actually too big
      (body-overflow request))) ; content-length indicateds the body is too big

  ([request] request)) ; there is no body, so nothing to do

;; ----- Parse URL Encoding -----

(defn- parse-query-string
  "Parse the query string into a map if there is one."
  [request]
  (if-let [query-string (:query-string request)]
    (assoc request :parsed-query-string (form-decode query-string))
    request)) ; no query-string to parse

;; ----- Request Capture -----

(defn- host? [header]
  (= (s/upper-case header) "HOST"))

(defn- extract-request-parts
  "Given a processed ring request, extract the pieces of it that we want to store into a new map."
  [request]
  (-> {}
    ; don't store "host" in the headers
    (assoc :headers (select-keys (:headers request) (filter #(not (host? %)) (keys (:headers request)))))
    ; extract all the other pieces of the ring request that we need to store in redis
    (assoc :timestamp (:timestamp request))
    (assoc :parsed-query-string (:parsed-query-string request))
    (assoc :derived-content-type (:derived-content-type request))    
    (assoc :status (:status request))
    (assoc :body (:body request))
    (assoc :invalid-body (:invalid-body request))
    (assoc :body-overflow (:body-overflow request))
    (assoc :request-method (:request-method request))))

(defn capture-request
  "
  Save the processed request, respond to the POST.

  Data flow: Incoming Ring Request -> Processed Request -> Storage -> HTTP Response
  "
  [url-uuid request]
  ;; Process the request
  (let [processed-request
    (-> request
      (add-time-stamp) ; save the time of the request
      (parse-query-string) ; handle any query string parameters
      (limit-body-request-size) ; read in the body and deal with bodies that are bigger than the max allowed
      (pretty-print-json) ; handle the body data if it's JSON
      (pretty-print-xml) ; handle the body data if it's XML
      (pretty-print-urlencoded) ; handle the body data if it's URL encoded field data
      (handle-response-status))] ; handle the requested states
    ;; Save the request
    (save-request url-uuid (extract-request-parts processed-request))
    ;; Respond to the HTTP client
    (post-response url-uuid processed-request)))