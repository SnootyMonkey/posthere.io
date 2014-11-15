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

  ; POST saves headers

  ; POST saves GET attributes

  ; POST saves BODY content
  (fact "body is saved"
    (let [url-uuid (uuid)
          url (url-for url-uuid)
          request (request :post url)
          body (body request "I'm a little teapot.")
          response (app body)]
        (:body (first (requests-for url-uuid))) => "I'm a little teapot.")))

(future-facts "about giant POSTs getting partially saved"

  ; POST doesn't save BODY if content-length header > 1MB

  ; POST doesn't save BODY if content is > 1MB

  )

(future-facts "about requested status getting used as the status"

  ; valid HTTP statuses get used as the status

  ; invalid HTTP statuses don't get used as the status

  )

(future-facts "GETs, PUTS, DELETEs don't get saved")