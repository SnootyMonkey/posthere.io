(ns posthere.results
  "Show the saved requests for a particular URL UUID."
  (:require [net.cgrand.enlive-html :as enl :refer (content html-snippet deftemplate)]
            [cheshire.core :refer (generate-string)]
            [posthere.static-templating :as st :refer (partial-for)]))

(deftemplate results-page st/layout [results uuid]

  ;; use Enlive to combine the layout and the page partial into a HTMl page
  [:#page-partial-container] (content (html-snippet (partial-for "results")))

  ;; unhide the results div if we DO have some results
  [:#results] (if (not-empty results)
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
  [results uuid]
  (apply str (results-page results uuid)))