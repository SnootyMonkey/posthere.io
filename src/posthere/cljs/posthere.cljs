(ns posthere
  "POSThere.io Cljs"
    (:require-macros [hiccups.core :refer (defhtml)])
    (:require   [jayq.core :refer ($ bind)]
                [clojure.string :as s]
                [hiccups.runtime :as hiccupsrt]))

;; ----- Unique UUID generation -----

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

(defn- update-uuid-value
  "Change the UUID portion of the generated URL when the user edits the UUID."
  []
  (.text ($ "#urlUUIDInputDisplay") (.val ($ "#urlUUIDInput"))))

(defn- set-base-uuid []
  "Generate and set a base UUID view for our page"
  (def base_uuid (short-uuid))
  (.val ($ "#urlUUIDInput") base_uuid)
  (.text ($ "#urlUUIDInputDisplay") base_uuid))

(defn- update-http-scheme
  "Change the http/https scheme in the generated URL when the user changes the selection."
  []
  (.text ($ "#urlMethodInputDisplay") (.val ($ "#urlMethodInput"))))

(defn- update-http-status
  "Change the HTTP status in the generated URL when the user changes the selection."
  []
  (let [status (.val ($ "#urlStatusInput"))]
    (if (= status "200")
      (.text ($ "#urlStatusInputDisplay") "")
      (.text ($ "#urlStatusInputDisplay") (str "?status=" status)))))
  
;; ----- Data manipulation functions for presented results -----

(defn- html-escape 
  "Make a string possibly containing HTML display literally rather than as intepreted HTML."
  [string]
  (s/escape string {\< "&lt;", \> "&gt;" \& "&amp;"}))

(defn- time-ago
  "Use the momemnt.js library to return a human readable English string describing how long ago the request was made."
  [result]
  (let [timestamp (aget result "timestamp")
        relative-time (.fromNow (js/moment. timestamp))]
    ;; Relative times in the future make no sense for our case.
    ;; It just mean clocks are out of sync between our server
    ;; and the user's browser. Change future relative times, marked
    ;; by the presence of "in ", to "just now".
    (if (re-find #"^in " relative-time)
      "just now"
      relative-time)))

;; ----- Result table building functions -----

(defn- table-header [label]
  [:tr [:th.text-center {:colspan 2} label]])

(declare row-for) ; HTML snippet coming later in the namespace
(defn- table-rows
  "Turn a cljs map into name/value rows in a table with hiccup."
  [entries]
  (reduce #(conj %1 (row-for %2 (get entries %2))) () (keys entries)))

(defn- string-content 
  "A hiccup table row with only one data element, the <pre> and <code> HTML escaped content."
  [content]
  [:tr [:td.text-left {:colspan 2} [:pre [:code (html-escape content)]]]])

(defn- map-content
  "
  Try to treat the content as a map, and create table rows of name/value pairs.
  If it's not a map, just create a single row with the content as the only data in the row.
  "
  [content]
  (let [cljs-content (js->clj content)]
    (if (map? cljs-content)
      (table-rows cljs-content)
      (string-content content))))

;; ----- Hiccup HTML snippets for the results page -----

(defhtml row-for
  "Hiccup HTML for a key/value row in a table."
  [key val]
  [:tr 
    [:td key]
    [:td val]])

(defhtml body-table
  "
  HTML for a table, containing an optional query string row and the query string content
  and a body label row and the body content that was POSTed.
  "
  [result]
  (let [query-string (aget result "parsed-query-string")
        body (aget result "body")]
    [:div.col-md-8
      (if (or query-string body)
        [:table.table.table-bordered.result-table
          [:tbody
            (if query-string (table-header "Query String"))
            (if query-string (map-content query-string))
            (if body (table-header "Body"))
            (if body (map-content body))]])]))

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
  (bind ($ "#urlUUIDInput") :keyup update-uuid-value)
  (bind ($ "#urlMethodInput") :change update-http-scheme)
  (bind ($ "#urlStatusInput") :change update-http-status))

;; ----- Exported function for the results page -----

(defn ^:export setup-results []
  (doseq [result js/resultData]
    (.append ($ "#results") (result-template result)))
  (.initHighlightingOnLoad js/hljs)) ; syntax highlight the results