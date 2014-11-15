(ns posthere.results
  "Show the saved requests for a particular URL UUID."
  (:use     [net.cgrand.enlive-html])
  (:require [cheshire.core :refer (generate-string)]
            [posthere.storage :refer [requests-for]]))

(defn- template-for [name] (str "../resources/html/" name))

; Results page work
(defsnippet result-rows (template-for "result-table.html")
  [:.result-group]
  [replacements]
  [:.result-group any-node] (replace-vars replacements))

(deftemplate results-page (template-for "results.html") [results, uuid] 

  [:#results] (if (not-empty results) 
                  (remove-attr :style))
  
  [:#empty-results] (if (not-empty results)
                      nil ; clear out the empty results copy
                      (append (str ""))) ;; no-op

  [:#data] (if (not-empty results)
    (html-content (str "<script type='text/javascript'>var resultData = " (generate-string results) "</script>"))
    (html-content (str "<script type='text/javascript'>var resultData = []; </script>")))

  [:.uuid-value] (html-content (str uuid)))

(defn results-view [uuid]
  (let [results (requests-for uuid)]
    ; (doseq [item results] (prn (:headers item)))
    (apply str (results-page results uuid))))