(ns posthere.util.uuid
  "UUIDs and the heat death of the Universe...")

(defn uuid
  "Simple wrapper for Java's UUID"
  []
  (str (java.util.UUID/randomUUID)))