(ns posthere.unit.storage
  "Test Redis storage of requests."
  (:require [clojure.string :as s]
            [midje.sweet :refer :all]
            [taoensso.carmine :as car]
            [clj-time.core :as t]
            [posthere.util.uuid :refer (uuid)]
            [posthere.storage :as storage :refer (day request-separator)]))

(facts "about storing requests"

  (future-fact "requests can be stored")
    ;; Store
  
  (future-fact "stored requests can be retrieved")
    ;; Store, Read, check expires
  
  (fact "request lists expire"
    (let [url-uuid (uuid)]
      (storage/save-request url-uuid {})
      (storage/wcar*
        (car/ttl (storage/url-key-for url-uuid))) => (roughly day 10)))

  (future-fact "request list entries contain an expiration and a request UUID")

  (fact "stored requests expire"
    (let [url-uuid (uuid)]
      (storage/save-request uuid {})
      (let [request-entry (storage/wcar* (car/lpop (storage/url-key-for uuid)))
            request-uuid (last (s/split request-entry (re-pattern (str "\\" request-separator))))]
        (println request-entry)
        (println request-uuid)
        (storage/wcar* (car/ttl (storage/request-key-for request-uuid))) => (roughly day 10))))


  (future-fact "only the last 100 requests are stored")
    ;; Store 101
    ;; Read, get 100
)