(ns posthere.integration.post
  "Test POST handling by the posthere.io service."
  (:require [midje.sweet :refer :all]
            [ring.mock.request :refer (request body content-type header)]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [posthere.util.uuid :refer (uuid)]
            [posthere.app :refer (app)]
            [posthere.capture-request :refer (post-response-body)]
            [posthere.storage :refer (requests-for)]))

(def string-body "I'm a little teapot.")
(def json-body "{\"lyric\": \"I'm a little teapot.\"}")
(def xml-body "<song><lyric>I'm a little teapot.</lyric></song>")

(def query-params "foo=bar&ferrets=evolved")

(defn url-for [url-uuid]
  (str "/" url-uuid))

(facts "about POST responses"

  (fact "default response status is a 200"
    (let [url-uuid (uuid)
          url (url-for url-uuid)
          request (request :post url)
          response (app request)]
      (:status response) => 200))

  (fact "response content-type is text/plain"
    (let [url-uuid (uuid)
          url (url-for url-uuid)
          request (request :post url)
          response (app request)]
      (get-in response [:headers "Content-Type"]) => "text/plain"))

  (fact "response body is text informing the developer what to do next"
    (let [url-uuid (uuid)
          url (url-for url-uuid)
          request (request :post url)
          response (app request)]
      (:body response) => (post-response-body url-uuid))))

(facts "about POSTs getting saved"

  (fact "timestamp is saved, and it's about now"
    (let [url-uuid (uuid)
          url (url-for url-uuid)
          request (request :post url)
          response (app request)
          timestamp (f/parse (:timestamp (first (requests-for url-uuid))))]
        (t/after? timestamp (t/minus (t/now) (t/seconds 10))) => true ; after 10 secs ago
        (t/before? timestamp (t/now)) => true)) ; before now

  (fact "headers are saved"
    (let [url-uuid (uuid)
          url (url-for url-uuid)
          request (request :post url)
          headers (-> request
            (header "user-agent" "ring-mock")
            (header "accept" "inevitability/death")
            (header "content-type" "delicious/cake")
            (header "content-length" "42")
            (header "super-bowl" "Buccaneers"))
          response (app headers)
          request-headers (:headers (first (requests-for url-uuid)))]
      request-headers => (contains {"user-agent" "ring-mock"})
      request-headers => (contains {"accept" "inevitability/death"})
      request-headers => (contains {"content-type" "delicious/cake"})
      request-headers => (contains {"content-length" "42"})
      request-headers => (contains {"super-bowl" "Buccaneers"})))

  ; POST saves query parameters
  (facts "query-string is saved"
    
    (fact "without a body"
      (let [url-uuid (uuid)
            url (url-for (str url-uuid "?" query-params))
            request (request :post url)
            response (app request)]
        (:query-string (first (requests-for url-uuid))) => query-params))
    
    (fact "with a body"
      (let [url-uuid (uuid)
          url (url-for (str url-uuid "?" query-params))
          request (request :post url)
          body (body request string-body)
          response (app body)]
        (:query-string (first (requests-for url-uuid))) => query-params)))

  ; POST saves form fields

  ; POST saves BODY content
  (facts "body is saved"

    (fact "string body is saved"
      (let [url-uuid (uuid)
            url (url-for url-uuid)
            request (request :post url)
            body (body request string-body)
            response (app body)]
          (:body (first (requests-for url-uuid))) => string-body))

    (fact "JSON body is saved"
      (let [url-uuid (uuid)
            url (url-for url-uuid)
            request (request :post url)
            body (body request json-body)
            response (app body)]
          (:body (first (requests-for url-uuid))) => json-body))

    (fact "XML body is saved"
      (let [url-uuid (uuid)
            url (url-for url-uuid)
            request (request :post url)
            body (body request xml-body)
            response (app body)]
          (:body (first (requests-for url-uuid))) => xml-body))))


(future-facts "about giant POSTs getting partially saved"

  ; POST doesn't save BODY if content-length header > 1MB

  ; POST doesn't save BODY if content is > 1MB

  )

(future-facts "about requested status getting used as the status"

  ; valid HTTP statuses get used as the status

  ; invalid HTTP statuses don't get used as the status

  )

(future-facts "GETs, PUTS, DELETEs don't get saved")