(ns posthere.integration.get
  "Test GET API request handling by the POSThere.io service."
  (:require [midje.sweet :refer :all]
            [ring.mock.request :refer (request header)]
            [posthere.util.uuid :refer (uuid)]
            [posthere.app :refer (app)]
            [posthere.storage :refer (save-request requests-for)]))

(defn- url-for [url-uuid]
  (str "/" url-uuid))

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

  (future-fact "a JSON representation of the examples is returned for a request on the examples")

  (fact "a JSON representation of the requests made is returned"
    (let [url-uuid (uuid)
          url (url-for url-uuid)]
      ;; Store some requests
      (save-request url-uuid {:body "<book><title>The Stranger</title></book>" :headers {:foo :bar}})
      (save-request url-uuid {:body "{\"title\":\"The Stranger\"}" :headers {:bar :foo}})
      ;; GET the requests
      (let [request (request :get url)
            accept-header (header request "Accept" "application/json")
            response (app accept-header)]
        (:body response) =>
          "[{\"headers\":{\"bar\":\"foo\"},\"body\":\"{\\\"title\\\":\\\"The Stranger\\\"}\"},{\"headers\":{\"foo\":\"bar\"},\"body\":\"<book><title>The Stranger</title></book>\"}]"))))