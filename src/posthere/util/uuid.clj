(ns posthere.util.uuid
  "UUIDs and the heat death of the Universe..."
  (:require [clojure.string :as s]))

(defn uuid []
  "Simple wrapper for Java's UUID"
  (str (java.util.UUID/randomUUID)))

(defn short-uuid []
  "Take the middle 3 sections of a Java UUID to make a shorter UUID.

  Ex: f6f7-499f-b805
  "
  (s/join "-" (take 3 (rest (s/split (uuid) #"-")))))