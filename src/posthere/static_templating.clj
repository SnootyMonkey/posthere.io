(ns posthere.static-templating
  "Use Enlive at design time to create static HTML pages from page partials and layout templates."
  (:require [clojure.string :as s]
            [net.cgrand.enlive-html :as enl :refer (deftemplate html-content)]))

(def template-dir "posthere/templates/")
(def layout (str template-dir "layout.html"))

(def target-dir "resources/public/")

(defn partial-for [page-partial]
  (slurp (str "src/" template-dir "_" page-partial ".html")))

;; HTML page partials
(def html-pages
  ["index" "faq" "terms" "privacy"])

(deftemplate layout-page layout [page-partial]
  ;; use Enlive to combine the layout and the page partial into a HTMl page
  [:#page-partial-container] (html-content (partial-for page-partial)))

(defn export-page [page-partial]
  (spit (str target-dir page-partial ".html") (apply str (layout-page page-partial))))

(defn export
  "
  Create static HTML pages for each page in the html-page-map.

  Run via `lein build-pages` whenever the HTML changes.
  "
  []
  (doseq [page html-pages]
    (export-page page)))

