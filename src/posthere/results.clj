(ns posthere.results
  "Show the saved requests for a particular URL UUID."
  (:require [net.cgrand.enlive-html :as enl :refer (content html-snippet deftemplate)]
            [ring.util.response :refer (response header)]
            [cheshire.core :refer (generate-string)]
            [posthere.examples :as examples]
            [posthere.static-templating :as st :refer (partial-content add-key remove-js)]))

(deftemplate results-page st/layout [results uuid]

  ;; use Enlive to combine the layout and the page partial into a HTMl page
  [:#page-partial-container] (content (html-snippet (partial-content "results")))

  [:#doorbell-io-app-key] (add-key st/doorbell-io-app-key) ;; insert the config'd doorbell.io app key
  [:#doorbell-js] (remove-js st/doorbell-io-app-key) ;; remove the doorbell.io JS if there's no app key

  [:#ga-tracking-code] (add-key st/ga-tracking-code) ;; insert the config'd Google Analytics tracking code
  [:#ga-js] (remove-js st/ga-tracking-code) ;; remove the Google Analytics JS if there's no tracking code

  ;; unhide the results div if we DO have some results
  [:#results] (if-not (empty? results)
                  (enl/remove-attr :style))

  ;; unhide the API hints div if we DO have some results and it's not the examples
  [:#api] (if-not (or (empty? results) (= (str "/" uuid) examples/example-url))
                  (enl/remove-attr :style))

  ;; keep the empty-results div only if we DON'T have any results
  [:#empty-results] (if (empty? results)
                      (enl/append "")) ; append a blank string (do nothing), otherwise returning nil wipes it out

  ;; add a JavaScript array with the POST results for this UUID from redis (or an empty array if there are none)
  [:#data] (if (empty? results)
            (enl/html-content "<script type='text/javascript'>var resultData = []; </script>")
            (enl/html-content (str  "<script type='text/javascript'>var resultData = "
                                      (generate-string results)
                                    "</script>")))

  ;; replace the placeholder UUID in the HTML template with our actual UUID
  [:.uuid-value] (enl/html-content (str uuid)))

(defn results-view
  "Create our HTML page for results using the results HTML template and enlive."
  [results uuid headers]
  (if (or
      (= (get headers "accept") "application/json") ; http-kit in development is lower-case
      (= (get headers "Accept") "application/json")) ; nginx-clojure in production is camel-case
    ;; Asked for JSON
    (header (response (generate-string results)) "content-type" "application/json")
    ;; Everything else gets HTML
    (apply str (results-page results uuid))))