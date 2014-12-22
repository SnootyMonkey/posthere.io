(ns posthere.examples
  ""
  (:require [clj-time.core :refer (hours ago)]))

(def example-url "/_/example")
(def post-to-example-status 405)
(def post-to-example-body (str "The POSThere.io URL " example-url " is reserved for internal use."))

(defn- update-timestamps
  "Update the timestamp of the 3 canned examples to be 1, 2 and 3 hours ago."
  [examples]
  (-> examples
    (assoc 0 (assoc (first examples) :timestamp (str (-> 1 hours ago))))
    (assoc 1 (assoc (second examples) :timestamp (str (-> 2 hours ago))))
    (assoc 2 (assoc (last examples) :timestamp (str (-> 3 hours ago))))))

(defn example-results
  "Read in canned results as an example."
  []
  (-> (clojure.java.io/resource "example.edn")
    (slurp)
    (read-string)
    (update-timestamps)))