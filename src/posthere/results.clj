(ns posthere.results
  "Show the saved requests for a particular URL UUID."
  (:require [net.cgrand.enlive-html :as enl :refer (defsnippet deftemplate)]
            [cheshire.core :refer (generate-string)]
            [posthere.storage :refer [requests-for]]))

(defn- template-for [name] (str "../resources/html/" name))

(defsnippet result-rows (template-for "result-table.html")
  [:.result-group]
  [replacements]
  [:.result-group enl/any-node] (enl/replace-vars replacements))

(deftemplate results-page (template-for "results.html") [results, uuid] 

  [:#results] (if (not-empty results) 
                  (enl/remove-attr :style))
  
  [:#empty-results] (if (not-empty results)
                      nil ; clear out the empty results copy
                      (enl/append (str ""))) ;; no-op

  [:#data] (if (not-empty results)
    (enl/html-content (str "<script type='text/javascript'>var resultData = " (generate-string results) "</script>"))
    (enl/html-content (str "<script type='text/javascript'>var resultData = []; </script>")))

  [:.uuid-value] (enl/html-content (str uuid)))

(defn results-view [uuid]
  (let [results (requests-for uuid)]
    (apply str (results-page results uuid))))