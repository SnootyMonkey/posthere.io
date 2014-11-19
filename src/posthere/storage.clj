(ns posthere.storage
  "Store and read POST requests to/from Redis."
  (:require [clojure.string :as s]
            [taoensso.carmine :as car]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [posthere.util.uuid :refer (uuid)]))

;; ----- Storage constants -----

(def day (* 60 60 24)) ; 1 day in seconds for Redis key expiration
(def request-storage-count 100) ; Store up to 100 requests per URL

;; ----- Redis "Schema" -----

(def url-key "url:")
(def request-key "request:")
(def request-separator "|")
(defn url-key-for [url-uuid] (str url-key url-uuid))
(defn request-key-for [request-uuid] (str request-key request-uuid))
(defn- request-entry-for [request-uuid] (str (t/plus (t/now) (t/hours 24)) request-separator request-uuid))

;; ----- Redis connection -----

(def server-conn {:pool {} :spec {:host "127.0.0.1" :port 6379}}) ; See `wcar` docstring for opts
(defmacro wcar* [& body] `(car/wcar server-conn ~@body))

;; ---- Internal -----

(defn parse-request-entry [request-entry]
  (s/split request-entry (re-pattern (str "\\" request-separator))))

(defn time-stamp-from-entry [request-entry]
  (first (parse-request-entry request-entry)))

(defn uuid-from-entry [request-entry]
  (last (parse-request-entry request-entry)))

(defn not-expired? [request-entry]
  (not (t/before? (f/parse (time-stamp-from-entry request-entry)) (t/now))))

(defn request-for [request-uuid]
  (if-let [request (wcar* (car/get (request-key-for request-uuid)))]
    request
    false))

;; ----- Public -----

(defn save-request
  "Store the request made to a UUID in Redis for up to 24h."
  [url-uuid request]

  (let [url-key (url-key-for url-uuid)
        request-uuid (uuid) ; new UUID for this request
        request-entry (request-entry-for request-uuid)
        request-key (request-key-for request-uuid)
        clean-request (dissoc request :async-channel)]
    
    ;; Save the POST request to the list for this URL UUID
    (wcar*
      (car/multi) ; transaction
        (car/lpush url-key request-entry) ; push the request onto the list
        (car/expire url-key day) ; renew the list expiration
        (car/set request-key clean-request) ; store the request
        (car/expire request-key day) ; expire the request
      (car/exec)) ; execute transaction

    ;; trim the request list if needed
    (if (> (wcar* (car/llen url-key)) request-storage-count)
      (wcar* (car/rpop url-key)))) ; trim the request list
  
  true)

(defn requests-for
  "Read the POST requests stored for a UUID from Redis."
  [url-uuid]
  (let [request-list (wcar*(car/lrange (url-key-for url-uuid) 0 request-storage-count)) ; Read list from Redis
        unexpired-request-list (filter not-expired? request-list)
        unexpired-requests (map uuid-from-entry unexpired-request-list)]
    (vec (map request-for unexpired-requests))))