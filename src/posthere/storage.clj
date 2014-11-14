(ns posthere.storage
  "Store and read POST requests to/from Redis."
  (:require [taoensso.carmine :as car]
            [clj-time.core :as t]
            [posthere.util.uuid :refer (uuid)]))

;; Storage constants
(def day (* 60 60 24)) ; 1 day in seconds for Redis key expiration
(def request-storage-count 100) ; Store up to 100 requests per URL

;; Redis "schema"
(def url-key "url:")
(def request-key "request:")
(def request-separator "|")
(defn url-key-for [url-uuid] (str url-key url-uuid))
(defn request-key-for [request-uuid] (str request-key request-uuid))
(defn- request-entry-for [request-uuid] (str (t/now) request-separator request-uuid))

;; Redis connection
(def server-conn {:pool {} :spec {:host "127.0.0.1" :port 6379}}) ; See `wcar` docstring for opts
(defmacro wcar* [& body] `(car/wcar server-conn ~@body))

(defn save-request
  "Store the request made to a UUID in Redis for up to 24h."
  [url-uuid request]
  (let [url-key (url-key-for url-uuid)
        request-uuid (uuid) ;; new UUID for this request
        request-entry (request-entry-for request-uuid)
        request-key (request-key-for request-uuid)]
    
    ;; Save the POST request to the list for this URL UUID
    (wcar*
      (car/multi) ;; transaction
        (car/lpush url-key request-entry) ;; push the request onto the list
        (car/expire url-key day) ;; renew the list expiration
        (car/set request-key request) ;; store the request
        (car/expire request-key day) ;; expire the request
      (car/exec)) ;; execute transaction

    ;; trim the request list if needed
    (if (> (wcar* (car/llen url-key)) request-storage-count)
      (wcar* (car/rpop url-key)))) ; trim the request list
  
  true)

(defn requests-for
  "Read the POST requests stored for a UUID from Redis."
  [uuid])