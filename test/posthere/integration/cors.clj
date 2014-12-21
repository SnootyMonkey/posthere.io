(ns posthere.integration.cors
  "Test CORS responses from the POSThere.io service."
  (:require [midje.sweet :refer :all]
            [ring.mock.request :refer (request body content-type header content-length)]
            [posthere.util.uuid :refer (uuid)]
            [posthere.app :refer (app)]
            [posthere.examples :as examples]
            [cheshire.core :refer (parse-string generate-string)]
            [clojure.data.xml :refer (parse-str indent-str)]))

(def origins ["http://example.com:3000/" "https://example.com/" "null"])

(defn- url-for [url-uuid]
  (str "/" url-uuid))

(facts "about CORS pre-flight responses"

  (fact "success response status is provided"
  	(doseq [origin origins]
    	(let [url-uuid (uuid)
          	url (url-for url-uuid)
            request (request :options url)
            origin-header (header request "Origin" origin)
            access-control-header (header origin-header "Access-Control-Method" "POST")
            access-control-request-header (header access-control-header "Access-Control-Request-Headers" "X-Requested-With")
            response (app access-control-request-header)]
      (:status response) => 200)))

  (fact "CORS headers are provided"
    (doseq [origin ["http://example.com:3000/" "https://example.com/" "null"]]
      (let [url-uuid (uuid)
            url (url-for url-uuid)
            request (request :options url)
            origin-header (header request "Origin" origin)
            access-control-header (header origin-header "Access-Control-Method" "POST")
            access-control-request-header (header access-control-header "Access-Control-Request-Headers" "X-Requested-With")
            response (app access-control-request-header)
            response-headers (:headers response)]
        response-headers => (contains {
          "Access-Control-Allow-Headers" "X-Requested-With"
          "Access-Control-Allow-Origin" origin})))))

(facts "about CORS POST responses"

  (fact "success response status is provided"
    (doseq [origin origins]
      (let [url-uuid (uuid)
            url (url-for url-uuid)
            request (request :post url)
            origin-header (header request "Origin" origin)
            requested-with-header (header origin-header "X-Requested-With" "XMLHttpRequest")
            response (app requested-with-header)]
      (:status response) => 200)))

  (fact "CORS Origin header is provided"
    (doseq [origin origins]
      (let [url-uuid (uuid)
            url (url-for url-uuid)
            request (request :post url)
            origin-header (header request "Origin" origin)
            requested-with-header (header origin-header "X-Requested-With" "XMLHttpRequest")
            response (app requested-with-header)
            response-headers (:headers response)]
        response-headers => (contains {"Access-Control-Allow-Origin" origin})))))