(ns posthere.results
  "Show the saved requests for a particular URL UUID."
  (:require [net.cgrand.enlive-html :as enl :refer (defsnippet deftemplate)]
            [cheshire.core :refer (generate-string)]
            [posthere.storage :refer [requests-for]]))

(defn- template-for [template-name] (str "posthere/templates/" template-name))

(deftemplate results-page (template-for "results.html") [results uuid]

  ;; unhide the results div if we DO have some results
  [:#results] (if (not-empty results)
                  (enl/remove-attr :style))

  ;; keep the empty-results div only if we DON'T have any results
  [:#empty-results] (if (empty? results)
                      (enl/append "")) ; append a blank string (do nothing), otherwise returning nil wipes it out

  ;; add a JavaScript array with the POST results for this UUID from redis (or an empty array if there are none)
  [:#data] (if (empty? results)
            (enl/html-content "<script type='text/javascript'>var resultData = []; </script>")
            (enl/html-content (str "<script type='text/javascript'>var resultData = " (generate-string results) "</script>")))

  ;; replace the placeholder UUID in the HTML template with our actual UUID
  [:.uuid-value] (enl/html-content (str uuid)))

(defn results-view
  "Create our HTML page for results using the results HTML template and enlive."
  [uuid]
  (let [results (requests-for uuid)]
    (apply str (results-page results uuid))))