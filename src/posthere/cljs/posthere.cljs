(ns posthere
  "POSThere.io Cljs"
    (:require-macros [hiccups.core :refer (defhtml)])
    (:require   [jayq.core :refer ($ bind ajax)]
                [clojure.string :as s]
                [hiccups.runtime :as hiccupsrt]))

; If you change this sentence, also change max-body-size in capture-request.cljs
(def too-big "The body was larger than POSThere.io's maximum of 1 megabyte.")
(def invalid-body-warning "This content-type does not appear to match the body.")

;; ----- URL manipulation functions for presented results -----

(defn- protocol []
  (.-protocol (.-location js/window)))

(defn- host-name []
  (.-host (.-location js/window)))

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

(defn- update-post-url
  "Change the generated URL when any of the parts that make up the URL change."
  []
  (let [scheme (.val ($ "#url-scheme-input"))
        uuid (.val ($ "#url-uuid-input"))
        host (host-name)
        partial-url (str scheme "://" host "/" uuid)
        status-setting (.val ($ "#url-status-input"))
        status (if (= status-setting "200") "" (str "?status=" status-setting))
        new-url (str partial-url status)
        post-url-anchor ($ "#post-url")]
    (.text ($ "#host") host)
    (.text post-url-anchor new-url)
    (.attr post-url-anchor "href" partial-url)))

(defn- set-base-uuid []
  "Generate and set a base UUID view for our page"
  (let [base_uuid (short-uuid)]
    (.val ($ "#url-uuid-input") base_uuid)
    (update-post-url)))

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

(declare row-for) ; HTML snippet coming later in the namespace
(defn- table-rows
  "Turn a cljs map into name/value rows in a table with hiccup."
  [entries]
  (reduce #(conj %1 (row-for %2 (get entries %2))) () (keys entries)))

(defn- string-content
  "A hiccup table row with only one data element, the <pre> and <code> HTML escaped content."
  [content result]
  (let [derived-content-type (aget result "derived-content-type")
        syntax-highlight (if derived-content-type derived-content-type "nohighlight")]
    [:tr
      [:td.text-left {:colspan 2}
        [:pre
          [:code {:class syntax-highlight} (html-escape content)]]]]))

(defn- map-content
  "
  Try to treat the content as a map, and create table rows of name/value pairs.
  If it's not a map, just create a single row with the content as the only data in the row.
  "
  [content result]
  (let [cljs-content (js->clj content)]
    (if (map? cljs-content)
      (table-rows cljs-content)
      (string-content content result))))

(defn- warn-of-invalid-body
  ""
  [headers result]
  (if (aget result "invalid-body") 
    (assoc headers "content-type" [:span [:span.glyphicon.glyphicon-warning-sign {:title invalid-body-warning}](get headers "content-type")]) 
    headers))
;; ----- Hiccup HTML snippets for the results page -----

(defhtml table-header
  "HTML for a table header."
  [label]
  [:tr [:th.text-center {:colspan 2} label]])

(defhtml row-for
  "HTML for a key/value row in a table."
  [key val]
  [:tr [:td key] [:td val]])

(defhtml request-query-string-table
  "Optional HTML table containing query string label and the query string content."
  [result]
  (if-let [query-string (aget result "parsed-query-string")]
    [:table.table.table-bordered.result-table
      [:thead
         (table-header "Query String")]
      [:tbody
        (map-content query-string result)]]))

(defhtml request-body-table
  "Optional HTML for a table containing body label row and the body content that was POSTed."
  [result]
  (let [body-overflow (aget result "body-overflow") ; flag indicating the body was too big
        body (or (aget result "body") (if body-overflow true nil))] ; 3 cases: body, body overflow or no body
    (if body
      [:table.table.table-bordered.result-table
        [:thead
          (table-header "Body")]
        [:tbody
          (if body-overflow
            [:tr [:td [:span.text-muted too-big]]]
            (map-content body result))]])))

(defhtml request-headers-table
  "HTML for a table with all the HTTP header name/value pairs."
  [result]
  [:table.table.table-bordered.result-table
    [:thead
      [:tr
        [:th.text-center {:colspan 2} "Headers"]]]
    [:tbody
      (let [headers (js->clj (.-headers result))
            warning-headers (warn-of-invalid-body headers result)]
        (table-rows warning-headers))]])

(defhtml result-metadata
  "HTML for timestamp and status."
  [result]
  (let [method-name (s/upper-case (aget result "request-method"))
        suffix (if (= method-name "PUT") "" "ed")
        method [:span [:strong method-name] suffix]]
  [:div
    [:div.clearfix
      [:div.col-md-6.text-left
        method "&nbsp;" (time-ago result)]
      [:div.col-md-6.text-right
        "Status: " [:strong (aget result "status")]]]
    [:div
      [:div.col-md-12.text-left.text-muted
        [:span.result-timestamp (.-timestamp result)]]]]))

(defhtml result-template
  "A block of HTML for each request that's been made to this URL."
  [result]
  ;; DIV container for the whole result
  [:div.result-group.clearfix
    ;; General information about this request
    (result-metadata result)
    ;; Details about the request
    [:div.clearfix
      [:div.col-md-8
        (request-query-string-table result)
        (request-body-table result)]
      [:div.col-md-4
        (request-headers-table result)]]
    ;; Row divider
    [:div
      [:div.col-md-12
        [:hr]]]])

;; ----- Exported functions for the results page -----

(defn ^:export delete-results []
  ;; get the UUID
  (let [uuid (.text ($ "#uuid-value"))]
    ;; make AJAX call to delete the UUI
    (ajax (str "/" uuid) {
      :type :delete
      :dataType :json
      :success (fn [data] (.reload js/location))}))) ; if the delete works... reload the page

(defn- replace-span-html [span-class replacement-html]
  (let [spans (js->clj ($ (str "span." span-class)))]
    (doseq [span spans]
      (set! (.-innerHTML span) replacement-html))))

(defn ^:export setup-results []

  ;; Set the protocol everywhere it appears
  (replace-span-html "protocol" (protocol))  
  ;; Set the host everywhere it appears
  (replace-span-html "host" (host-name))

  ;; Render each result
  (doseq [result js/resultData]
    (.append ($ "#results") (result-template result)))
  
  ;; Syntax highlight the results
  (.initHighlightingOnLoad js/hljs))

;; ----- Exported function for the home page -----

(defn ^:export init []
  ;; Generate a unique UUID and set it in the POSTing URL
  (set-base-uuid)
  ;; Bind changes of user selections to update the generated POSTing URL
  (bind ($ "#url-uuid-input") :keyup update-post-url)
  (bind ($ "#url-scheme-input") :change update-post-url)
  (bind ($ "#url-status-input") :change update-post-url))