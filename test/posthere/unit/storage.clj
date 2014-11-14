(ns posthere.unit.storage
  "Test Redis storage of requests."
  (:require [clojure.string :as s]
            [midje.sweet :refer :all]
            [taoensso.carmine :as car]
            [clj-time.core :as t]
            [posthere.util.uuid :refer (uuid)]
            [posthere.storage :as storage :refer (day request-separator request-storage-count)]))

(facts "about storing requests"

  (future-fact "requests can be stored in the request list"
    (let [url-uuid (uuid)]
      (storage/save-request url-uuid {})
      (storage/wcar*
        (car/exists (storage/url-key-for url-uuid))) => true
        (car/llen (storage/url-key-for url-uuid)) => 1))
  
  (fact "stored request lists expire"
    (let [url-uuid (uuid)]
      (storage/save-request url-uuid {})
      (storage/wcar*
        (car/ttl (storage/url-key-for url-uuid))) => (roughly day 10)))

  (fact "request list entries contain an expiration and a request UUID"
    (let [url-uuid (uuid)]
      (storage/save-request uuid {})
      (let [request-entry (storage/wcar* (car/lpop (storage/url-key-for uuid)))]
          (count (storage/parse-request-entry request-entry)) => 2)))

  (fact "stored requests expire"
    (let [url-uuid (uuid)]
      (storage/save-request uuid {})
      (let [request-entry (storage/wcar* (car/lpop (storage/url-key-for uuid)))
            request-uuid (storage/uuid-from-entry request-entry)]
        (storage/wcar* (car/ttl (storage/request-key-for request-uuid))) => (roughly day 10))))

  (facts "stored requests can be retrieved"
    ;; store 1 request
    (let [request {:cool :beans}
          url-uuid (uuid)]
      (storage/save-request url-uuid request)
      (storage/requests-for url-uuid) => [request])
    ;; store many requests
    (let [requests [{:cool :beans}{:rotten :bananas}{:evolved :ferrets}]
          url-uuid (uuid)]
      (doseq [request requests]
        (storage/save-request url-uuid request))
      (storage/requests-for url-uuid) => (reverse requests)))

  (fact "only the last request-storage-count requests are stored"
    (let [request {:cool :beans}
          url-uuid (uuid)]
      ;; store too many requests!
      (dotimes [n (+ request-storage-count 2)]
        (storage/save-request url-uuid request))
      ; but there's still only the max
      (let [requests (storage/requests-for url-uuid)]
        (count requests) => request-storage-count
        requests => (repeat request-storage-count request)))))