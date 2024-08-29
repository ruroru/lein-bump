(defproject versions-plugin "1.0.1"
  :description "Lein plugin that helps updating version in project.clj"
  :url "https://github.com/ruroru/lein-versions-plugin"
  :license {:name "EPL-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/core.match "1.1.0"]]
  :plugins [[lein-release "1.1.3"]]
  :profiles {:test {:dependencies [[mock-clj "0.2.1"]]}}
  :eval-in-leiningen true
  :lein-release {:deploy-via :clojars})