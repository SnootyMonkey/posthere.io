(ns posthere.static-templating
  "Use Enlive at design time to create static HTML pages from page partials and layout templates."
  (:require [clojure.string :as s]
            [net.cgrand.enlive-html :as enl :refer (deftemplate html-content substitute append)]))

(def config-file (read-string (slurp (clojure.java.io/resource "config.edn"))))
(def doorbell-io-app-key (:doorbell-io-app-key config-file))
(def ga-tracking-code (:google-analytics-tracking-code config-file))

(def template-dir "posthere/templates/")
(def layout (str template-dir "layout.html"))

(def resources-dir "resources/public/")
(def target-dir (str resources-dir "_/"))

(def html-pages
  ["index" "faq" "terms" "error"])

(defn- partial-for [page-partial]
  (str template-dir "_" page-partial ".html"))

(defn partial-content [page-partial]
  (slurp (clojure.java.io/resource (partial-for page-partial))))

(defn- remove-js 
  "If the setting is blank, remove the JavaScript, otherwise do nothing."
  [config-setting]
  (if (s/blank? config-setting)
    (substitute nil) ;; remove the element from the DOM
    (append ""))) ;; do nothing

(defn- add-key
  "If the key is blank, do nothing, otherwise add it to the DOM element."
  [config-key]
  (if (s/blank? config-key)
    (append "") ;; do nothing
    (html-content config-key))) ;; add the key as the value of the DOM element

(deftemplate layout-page layout [page-partial]
  
  ;; use Enlive to combine the layout and the page partial into a HTMl page
  [:#page-partial-container] (html-content (partial-content page-partial))
  
  [:#doorbell-io-app-key] (add-key doorbell-io-app-key) ;; insert the config'd  doorbell.io app key
  [:#doorbell-js] (remove-js doorbell-io-app-key) ;; remove the doorbell.io JS if there's no config
  
  [:#ga-tracking-code] (add-key ga-tracking-code) ;; insert the doorbell.io app key
  [:#ga-js] (remove-js ga-tracking-code)) ;; remove the doorbell.io JS
  
(defn- export-page
  "Combine the contents of the layout.html template and the page partial template
  to create a static HTML file, handling the configuration settings for Google Analytics
  and doorbell.io. All static HTML files end up in `resources/public/_/` except for
  index, it ends up in `resources/public/`

  The `_` in the directory is to prevent polluting the namespace of available POSTing
  URLs.
  "
  [page-partial]
  (let [output-dir (if (= page-partial "index") resources-dir target-dir)
        output-file (str output-dir page-partial)]
    (spit output-file (apply str (layout-page page-partial)))
    (println "Rendered:" output-file "from layout:" layout "and page partial:" (partial-for page-partial))))

(defn export
  "
  Create static HTML pages for each page in the html-page-map.

  Run via `lein build-pages` whenever the HTML changes.
  "
  []
  (println config-file)
  (println ga-tracking-code)
  (.mkdir (java.io.File. target-dir))
  (doseq [page html-pages]
    (export-page page)))