(ns posthere.integration.get
  "Test GET API request handling by the POSThere.io service."
  (:require [clojure.walk :refer (keywordize-keys)]
            [midje.sweet :refer :all]
            [ring.mock.request :refer (request header)]
            [cheshire.core :refer (parse-string)]
            [posthere.util.uuid :refer (uuid)]
            [posthere.app :refer (app)]
            [posthere.storage :refer (save-request requests-for)]
            [posthere.examples :as examples :refer (example-results)]))

(defn- url-for [url-uuid]
  (str "/" url-uuid))

(defn- remove-timestamp [result]
  (dissoc result :timestamp))

(facts "about GET API responses"

  (fact "default response status is provided"
    (let [url-uuid (uuid)
          url (url-for url-uuid)
          request (request :get url)
          response (app request)]
      (:status response) => 200))

  (fact "default response status is provided for a URL with multiple segments"
    (let [url-uuid "foo/bar/blat/bloo"
          url (url-for url-uuid)
          request (request :get url)
          response (app request)]
      (:status response) => 200)))

(facts "about the contents of a GET API request"

  (fact "an empty JSON array is returned when there are no matching requests"
    (let [url-uuid (uuid)
          url (url-for url-uuid)
          request (request :get url)
          accept-header (header request "Accept" "application/json")
          response (app accept-header)]
      (:body response) => "[]"))

  (fact "a JSON representation of the examples is returned for a request on the examples"
    (let [request (request :get examples/example-url)
          accept-header (header request "Accept" "application/json")
          response (app accept-header)]
      (map remove-timestamp (keywordize-keys (parse-string (:body response)))) =>
        (map remove-timestamp (keywordize-keys (example-results)))))

  (fact "a JSON representation of the requests made is returned"
    (let [url-uuid (uuid)
          url (url-for url-uuid)]
      ;; Store some requests
      (save-request url-uuid {:body "Foo" :headers {:foo :bar}})
      (save-request url-uuid {:body "Bar" :headers {:bar :foo}})
      ;; GET the requests
      (let [request (request :get url)
            accept-header (header request "Accept" "application/json")
            response (app accept-header)]
        (parse-string (:body response)) =>
          (parse-string "[{\"headers\":{\"bar\":\"foo\"},\"body\":\"Bar\"},{\"headers\":{\"foo\":\"bar\"},\"body\":\"Foo\"}]")))))