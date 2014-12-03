(ns posthere
  "POSThere.io Cljs"
    (:require-macros [hiccups.core :refer (defhtml)])
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
  "Take the middle 3 sections of a UUID to make a shorter UUID.

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

(declare row-for) ; HTML snippet coming later in the namespace

(defn- table-rows
  "Turn a cljs map into name/value rows in a table with hiccup."
  [entries]
  (reduce #(conj %1 (row-for %2 (get entries %2))) () (keys entries)))

(defn- html-escape 
  "Make a string possibly containing HTML display literally rather than as intepreted HTML."
  [string]
  (s/escape string {\< "&lt;", \> "&gt;" \& "&amp;"}))

(defn- time-ago
  "Use the momemnt.js library to return a human readable English string describing how long ago the request was made."
  [result]
  ; moment(timestamp).fromNow();
  (.fromNow (js/moment. (aget result "timestamp"))))

;; ----- Hiccup HTML snippets for the results page -----

(defhtml row-for
  ""
  [key val]
  [:tr 
    [:td key]
    [:td val]])

(defn- body-table-string-content [body]
  [:tr
    [:td.text-left [:pre [:code
      (html-escape body)]]]])

(defn- body-table-content [result]
  (let [body (aget result "body")
        cljs-body (js->clj body)]
    (if (map? cljs-body)
      (table-rows cljs-body)
      (body-table-string-content body))))

(defhtml body-table
  "HTML for a 2 row table, containing a body label row and the body content that was POSTed."
  [result]
  [:div.col-md-8
    [:table.table.table-bordered.result-table
      [:tbody
        [:tr
          [:th.text-center {:colspan 2} "Body"]]
        (body-table-content result)]]])

(defhtml headers-table
  "HTML for a table with all the HTTP header name/value pairs."
  [result]
  [:div.col-md-4
    [:table.table.table-bordered.result-table
      [:tbody
        [:tr
          [:th.text-center {:colspan 2} "Headers"]]
        (table-rows (js->clj (.-headers result)))]]])

(defhtml result-table-header
  "HTML for a table header with timestamp and status."
  [result]
  [:div
    [:div.clearfix
      ;; Put this back in if/when we add expand/collapse
      ;; [:div.col-md-1
        ;; [:span.glyphicon-chevron-down {:aria-hidden "true"}]]]
      [:div.col-md-6.text-left
        [:strong (time-ago result)]]
      [:div.col-md-6.text-right
        "Status: " [:strong (aget result "status")]]]
    [:div
      [:div.col-md-12.text-left.text-muted 
        [:span.result-timestamp (.-timestamp result)]]]])

(defhtml result-template
  "A block of HTML for each request that's been made to this URL."
  [result]
  ;; DIV container for the whole result
  [:div.result-group.clearfix
    ;; Table header with general information about this request
    (result-table-header result)
    [:div.clearfix
      (body-table result)
      (headers-table result)]
    [:div
      [:div.col-md-12
        [:hr]]]])

;; ----- Exported function for the home page -----

(defn ^:export init []
    (set-base-uuid)
    (bind ($ "#urlUUIDInput") :keyup (fn [] (this-as this (update-uuid-value this))))
    (bind ($ "#urlMethodInput") :change (fn [] (this-as this (update-selected-http-method this)))))

;; ----- Exported function for the results page -----

(defn ^:export setup-results []
  (doseq [result js/resultData]
    (.append ($ "#results") (result-template result)))
  (.initHighlightingOnLoad js/hljs)) ; syntax highlight the results