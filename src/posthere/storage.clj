(ns posthere.storage
  "Store and read POST/PUT/PATCH requests to/from Redis.

  # Redis Schema

  Up to a fixed number of requests. After that number, start deleting the oldest.

  Up to specified retention length. Requests go away after their retention expires.

  LRU eviction strategy (Redis setting) on the keys…. so if we are so popular that we are out
  of memory, we retain less requests.

  Set of keys with 'url' as a prefix for each UUID we’ve gotten a request to. URL key:

    url:{url-uuid}

  These keys expire in the retention period.

  These are lists. Each new request LPUSHes an entry into the list, up to 100 entries. At the max
  number of requests we both LPUSH the new entry and RPOP the oldest off the list. The entries in the
  list are UUID strings and timestamps separated by a |. The timestamp is when the entry
  will expire.

  List entry:

    {expiration-timestamp}|{request-uuid}

  Each element in the list can be used to form the key to a serialized EDN string of the
  ring request map. We only attempt to retrieve the request if the timestamp indicates the
  request hasn't expired yet. Request key has 'request' as a prefix:

    request:{request-uuid}

  These EDN representations of the ring request expire in the specified retention period.

  In each, we retain these name/values:

    timestamp: timestamp when it happened
    headers: header data
    status-code: integer HTTP status code
    query-string: raw query string as provided in the request
    parsed-query-string: parsed query string as a hash map
    body: the body, pretty-printed string for JSON/XML, hash map for form encoded
    invalid-body: true/false boolean for if body didn't parse as its indicated content-type
    body-overflow: true/false boolean if we overflowed our body size limit

  ## Sample Scenario

  Here is a hypothetical sequence of events, from an empty database...

  User GETs: http://posthere.io/test-it

  We look for a URL UUID key matching the 'test-it' UUID:

    LRANGE uuid:test 0 100

  And don’t find it, so they get empty results.

  User POSTs to: http://posthere.io/test-it

  We generate a random UUID for the request of abc-123.

  We push an entry to the URL UUID key:

    LPUSH uuid:test-it <timestamp + retention period>|abc-123

  If the list is > 100, we right POP to shorten it to 100:

    LLEN uuid:test-it
    RPOP uuid:test-it

  We set the list to expire:

    EXPIRE uuid:test-it 86400

  We create a hash, keyed by the request UUID, containing the details of the request:

    SET request:abc-123 <EDN string>

  We set this key to expire.

    EXPIRE request:abc-123 86400

  User GETS: http://posthere.io/test-it

  We get the most recent 100 keys in the list.

    LRANGE uuid:test-it 0 100

  For each key, if the timestamp portion hasn’t elapsed already, we get the request.

      GET request:abc-123

  User gets the stored request as their results.

  User again POSTs to: http://posthere.io/test-it

  We generate a new random UUID for the request of def-456.

  We push an new entry to the URL UUID key:

    LPUSH uuid:test-it <timestamp + retention period>|def-456

  We set the list to expire anew:

    EXPIRE uuid:test-it 86400

  And everything continues as before.
  "
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

(defn request-for
  "Read the stored ring request from Redis using the request key and the request UUID,
  removing the host from the headers."
  [request-uuid]
  (if-let [request (wcar* (car/get (request-key-for request-uuid)))]
    (-> request
      (assoc :headers (dissoc (:headers request) "host")) ; http-kit in dev is lower-case
      (assoc :headers (dissoc (:headers request) "Host"))) ; nginx-clojure in production is camel-case
    false))

;; ----- Public -----

(defn save-request
  "Store the request made to a UUID in Redis with an expiration."
  [url-uuid request]

  (let [url-key (url-key-for url-uuid)
        request-uuid (uuid) ; new UUID for this request
        request-entry (request-entry-for request-uuid)
        request-key (request-key-for request-uuid)
        clean-request (dissoc request :async-channel)]

    ;; Save the request to the list for this URL UUID
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
        unexpired-request-list (filter not-expired? request-list) ; filter out requests that have already expired
        unexpired-requests (map uuid-from-entry unexpired-request-list)] ; get request keys for the remaining requests
    (vec (map request-for unexpired-requests)))) ; turn the sequence of request keys into a vector of requests

(defn delete-requests
  "Remove any requests stored for a UUID from Redis."
  [url-uuid]
  (let [request-list (wcar*(car/lrange (url-key-for url-uuid) 0 request-storage-count)) ; Read list from Redis
        unexpired-request-list (filter not-expired? request-list) ; filter out requests that have already expired
        unexpired-requests (map uuid-from-entry unexpired-request-list)] ; get request keys for the remaining requests
    (wcar*
      (car/multi) ; transaction
        ;; Delete the request list
        (car/del (url-key-for url-uuid))
        ;; Delete the stored requests
        (doseq [unexpired-request unexpired-requests]
          (wcar* (car/del unexpired-request)))
      (car/exec)))) ; execute transaction