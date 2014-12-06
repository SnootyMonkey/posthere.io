(ns posthere.pretty-print
  "Pretty print JSON, XML and URL encoded content."
  (:require [clojure.string :as s]
            [defun :refer (defun-)]
            [ring.util.codec :refer (form-decode)]
            [cheshire.core :refer (parse-string generate-string)]
            [clojure.data.xml :refer (parse-str indent-str)]))

(def form-urlencoded "application/x-www-form-urlencoded")

(def json-mime-types #{
  "application/json"
  "application/x-json"
  "application/javascript"
  "application/x-javascript"
  "text/json"
  "text/x-json"
  "text/javascript"
  "text/x-javascript"
  })

(def xml-mime-types #{
  "application/xml"
  "application/xhtml+xml"
  "application/rdf+xml"
  "application/atom+xml"
  "text/xml"
  })

(def url-encoded "url-encoded")
(def json-encoded "json")
(def xml-encoded "xml")

(defn- content-type-for [request]
  (get-in request [:headers "content-type"]))

(defn- mark-url-encoded-invalid
  [request]
  (if (= (content-type-for request) form-urlencoded)
    (assoc request :invalid-body true)
    request))

(defun- json?
  "Generously determine if this mime-type is possibly JSON."
  ([nil] false)
  ([content-type]
    (let [base (first (s/split content-type #";"))]
      (or
        (contains? json-mime-types base) ; is a JSON mime-type seen out in the wild
        (re-find #"\+json" base))))) ; is some custom mime-type that uses JSON

(defun- xml?
  "Generously determine if this mime-type is possibly XML."
  ([nil] false)
  ([content-type]
    (let [base (first (s/split content-type #";"))]
      (or
        (contains? xml-mime-types base) ; is an XML mime-type seen out in the wild
        (re-find #"\+xml" base))))) ; is some custom mime-type that uses XML

(defn- url-encoded? [content-type]
  (= content-type form-urlencoded))

(defn- has-xml-declaration? [xml]
  (re-find #"^<\?xml" xml))

(defn- remove-xml-declaration [xml]
  ;; split the XML at the end of the XML declaration
  (let [parts (s/split xml #"\?>")]
    ;; if all went well, we should have 2 parts
    (if (= (count parts) 2)
      (last parts) ; the 2nd part is the XML
      xml))) ; things didn't go as expected with the split, so just return the original XML

(defn- newline-after-xml-declaration [xml]
  ;; split the XML at the end of the XML declaration
  (let [parts (s/split xml #"\?>")]
    ;; if all went well, we should have 2 parts
    (if (= (count parts) 2)
      (str (first parts) "?>\n" (last parts)) ; the 2nd part is the XML
      xml))) ; things didn't go as expected with the split, so just return the original XML

(defn pretty-print-xml-and-declaration
  "Determine if the XML already has a declaration, then pretty-print it, then yank off the declaration
  if and only if it didn't have one to start with."
  [body]
  (let [pretty-xml (indent-str (parse-str body))]
    (if (has-xml-declaration? body)
      (newline-after-xml-declaration pretty-xml)
      (remove-xml-declaration pretty-xml))))

(defn- pretty-print
  "Try to pretty-print the request body if content-type matches or is not provided
   and its content-type has not already been derived."
  [request content-type? pretty-printer derived-content-type]
  (let [content-type (content-type-for request)]
    ;; If the content-type checking function passes, or the content-type is blank
    ;; and we haven't aleady derived a content-type, then attempt to pretty print the
    ;; content type using the pretty printer function
    (if (or (content-type? content-type)
            (and (s/blank? content-type) (s/blank? (:derived-content-type request))))
      (try
        ; pretty-print the body and set the derived content type
        (-> request
          (assoc :body (pretty-printer))
          (assoc :derived-content-type derived-content-type))
        (catch Exception e ; pretty printing failed
          (if (s/blank? content-type) ; did they tell us it was this content-type?
            request ; we were only guessing it might be, so leave it as is
            (assoc request :invalid-body true)))) ; they told us it was this type, but it didn't parse
      request))) ; content-type doesn't match

(defn pretty-print-json
  [request]
  (pretty-print
    request
    json?
    (fn [] (generate-string (parse-string (:body request)) {:pretty true}))
    json-encoded))

(defn pretty-print-xml
  [request]
  (pretty-print
    request
    xml?
    (fn [] (pretty-print-xml-and-declaration (:body request)))
    xml-encoded))

 (defn pretty-print-urlencoded
  [request]
  (let [content-type (content-type-for request)
        body-value (:body request)
        body (if body-value body-value "")]
    ;; Attempt form-urlencoded parsing if the body has an = and
    ;; they indicate it's form-urlencoded by content-type, or there's
    ;; no content-type. The pretty-print attempt won't do anything if
    ;; its already been handled as JSON or XML.
    (if (and
          (re-find #"=" body)
          (or (= content-type form-urlencoded)
              (s/blank? content-type)))
      (pretty-print
        request
        url-encoded?
        (fn [] (form-decode body))
        url-encoded)
      (mark-url-encoded-invalid request)))) ; potentially mark it as invalid URL encoded