(ns posthere.integration.delete
  "Test DELETE API request handling by the POSThere.io service."
  (:require [midje.sweet :refer :all]
            [ring.mock.request :refer (request)]
            [posthere.util.uuid :refer (uuid)]
            [posthere.app :refer (app)]
            [posthere.storage :refer (save-request requests-for)]))

(defn- url-for [url-uuid]
  (str "/" url-uuid))

(facts "about DELETE API responses"

  (fact "default response status is provided"
    (let [url-uuid (uuid)
          url (url-for url-uuid)
          request (request :delete url)
          response (app request)]
      (:status response) => 204))

  (fact "default response status is provided for a URL with multiple segments"
    (let [url-uuid "foo/bar/blat/bloo"
          url (url-for url-uuid)
          request (request :delete url)
          response (app request)]
      (:status response) => 204)))

(fact "about the effect of a DELETE API request"
  (let [url-uuid (uuid)
        url (url-for url-uuid)]  
    ;; Store some requests
    (save-request url-uuid {:headers {:foo :bar}})
    (save-request url-uuid {:headers {:bar :foo}})
    ;; Verify they are there
    (requests-for url-uuid) => [{:headers {:bar :foo}}{:headers {:foo :bar}}]
    ;; Delete the requests
    (app (request :delete url))
    ;; Verify they are gone
    (requests-for url-uuid) => []))