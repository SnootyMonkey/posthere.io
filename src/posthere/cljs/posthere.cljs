(ns posthere
  "POSThere.io Cljs"
    (:require-macros [hiccups.core :as hiccups])
    (:require   [jayq.core :refer ($ css html bind ajax)]
                [clojure.string :as s]
                [hiccups.runtime :as hiccupsrt]))

; index page
(defn- update-uuid-value
    [selector]
    (.text ($ "#urlUUIDInputDisplay") (.val ($ selector))))

(defn- uuid
  "
  Modified from https://github.com/davesann/cljs-uuid
  Returns a new randomly generated (version 4) cljs.core/UUID,
  like: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
  as per http://www.ietf.org/rfc/rfc4122.txt.
  Usage:
  (uuid)  =>  #uuid \"305e764d-b451-47ae-a90d-5db782ac1f2e\""
  []
  (letfn [(f [] (.toString (rand-int 16) 16))
          (g [] (.toString  (bit-or 0x8 (bit-and 0x3 (rand-int 15))) 16))]
    (UUID. (.toString
             (goog.string.StringBuffer.
               (f) (f) (f) (f) (f) (f) (f) (f) "-" (f) (f) (f) (f) 
               "-4" (f) (f) (f) "-" (g) (f) (f) (f) "-"
               (f) (f) (f) (f) (f) (f) (f) (f) (f) (f) (f) (f))))))

(defn- short-uuid []
  "Take the middle 3 sections of a Java UUID to make a shorter UUID.

  Ex: f6f7-499f-b805
  "
  (s/join "-" (take 3 (rest (s/split (uuid) #"-")))))

(defn- set-base-uuid []
  "Generate and set a base UUID view for our page"
  (def base_uuid (short-uuid))
  (.val ($ "#urlUUIDInput") base_uuid)
  (.text ($ "#urlUUIDInputDisplay") base_uuid))

(defn- update-selected-http-method 
    [selector]
    (.text ($ "#urlMethodInputDisplay") (.val ($ selector))))

(defn ^:export init []
    (set-base-uuid)
    (bind ($ "#urlUUIDInput") :keyup (fn [] (this-as this (update-uuid-value this))))
    (bind ($ "#urlMethodInput") :change (fn [] (this-as this (update-selected-http-method this)))))

; results page
(hiccups/defhtml result-header [result]
  [:div
      [:div.clearfix
        [:div.col-md-1
          [:span.glyphicon-glypicon-chevron-down {:aria-hidden "true"}]]
        [:div.col-md-3.text-left
          [:strong "Replace With Timeago"]]
        [:div.col-md-4.text-center
          [:strong "Replace with Content Type"]]
        [:div.col-md-4.text-right
          (list "Status:" [:strong "Result"])]]

      [:div
        [:div.col-md-1 ""]
        [:div.col-md-11.text-left.text-muted 
          [:span.result-timestamp (.-timestamp result)]]]])

(hiccups/defhtml result-table [result]
  [:div "Results"])

(defn row-for [key val]
  (html
    [:tr
      [:th key]
      [:td val]]))

(defn header-table [result]
  (let [headers (js->clj (.-headers result))]
    (.log js/console (keys headers))
    (reduce [] #(conj %1 (row-for %2 (get headers %2)) (keys headers)))))

(hiccups/defhtml result-headers [result]
  [:div.col-md-4
    [:table.table.table-bordered.header-table
      [:tbody
        [:tr
          [:th.text-center {:colspan 2} "Headers"]]
        (header-table result)]]])

(hiccups/defhtml result-template [result]
  [:div.result-group.clearfix 
    (result-header result)
    (result-table result)
    (result-headers result)])

(defn ^:export setup-results []
  (doseq [result js/resultData]
    (.append ($ "#results") (result-template result))))