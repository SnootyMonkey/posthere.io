(ns posthere.integration.post
  "Test POST handling by the posthere.io service."
  (:require [midje.sweet :refer :all]
            [ring.util.codec :refer (form-encode)]
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

(def params {
  "email" "jobs@path.com"
  "password" "This1sMySup3rS3cr3tPassw0rdAndY0uCanN0tGuess1t"
  "work-at" "Path"
  "most-evolved" "ferrets"
  "super-bowl" "Buccaneers"
})

(def header-values {
  "user-agent" "ring-mock"
  "accept" "inevitability/death"
  "content-type" "delicious/cake"
  "content-length" "42"
  "super-bowl" "Buccaneers"
})

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
          ; wrap the request in repeated calls to ring mock's header/3 function for each
          ; header name/value pair
          headers (reduce #(header %1 %2 (get header-values %2)) request (keys header-values))
          response (app headers)
          request-headers (:headers (first (requests-for url-uuid)))]
      ; verify storage contains each name/value header in the :headers map
      (doseq [header-name (keys header-values)]
        request-headers => (contains {header-name (get header-values header-name)}))))

  (facts "query-string is saved"
    
    (fact "without a body"
      (let [url-uuid (uuid)
            query-string (form-encode query-params)
            url (url-for (str url-uuid "?" query-string))
            request (request :post url)
            response (app request)]
        (:query-string (first (requests-for url-uuid))) => query-string))
    
    (fact "with a body"
      (let [url-uuid (uuid)
            query-string (form-encode query-params)
            url (url-for (str url-uuid "?" query-string))
            request (request :post url)
            body (body request string-body)
            response (app body)]
        (:query-string (first (requests-for url-uuid))) => query-string)))

 (fact "url-encoded form fields are saved"
    (let [url-uuid (uuid)
          url (url-for url-uuid)
          request (request :post url)
          body (body request params)
          response (app body)
          stored-request (first (requests-for url-uuid))]
      (:content-type stored-request) => "application/x-www-form-urlencoded"
      (:body stored-request) => (form-encode params)
      (:parsed-body stored-request) => params))

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

  ; POST doesn't save body if content-length header > 1MB

  ; POST doesn't save body if content is > 1MB

  )

(future-facts "about requested status getting used as the status"

  ; valid HTTP statuses get used as the status

  ; invalid HTTP statuses don't get used as the status

  )

(future-fact "other HTTP verbs don't get saved"

  ; GET, PUT, DELETE, PATCH, FOO
  )