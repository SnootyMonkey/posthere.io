(ns posthere
  "POSThere.io Cljs"
    (:require   [jayq.core :refer ($ css html bind ajax)]))

(defn- update-uuid-value
    [selector]
    (.text ($ "#urlUUIDInputDisplay") (.val ($ selector))))

(defn- update-selected-http-method 
    [selector]
    (.text ($ "#urlMethodInputDisplay") (.val ($ selector))))

(defn ^:export init []
    (bind ($ "#urlUUIDInput") :keyup (fn [] (this-as this (update-uuid-value this))))
    (bind ($ "#urlMethodInput") :change (fn [] (this-as this (update-selected-http-method this)))))