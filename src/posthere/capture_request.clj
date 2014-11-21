(ns posthere.capture-request
  "
  Capture the request to a particular URL in the storage so they can be retrieved later.
  Respond to the request.
  "
  (:require [clojure.string :as s]
            [clojure.core.incubator :refer (dissoc-in)]
            [clojure.core.match :refer (match)]
            [defun :refer (defun-)]
            [ring.util.codec :refer (form-decode)]
            [ring.util.response :refer (header response status)]
            [posthere.storage :refer (save-request)]
            [clj-time.core :as t]
            [cheshire.core :refer (parse-string generate-string)]
            [clojure.data.xml :refer (parse-str indent-str)]))

(def max-body-size (* 1024 1024)) ; 1 megabyte

(def form-urlencoded "application/x-www-form-urlencoded")

(def default-http-status-code 200)

;; http://en.wikipedia.org/wiki/List_of_HTTP_status_codes
(def http-status-codes #{
  100 101 102 
  200 201 202 203 204 205 206 207 208 226
  300 301 302 303 304 305 306 307 308
  400 401 402 403 404 405 406 407 408 409 410 411 412 413 414 415 416 417 418 419 420 
  422 423 424 426 428 429 431 440 449 450 451 494 495 496 497 498 499
  500 501 502 503 504 505 506 507 508 509 510 511 520 521 522 523 524 598 599
})

(def json-mime-types #{
  "application/json"
  "application/x-json"
  "application/javascript"
  "application/x-javascript"
  "text/json"
  "text/x-json"
  "text/javascript"
  "text/x-javascript"
  })

(def xml-mime-types #{
  "application/xml"
  "application/xhtml+xml"
  "application/rdf+xml"
  "application/atom+xml"
  "text/xml"
  })

(defn- add-time-stamp [request] 
  (assoc request :timestamp (str (t/now))))

(defn- content-type-for [request]
  (get-in request [:headers "content-type"]))

;; ----- HTTP Response -----

(defn post-response-body [url-uuid]
  (str "We got your POST request! View your results at: http://posthere.io/" url-uuid "\n"))

(defn- post-response
  ""
  [url-uuid request]
  (-> (post-response-body url-uuid) ; string with directions
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
      (dissoc-in requested-status-path))))

;; ----- Limit Body Size -----

(defn- update-request-body-too-big [uuid request-hash]
  (save-request 
    uuid 
    (assoc (dissoc request-hash :body) :body-overflow true)))

(defn- content-length-OK?
  "Check if the content length header is < max-body-size"
  [request]
  (let [content-length-header (read-string (get-in request [:headers "content-length"])) ; content-length header as int
        content-length (or content-length-header 0)] ; 0 if we have no content-length header
    (< content-length max-body-size)))

;; TODO check body length after slurp
;; TODO set overflow flags
(defn- limit-body-request-size
  ""
  [request]
  (if (and (:body request) (content-length-OK? request))
    (assoc request :body (slurp (:body request)))
    (dissoc request :body)))

;; ----- Pretty Print Body -----

(defun- json?
  "Generously determine if this mime-type is possibly JSON."
  ([nil] false)
  ([content-type] 
    (let [base (first (s/split content-type #";"))]
      (or
        (contains? json-mime-types base) ; is a JSON mime-type seen out in the wild
        (re-find #"\+json" base))))) ; is some custom mime-type that uses JSON
  
(defun- xml?
  "Generously determine if this mime-type is possibly XML."
  ([nil] false)
  ([content-type] 
    (let [base (first (s/split content-type #";"))]
      (or
        (contains? xml-mime-types base) ; is an XML mime-type seen out in the wild
        (re-find #"\+xml" base))))) ; is some custom mime-type that uses XML

(defn- url-encoded? [content-type]
  (= content-type form-urlencoded))

(defn- pretty-print
  "Try to parse the request body as XML if content-type is XML or not provided and it's not JSON"
  [request content-type? pretty-print derived-content-type]
  (let [content-type (content-type-for request)]
    (if (or (content-type? content-type)
            (and (s/blank? content-type) (s/blank? (:derived-content-type request))))
      (try
        ; pretty-print the body
        (-> request 
          (assoc :body (pretty-print request))
          (assoc :derived-content-type derived-content-type))
        (catch Exception e ; XML parsing failed
          (if (s/blank? content-type) ; did they tell us it was XML?
            request ; we were only guessing it might be XML, so leave it as is
            (assoc request :invalid-body true)))) ; they told us it was XML, but it didn't parse
      request))) ; content-type is not XML

(defn- pretty-print-json
  [request]
  (pretty-print
    request
    json?
    (fn [request] (generate-string (parse-string (:body request)) {:pretty true}))
    "JSON"))

(defn- pretty-print-xml
  [request]
  (pretty-print
    request
    xml?
    (fn [request] (indent-str (parse-str (:body request))))
    "XML"))
 
 (defn- pretty-print-urlencoded
  [request]
  (pretty-print
    request
    url-encoded?
    (fn [request] (form-decode (:body request)))
    "URL ENCODED"))

;; ----- Parse URL Encoding -----

(defn- parse-query-string
  "Parse the query string into a map if there is one."
  [request]
  (if-let [query-string (:query-string request)]
    (assoc request :parsed-query-string (form-decode query-string))
    request)) ; no query-string to parse

;; ----- Data flow: Incoming Request -> Processed Request -> Storage -> HTTP Response -----

(defn capture-request
  "Save the processed request, respond to the POST."
  [url-uuid request]
  ;; Process the request
  (let [processed-request 
    (-> request
      (add-time-stamp)
      (limit-body-request-size)
      (parse-query-string)
      (pretty-print-json) ; handle the body data if it's JSON
      (pretty-print-xml) ; handle the body data if it's XML
      (pretty-print-urlencoded) ; handle the body data if it's URL encoded field data
      (handle-response-status))]
    ;; Save the request
    (save-request url-uuid processed-request)
    ;; Respond to the HTTP client
    (post-response url-uuid processed-request)))