(ns posthere.static-templating
  "Use Enlive at design time to create static HTML pages from page partials and layout templates."
  (:require [clojure.string :as s]
            [net.cgrand.enlive-html :as enl :refer (deftemplate html-content)]))

(def template-dir "posthere/templates/")
(def layout (str template-dir "layout.html"))

(def resources-dir "resources/public/")
(def target-dir (str resources-dir "_/"))

;; HTML page partials
(def html-pages
  ["index" "faq" "terms" "privacy"])

(defn partial-for [page-partial]
  (slurp (str "src/" template-dir "_" page-partial ".html")))

(deftemplate layout-page layout [page-partial]
  ;; use Enlive to combine the layout and the page partial into a HTMl page
  [:#page-partial-container] (html-content (partial-for page-partial)))

(defn- export-page
  "Combine the contents of the layout.html template and the page partial template
  to create a static HTML file. All end up in `resources/public/_/` except for
  index, it ends up in `resources/public/`

  The `_` in the directory is to prevent polluting the namespace of available POSTing
  URLs.
  "
  [page-partial]
  (let [output-dir (if (= page-partial "index") resources-dir target-dir)]
    (spit (str output-dir page-partial) (apply str (layout-page page-partial)))))

(defn export
  "
  Create static HTML pages for each page in the html-page-map.

  Run via `lein build-pages` whenever the HTML changes.
  "
  []
  (.mkdir (java.io.File. target-dir))
  (doseq [page html-pages]
    (export-page page)))

