(defproject posthere.io "1.0.3-SNAPSHOT"
  :description "Debug all the POST Requests."
  :url "http://posthere.io/"
  :license {
    :name "Mozilla Public License v2.0"
    :url "http://www.mozilla.org/MPL/2.0/"
  }
  :support {
    :name "Sean Johnson"
    :email "sean@snootymonkey.com"
  }

  :min-lein-version "2.5.1" ; highest version supported by Travis-CI as of 2/17/2015

  :dependencies [
    ;; Server-side
    [org.clojure/clojure "1.7.0"] ; Lisp on the JVM http://clojure.org/documentation
    [org.clojure/core.match "0.3.0-alpha4"] ; Erlang-esque pattern matching https://github.com/clojure/core.match
    [defun "0.3.0-alapha"] ; Erlang-esque pattern matching for Clojure functions https://github.com/killme2008/defun
    [ring/ring-devel "1.4.0"] ; Web application library https://github.com/ring-clojure/ring
    [ring/ring-core "1.4.0"] ; Web application library https://github.com/ring-clojure/ring
    [http-kit "2.1.19"] ; Development Web server http://http-kit.org/
    [compojure "1.4.0"] ; Web routing https://github.com/weavejester/compojure
    [jumblerg/ring.middleware.cors "1.0.1"] ; CORS library https://github.com/jumblerg/ring.middleware.cors
    [raven-clj "1.3.1"] ; Clojure interface to Sentry error reporting https://github.com/sethtrain/raven-clj
    [enlive "1.1.6"] ; HTML Templating system for Clojure https://github.com/cgrand/enlive
    [com.taoensso/carmine "2.12.1"] ; Redis client for Clojure https://github.com/ptaoussanis/carmine
    [clj-time "0.11.0"] ; Clojure date/time library https://github.com/clj-time/clj-time
    [environ "1.0.1"] ; Get environment settings from different sources https://github.com/weavejester/environ
    [cheshire "5.5.0"] ; JSON de/encoding https://github.com/dakrone/cheshire
    [org.clojure/data.xml "0.0.8"] ; XML parser/encoder https://github.com/clojure/data.xml
    [clj-http "2.0.0"] ; HTTP client https://github.com/dakrone/clj-http
    ;; Client-side
    [org.clojure/clojurescript "1.7.189"] ; ClojureScript compiler https://github.com/clojure/clojurescript
    [jayq "2.5.4"] ; ClojureScript wrapper for jQuery https://github.com/ibdknox/jayq
    [hiccups "0.3.0"] ; ClojureScript implementation of Hiccup https://github.com/teropa/hiccups
    [cljs-uuid "0.0.4"] ; ClojureScript UUID https://github.com/davesann/cljs-uuid
  ]

  :plugins [
    [lein-ring "0.9.7"] ; common ring tasks https://github.com/weavejester/lein-ring
    [lein-environ "1.0.1"] ; Get environment settings from lein project https://github.com/weavejester/environ
  ]

  :profiles {

    :uberjar {
      :aot :all
    }
    
    :qa {
      :env {
        :hot-reload false
      }
      :dependencies [
        [midje "1.8.2"] ; Example-based testing https://github.com/marick/Midje
        [ring-mock "0.1.5"] ; Test Ring requests https://github.com/weavejester/ring-mock
      ]
      :plugins [
        [lein-midje "3.2"] ; Example-based testing https://github.com/marick/lein-midje
        [jonase/eastwood "0.2.2"] ; Clojure linter https://github.com/jonase/eastwood
        [lein-kibit "0.1.2"] ; Static code search for non-idiomatic code https://github.com/jonase/kibit
      ]
    }

    :dev [:qa {
      :env ^:replace {
        :hot-reload true ; reload code when changed on the file system
      }
      :dependencies [
        [aprint "0.1.3"] ; Pretty printing in the REPL (aprint thing) https://github.com/razum2um/aprint
      ]
      :plugins [
        [lein-cljsbuild "1.1.1"] ; ClojureScript compiler https://github.com/emezeske/lein-cljsbuild
        [lein-bikeshed "0.2.0"] ; Check for code smells https://github.com/dakrone/lein-bikeshed
        [lein-checkall "0.1.1"] ; Runs bikeshed, kibit and eastwood https://github.com/itang/lein-checkall
        [lein-pprint "1.1.2"] ; pretty-print the lein project map https://github.com/technomancy/leiningen/tree/master/lein-pprint
        [lein-ancient "0.6.8"] ; Check for outdated dependencies https://github.com/xsc/lein-ancient
        [lein-spell "0.1.0"] ; Catch spelling mistakes in docs and docstrings https://github.com/cldwalker/lein-spell
        [lein-deps-tree "0.1.2"] ; Print a tree of project dependencies https://github.com/the-kenny/lein-deps-tree
        [lein-cljfmt "0.3.0"] ; Code formatting https://github.com/weavejester/cljfmt
      ]
      ;; REPL injections
      :injections [
        (require '[aprint.core :refer (aprint ap)]
                 '[clojure.stacktrace :refer (print-stack-trace)]
                 '[clojure.test :refer :all]
                 '[clj-time.core :as t]
                 '[clj-time.format :as f]
                 '[clojure.string :as s])
      ]
    }]

    :prod {
      :env {
        :hot-reload false
      }
    }

  }

  :aliases {
    "build-pages" ["run" "-m" "posthere.static-templating/export"] ; build the static HTML pages
    "build" ["with-profile" "prod" "do" "clean," "cljsbuild" "once," "build-pages," "uberjar"]
    "test!" ["with-profile" "qa" "midje"] ; run all tests
    "run!" ["with-profile" "prod" "run"] ; start a POSThere.io server in production
    "spell!" ["spell" "-n"] ; check spelling in docs and docstrings
    "bikeshed!" ["bikeshed" "-v" "-m" "120"] ; code check with max line length warning of 120 characters
    "ancient" ["ancient" ":all" ":allow-qualified"] ; check for out of date dependencies
  }

  ;; ----- Code check configuration -----

  :eastwood {
    ;; Enable some linters that are disabled by default
    :add-linters [:unused-namespaces :unused-private-vars :unused-locals]

    ;; More extensive lintering that will have a few false positives
    ;; :add-linters [:unused-namespaces :unused-private-vars :unused-locals :unused-fn-args]

    ;; Exclude testing namespaces
    :tests-paths ["test"]
    :exclude-namespaces [:test-paths]
  }

  ;; ----- ClojureScript -----

  :cljsbuild {
    :builds
      [{
      :source-paths ["src/posthere/cljs"] ; CLJS source code path
      ;; Google Closure (CLS) options configuration
      :compiler {
        :output-to "resources/public/js/posthere.js" ; generated JS script filename
        :optimizations :simple ; JS optimization directive
        :pretty-print true ; generated JS code prettyfication
      }}]
  }


  ;; ----- Web Application -----

  :ring {
    :handler posthere.app/app
    :reload-paths ["src"] ; work around issue https://github.com/weavejester/lein-ring/issues/68
  }

  :resource-paths ["resources"]

  :main ^:skip-aot posthere.app
)